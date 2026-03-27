package com.clubdarts.ui.game.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.clubdarts.ui.game.DartInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*

// ── Dartboard geometry (all radii in mm from board centre) ──────────────────

private val SEGMENTS = intArrayOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

private const val INNER_BULL_R  = 6.35f
private const val OUTER_BULL_R  = 15.9f
private const val TREBLE_INNER_R = 99f
private const val TREBLE_OUTER_R = 107f
private const val DOUBLE_INNER_R = 162f
private const val DOUBLE_OUTER_R = 170f
private const val NUM_RING_R    = 186f   // where segment numbers sit

// ── Interaction constants ───────────────────────────────────────────────────

private const val LONG_PRESS_MS      = 180L
private const val AUTO_CONFIRM_MS    = 1500
private const val DAMP_MIN           = 0.1f
private const val DAMP_MAX           = 1.0f
private const val DAMP_DISTANCE_PX   = 60f
private const val HIGHLIGHT_BASE_ALPHA = 0.55f
private const val RING_RADIUS_MM     = 11f
private const val RING_LINE_WIDTH_MM = 2.2f

// ── Board colours ───────────────────────────────────────────────────────────

private val BOARD_BLACK    = Color(0xFF111111)
private val SEGMENT_CREAM  = Color(0xFFEDE0C8)
private val SEGMENT_RED    = Color(0xFFCC2200)
private val SEGMENT_GREEN  = Color(0xFF005500)
private val BULL_RED       = Color(0xFFCC0000)
private val BULL_GREEN     = Color(0xFF006600)
private val WIRE_COLOR     = Color(0xFF888888)
private val DART_GOLD      = Color(0xFFFFD700)

// ── Internal data ───────────────────────────────────────────────────────────

/** Full information about where a dart landed, needed for both drawing and DB storage. */
private data class BoardDartScore(
    val score: Int,
    val multiplier: Int,
    val segIdx: Int,      // 0-19 for segments, -1 for bull zones
    val zoneInnerR: Float, // mm — inner boundary of landing zone
    val zoneOuterR: Float  // mm — outer boundary of landing zone
)

private data class PlacedDart(
    val cx: Float, val cy: Float,        // canvas position (px)
    val mmX: Float, val mmY: Float,      // board mm position from centre
    val score: BoardDartScore,
    val index: Int                       // 1, 2 or 3
)

// ── Composable ──────────────────────────────────────────────────────────────

/**
 * Touch-based dartboard input. Replaces the numpad when board input mode is active.
 *
 * Gesture model:
 * - Quick tap → places a pending dart; starts 1.5 s auto-confirm countdown
 * - Tap while pending dart exists → silently confirms the pending dart, places new one
 * - Hold 180 ms + drag (pending dart must exist) → repositions the pending dart with
 *   distance-based movement damping; countdown restarts from zero on release
 *
 * @param currentDarts  The ViewModel's live list of darts in the current visit (used
 *                      to sync the confirmed-dot overlay when the user undoes darts or
 *                      a new visit begins).
 * @param onDartConfirmed  Called when a dart is confirmed. `boardX`/`boardY` are the
 *                         board mm coordinates from centre, suitable for DB storage.
 */
@Composable
fun DartBoardInput(
    currentDarts: List<DartInput>,
    onDartConfirmed: (score: Int, multiplier: Int, boardX: Float, boardY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // ── Pending dart ──────────────────────────────────────────────────────────
    var pendingX     by remember { mutableFloatStateOf(0f) }
    var pendingY     by remember { mutableFloatStateOf(0f) }
    var pendingMmX   by remember { mutableFloatStateOf(0f) }
    var pendingMmY   by remember { mutableFloatStateOf(0f) }
    var pendingScore by remember { mutableStateOf<BoardDartScore?>(null) }
    var hasPending   by remember { mutableStateOf(false) }

    // ── Countdown ─────────────────────────────────────────────────────────────
    var countdownProgress by remember { mutableFloatStateOf(0f) }
    var countdownJob      by remember { mutableStateOf<Job?>(null) }
    var isDragging        by remember { mutableStateOf(false) }

    // ── Confirmed dots for the current visit (positional) ────────────────────
    val confirmedDarts = remember { mutableStateListOf<PlacedDart>() }

    // Sync with ViewModel state: handle undo and new-visit clears
    var prevCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentDarts.size) {
        val newCount = currentDarts.size
        when {
            newCount == 0 && prevCount > 0   -> confirmedDarts.clear()
            newCount < prevCount             -> repeat(prevCount - newCount) { confirmedDarts.removeLastOrNull() }
        }
        prevCount = newCount
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────
    fun scale(size: Size) =
        if (size.width > 0f && size.height > 0f) minOf(size.width, size.height) / 2f / NUM_RING_R else 1f

    fun mmToPx(mm: Float, size: Size) = mm * scale(size)
    fun boardCx(size: Size) = size.width  / 2f
    fun boardCy(size: Size) = size.height / 2f

    fun canvasToMm(cxPos: Float, cyPos: Float, size: Size): Pair<Float, Float> {
        val s = scale(size)
        return Pair((cxPos - boardCx(size)) / s, (cyPos - boardCy(size)) / s)
    }

    // ── Score detection ───────────────────────────────────────────────────────
    fun detectScore(mmX: Float, mmY: Float): BoardDartScore {
        val dist = sqrt(mmX * mmX + mmY * mmY)
        if (dist <= INNER_BULL_R)  return BoardDartScore(25, 2, -1, 0f,            INNER_BULL_R)
        if (dist <= OUTER_BULL_R)  return BoardDartScore(25, 1, -1, INNER_BULL_R,  OUTER_BULL_R)

        val angleDeg = ((atan2(mmY, mmX) * 180.0 / PI) + 90.0 + 360.0) % 360.0
        val segIdx   = ((angleDeg + 9.0) % 360.0 / 18.0).toInt().coerceIn(0, 19)
        val segVal   = SEGMENTS[segIdx]

        return when {
            dist <= TREBLE_INNER_R -> BoardDartScore(segVal, 1, segIdx, OUTER_BULL_R,   TREBLE_INNER_R)
            dist <= TREBLE_OUTER_R -> BoardDartScore(segVal, 3, segIdx, TREBLE_INNER_R,  TREBLE_OUTER_R)
            dist <= DOUBLE_INNER_R -> BoardDartScore(segVal, 1, segIdx, TREBLE_OUTER_R,  DOUBLE_INNER_R)
            dist <= DOUBLE_OUTER_R -> BoardDartScore(segVal, 2, segIdx, DOUBLE_INNER_R,  DOUBLE_OUTER_R)
            else                   -> BoardDartScore(0,      1, -1,     0f,              0f)
        }
    }

    // ── Countdown management ──────────────────────────────────────────────────
    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    fun confirmPending() {
        if (!hasPending) return
        stopCountdown()
        val s = pendingScore ?: return
        confirmedDarts.add(PlacedDart(pendingX, pendingY, pendingMmX, pendingMmY, s, confirmedDarts.size + 1))
        hasPending = false
        pendingScore = null
        countdownProgress = 0f
        onDartConfirmed(s.score, s.multiplier, pendingMmX, pendingMmY)
    }

    fun startCountdown() {
        stopCountdown()
        countdownProgress = 0f
        var startFrameMs = -1L
        countdownJob = coroutineScope.launch {
            while (isActive) {
                withFrameMillis { frameMs ->
                    if (startFrameMs < 0L) startFrameMs = frameMs
                    val elapsed = frameMs - startFrameMs
                    countdownProgress = (elapsed.toFloat() / AUTO_CONFIRM_MS).coerceIn(0f, 1f)
                }
                if (countdownProgress >= 1f) {
                    confirmPending()
                    break
                }
            }
        }
    }

    fun placeDart(cx: Float, cy: Float, size: Size) {
        if (hasPending) confirmPending()
        val (mmX, mmY) = canvasToMm(cx, cy, size)
        pendingX     = cx
        pendingY     = cy
        pendingMmX   = mmX
        pendingMmY   = mmY
        pendingScore = detectScore(mmX, mmY)
        hasPending   = true
        startCountdown()
    }

    // ── Canvas + gesture input ────────────────────────────────────────────────
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                // PointerInputScope is an unrestricted CoroutineScope, so we can use
                // delay/coroutineScope/launch here. Only AwaitPointerEventScope (the
                // lambda inside awaitPointerEventScope{}) is @RestrictsSuspension.
                while (true) {
                    val currentSize = Size(size.width.toFloat(), size.height.toFloat())

                    // Wait for the next finger-down event (unrestricted: uses awaitPointerEventScope internally)
                    val down = awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                    val downPos = down.position
                    down.consume()

                    // Race: delay-timer vs. finger-up.
                    // If the finger lifts before LONG_PRESS_MS the up-event wins → quick tap.
                    // If the delay fires first it cancels the coroutineScope → long press.
                    var longPressOccurred = false
                    try {
                        coroutineScope {
                            val scopeJob = coroutineContext[Job]!!
                            val timerJob = launch {
                                delay(LONG_PRESS_MS)
                                longPressOccurred = true
                                scopeJob.cancel()   // cancels the sibling awaitPointerEventScope
                            }
                            awaitPointerEventScope {
                                val up = waitForUpOrCancellation()
                                if (up != null) {
                                    up.consume()
                                    placeDart(downPos.x, downPos.y, currentSize)
                                }
                            }
                            // Finger lifted before timeout: cancel only the timer, not the scope
                            timerJob.cancel()
                        }
                    } catch (e: CancellationException) {
                        // Only rethrow if something external cancelled us (not our own timer)
                        if (!longPressOccurred) throw e
                    }

                    // ── Long press: reposition pending dart via drag ──────────────
                    if (longPressOccurred && hasPending) {
                        isDragging = true
                        stopCountdown()

                        val fingerOriginX = downPos.x
                        val fingerOriginY = downPos.y
                        var lastFingerX   = fingerOriginX
                        var lastFingerY   = fingerOriginY
                        var dartPosX      = pendingX
                        var dartPosY      = pendingY

                        awaitPointerEventScope {
                            var pointerDown = true
                            while (pointerDown) {
                                val event  = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    pointerDown = false
                                } else {
                                    val currX   = change.position.x
                                    val currY   = change.position.y
                                    val frameDx = currX - lastFingerX
                                    val frameDy = currY - lastFingerY
                                    lastFingerX = currX
                                    lastFingerY = currY

                                    val totalDx = currX - fingerOriginX
                                    val totalDy = currY - fingerOriginY
                                    val distPx  = sqrt(totalDx * totalDx + totalDy * totalDy)
                                    val damp    = DAMP_MIN + (DAMP_MAX - DAMP_MIN) *
                                        (distPx / DAMP_DISTANCE_PX).coerceIn(0f, 1f)

                                    dartPosX  += frameDx * damp
                                    dartPosY  += frameDy * damp
                                    pendingX   = dartPosX
                                    pendingY   = dartPosY

                                    val (mmX, mmY) = canvasToMm(dartPosX, dartPosY, currentSize)
                                    pendingMmX   = mmX
                                    pendingMmY   = mmY
                                    pendingScore = detectScore(mmX, mmY)
                                    change.consume()
                                }
                            }
                        }

                        isDragging = false
                        startCountdown()
                    }
                }
            }
    ) {
        val s  = scale(size)
        val cx = boardCx(size)
        val cy = boardCy(size)

        // ── 1. Board background ───────────────────────────────────────────────
        drawCircle(
            color  = BOARD_BLACK,
            radius = mmToPx(NUM_RING_R + 6f, size),
            center = Offset(cx, cy)
        )

        // ── 2. Twenty segments (double / outer-single / treble / inner-single) ─
        for (i in 0 until 20) {
            val isEven      = (i % 2 == 0)
            val singleColor = if (isEven) SEGMENT_CREAM else BOARD_BLACK
            val ringColor   = if (isEven) SEGMENT_RED   else SEGMENT_GREEN
            // Compose Canvas: 0° = 3 o'clock, clockwise. Top = −90°.
            // Segment i centre is at −90° + i×18°; span = 18°.
            val startAngle  = -90f + i * 18f - 9f
            val sweep       = 18f

            drawAnnularSegment(cx, cy, mmToPx(DOUBLE_INNER_R, size), mmToPx(DOUBLE_OUTER_R, size), startAngle, sweep, ringColor)
            drawAnnularSegment(cx, cy, mmToPx(TREBLE_OUTER_R, size), mmToPx(DOUBLE_INNER_R, size), startAngle, sweep, singleColor)
            drawAnnularSegment(cx, cy, mmToPx(TREBLE_INNER_R, size), mmToPx(TREBLE_OUTER_R, size), startAngle, sweep, ringColor)
            drawAnnularSegment(cx, cy, mmToPx(OUTER_BULL_R,   size), mmToPx(TREBLE_INNER_R, size), startAngle, sweep, singleColor)
        }

        // ── 3. Bull zones ─────────────────────────────────────────────────────
        drawCircle(BULL_GREEN, radius = mmToPx(OUTER_BULL_R, size), center = Offset(cx, cy))
        drawCircle(BULL_RED,   radius = mmToPx(INNER_BULL_R, size), center = Offset(cx, cy))

        // ── 4. Wire lines ─────────────────────────────────────────────────────
        val wireWidth = (1.2f * s).coerceAtLeast(1f)
        for (i in 0 until 20) {
            val rad  = (-90f + i * 18f - 9f) * PI.toFloat() / 180f
            val cosA = cos(rad)
            val sinA = sin(rad)
            drawLine(
                color       = WIRE_COLOR,
                start       = Offset(cx + mmToPx(OUTER_BULL_R,   size) * cosA, cy + mmToPx(OUTER_BULL_R,   size) * sinA),
                end         = Offset(cx + mmToPx(DOUBLE_OUTER_R, size) * cosA, cy + mmToPx(DOUBLE_OUTER_R, size) * sinA),
                strokeWidth = wireWidth
            )
        }
        for (ringR in listOf(OUTER_BULL_R, TREBLE_INNER_R, TREBLE_OUTER_R, DOUBLE_INNER_R, DOUBLE_OUTER_R)) {
            drawCircle(
                color  = WIRE_COLOR,
                radius = mmToPx(ringR, size),
                center = Offset(cx, cy),
                style  = Stroke(width = wireWidth)
            )
        }

        // ── 5. Segment numbers ────────────────────────────────────────────────
        val numSizePx = size.minDimension * 0.046f
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color    = android.graphics.Color.WHITE
                textSize = numSizePx
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            val textCentreOffset = -(paint.ascent() + paint.descent()) / 2f
            for (i in 0 until 20) {
                val rad  = (-90f + i * 18f) * PI.toFloat() / 180f
                val numR = mmToPx(NUM_RING_R, size)
                canvas.nativeCanvas.drawText(
                    "${SEGMENTS[i]}",
                    cx + numR * cos(rad),
                    cy + numR * sin(rad) + textCentreOffset,
                    paint
                )
            }
        }

        // ── 6. Confirmed dart dots for this visit ─────────────────────────────
        confirmedDarts.forEach { dart ->
            drawCircle(DART_GOLD,   radius = mmToPx(3.5f, size), center = Offset(dart.cx, dart.cy))
            drawCircle(Color.White, radius = mmToPx(3.5f, size), center = Offset(dart.cx, dart.cy),
                style = Stroke(width = mmToPx(0.7f, size)))
            // Dart index number using native canvas for pixel-accurate sizing
            drawIntoCanvas { canvas ->
                val p = Paint().apply {
                    color    = android.graphics.Color.BLACK
                    textSize = size.minDimension * 0.03f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                val offset = -(p.ascent() + p.descent()) / 2f
                canvas.nativeCanvas.drawText("${dart.index}", dart.cx, dart.cy + offset, p)
            }
        }

        // ── 7. Pending dart, field highlight, and countdown ring ──────────────
        if (hasPending) {
            val score    = pendingScore
            val progress = countdownProgress

            // Field highlight (drawn below everything else on the overlay)
            if (score != null) {
                val alpha = HIGHLIGHT_BASE_ALPHA
                if (alpha > 0.01f) {
                    drawFieldHighlight(score, cx, cy, s, alpha)
                }
            }

            // Countdown track ring (full circle, dim)
            drawCircle(
                color  = Color.White.copy(alpha = 0.18f),
                radius = mmToPx(RING_RADIUS_MM, size),
                center = Offset(pendingX, pendingY),
                style  = Stroke(width = mmToPx(RING_LINE_WIDTH_MM, size))
            )

            // Countdown progress arc (only when not dragging)
            if (!isDragging && progress > 0f) {
                val ringColor = when {
                    progress < 0.6f  -> Color.White.copy(alpha = 0.7f + progress * 0.3f)
                    progress < 0.85f -> Color(0xFFFFC828)   // amber
                    else             -> Color(0xFFFF5028)   // red-orange
                }
                val ringR = mmToPx(RING_RADIUS_MM, size)
                drawArc(
                    color      = ringColor,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter  = false,
                    topLeft    = Offset(pendingX - ringR, pendingY - ringR),
                    size       = Size(ringR * 2f, ringR * 2f),
                    style      = Stroke(width = mmToPx(RING_LINE_WIDTH_MM, size), cap = StrokeCap.Round)
                )
            }

            // Pending dart dot (semi-transparent gold with white stroke)
            val dotR = mmToPx(4f, size)
            drawCircle(DART_GOLD.copy(alpha = 0.85f), radius = dotR, center = Offset(pendingX, pendingY))
            drawCircle(Color.White, radius = dotR, center = Offset(pendingX, pendingY),
                style = Stroke(width = mmToPx(0.8f, size)))
        }
    }
}

// ── Private drawing helpers ─────────────────────────────────────────────────

/** Draws a filled arc segment between two concentric circles (an annular wedge). */
private fun DrawScope.drawAnnularSegment(
    cx: Float, cy: Float,
    innerR: Float, outerR: Float,
    startAngleDeg: Float, sweepAngleDeg: Float,
    color: Color
) {
    val startRad = (startAngleDeg * PI / 180.0).toFloat()
    val endRad   = ((startAngleDeg + sweepAngleDeg) * PI / 180.0).toFloat()
    val path = Path().apply {
        moveTo(cx + outerR * cos(startRad), cy + outerR * sin(startRad))
        arcTo(
            rect              = Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR),
            startAngleDegrees = startAngleDeg,
            sweepAngleDegrees = sweepAngleDeg,
            forceMoveTo       = false
        )
        lineTo(cx + innerR * cos(endRad), cy + innerR * sin(endRad))
        arcTo(
            rect              = Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR),
            startAngleDegrees = startAngleDeg + sweepAngleDeg,
            sweepAngleDegrees = -sweepAngleDeg,
            forceMoveTo       = false
        )
        close()
    }
    drawPath(path, color)
}

/**
 * Draws the semi-transparent zone highlight for the landing segment/ring.
 *
 * Highlight colour: white for dark-background zones (black singles, coloured rings, bull),
 * dark for the cream single beds (odd-indexed segments in single zones).
 */
private fun DrawScope.drawFieldHighlight(
    score: BoardDartScore,
    cx: Float, cy: Float,
    scale: Float,
    alpha: Float
) {
    if (score.score == 0) return   // miss – no highlight

    // Even-index single beds are cream (light background) → dark overlay for contrast.
    // Odd-index singles are black, all rings and bull zones are coloured → white overlay.
    val isEvenSegSingle = score.segIdx >= 0 && score.segIdx % 2 == 0 && score.multiplier == 1
    val highlightColor = if (isEvenSegSingle) Color(0xFF1A1A1A).copy(alpha = alpha)
                         else Color.White.copy(alpha = alpha)

    when {
        score.segIdx < 0 && score.multiplier == 2 -> {
            // Inner bull: filled circle
            drawCircle(highlightColor, radius = score.zoneOuterR * scale, center = Offset(cx, cy))
        }
        score.segIdx < 0 -> {
            // Outer bull: annular ring
            drawAnnularSegment(cx, cy, score.zoneInnerR * scale, score.zoneOuterR * scale, 0f, 360f, highlightColor)
        }
        else -> {
            val startAngle = -90f + score.segIdx * 18f - 9f
            drawAnnularSegment(
                cx, cy,
                score.zoneInnerR * scale, score.zoneOuterR * scale,
                startAngle, 18f,
                highlightColor
            )
        }
    }
}
