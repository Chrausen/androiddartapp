package com.clubdarts.ui.training

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.*

// ── Dartboard geometry (all radii in mm from board centre) ───────────────────
internal val SEGMENTS = intArrayOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)

internal const val INNER_BULL_R   = 6.35f
internal const val OUTER_BULL_R   = 15.9f
internal const val TREBLE_INNER_R = 99f
internal const val TREBLE_OUTER_R = 107f
internal const val DOUBLE_INNER_R = 162f
internal const val DOUBLE_OUTER_R = 170f
internal const val NUM_RING_R     = 186f
internal const val SCALE_BOUNDARY_R = 200f

// Normalised radius of the scoring boundary (used for heatmap/dispersion math)
internal const val SCORING_BOUNDARY_NORM = DOUBLE_OUTER_R / SCALE_BOUNDARY_R  // ≈ 0.85

// Board colours
private val BOARD_BLACK   = Color(0xFF111111)
private val SEGMENT_CREAM = Color(0xFFEDE0C8)
private val SEGMENT_RED   = Color(0xFFCC2200)
private val SEGMENT_GREEN = Color(0xFF005500)
private val BULL_RED      = Color(0xFFCC0000)
private val BULL_GREEN    = Color(0xFF006600)
private val WIRE_COLOR    = Color(0xFF888888)

/**
 * Read-only dartboard canvas. Draws the full board and can overlay an
 * [ImageBitmap] (e.g. a pre-computed heatmap) on top of the board.
 */
@Composable
fun DartboardCanvas(
    modifier: Modifier = Modifier,
    overlay: ImageBitmap? = null,
    onDraw: (DrawScope.(cx: Float, cy: Float, scale: Float) -> Unit)? = null
) {
    Canvas(modifier = modifier) {
        val s  = if (size.width > 0f && size.height > 0f) minOf(size.width, size.height) / 2f / SCALE_BOUNDARY_R else 1f
        val cx = size.width  / 2f
        val cy = size.height / 2f

        fun mmToPx(mm: Float) = mm * s

        // 1. Board background
        drawCircle(BOARD_BLACK, radius = mmToPx(NUM_RING_R + 6f), center = Offset(cx, cy))

        // 2. Segments
        for (i in 0 until 20) {
            val isEven      = (i % 2 == 0)
            val singleColor = if (isEven) SEGMENT_CREAM else BOARD_BLACK
            val ringColor   = if (isEven) SEGMENT_RED   else SEGMENT_GREEN
            val startAngle  = -90f + i * 18f - 9f
            val sweep       = 18f
            drawAnnularSegment(cx, cy, mmToPx(DOUBLE_INNER_R), mmToPx(DOUBLE_OUTER_R), startAngle, sweep, ringColor)
            drawAnnularSegment(cx, cy, mmToPx(TREBLE_OUTER_R), mmToPx(DOUBLE_INNER_R), startAngle, sweep, singleColor)
            drawAnnularSegment(cx, cy, mmToPx(TREBLE_INNER_R), mmToPx(TREBLE_OUTER_R), startAngle, sweep, ringColor)
            drawAnnularSegment(cx, cy, mmToPx(OUTER_BULL_R),   mmToPx(TREBLE_INNER_R), startAngle, sweep, singleColor)
        }

        // 3. Bull
        drawCircle(BULL_GREEN, radius = mmToPx(OUTER_BULL_R), center = Offset(cx, cy))
        drawCircle(BULL_RED,   radius = mmToPx(INNER_BULL_R), center = Offset(cx, cy))

        // 4. Wires
        val wireWidth = (1.2f * s).coerceAtLeast(1f)
        for (i in 0 until 20) {
            val rad  = (-90f + i * 18f - 9f) * PI.toFloat() / 180f
            drawLine(
                color       = WIRE_COLOR,
                start       = Offset(cx + mmToPx(OUTER_BULL_R)   * cos(rad), cy + mmToPx(OUTER_BULL_R)   * sin(rad)),
                end         = Offset(cx + mmToPx(DOUBLE_OUTER_R) * cos(rad), cy + mmToPx(DOUBLE_OUTER_R) * sin(rad)),
                strokeWidth = wireWidth
            )
        }
        for (ringR in listOf(OUTER_BULL_R, TREBLE_INNER_R, TREBLE_OUTER_R, DOUBLE_INNER_R, DOUBLE_OUTER_R)) {
            drawCircle(WIRE_COLOR, radius = mmToPx(ringR), center = Offset(cx, cy), style = Stroke(width = wireWidth))
        }

        // 5. Segment numbers
        val numSizePx = size.minDimension * 0.046f
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color     = android.graphics.Color.WHITE
                textSize  = numSizePx
                textAlign = Paint.Align.CENTER
                typeface  = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            val textOffset = -(paint.ascent() + paint.descent()) / 2f
            for (i in 0 until 20) {
                val rad  = (-90f + i * 18f) * PI.toFloat() / 180f
                val numR = mmToPx(NUM_RING_R)
                canvas.nativeCanvas.drawText("${SEGMENTS[i]}", cx + numR * cos(rad), cy + numR * sin(rad) + textOffset, paint)
            }
        }

        // 6. Optional overlay bitmap (heatmap)
        overlay?.let {
            drawImage(
                image   = it,
                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
            )
        }

        // 7. Custom drawing (dispersion circle, guide rings, etc.)
        onDraw?.invoke(this, cx, cy, s)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

internal fun DrawScope.drawAnnularSegment(
    cx: Float, cy: Float,
    innerR: Float, outerR: Float,
    startAngleDeg: Float, sweepAngleDeg: Float,
    color: Color
) {
    val startRad = (startAngleDeg * PI / 180.0).toFloat()
    val endRad   = ((startAngleDeg + sweepAngleDeg) * PI / 180.0).toFloat()
    val path = Path().apply {
        moveTo(cx + outerR * cos(startRad), cy + outerR * sin(startRad))
        arcTo(Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR), startAngleDeg, sweepAngleDeg, false)
        lineTo(cx + innerR * cos(endRad), cy + innerR * sin(endRad))
        arcTo(Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR), startAngleDeg + sweepAngleDeg, -sweepAngleDeg, false)
        close()
    }
    drawPath(path, color)
}

/**
 * Returns the geometric centre (mmX, mmY) of a named field on the board.
 * Coordinates are in mm from board centre, Y axis pointing down.
 */
internal fun fieldCentroid(field: String): Pair<Double, Double>? {
    return when {
        field == "Bullseye" -> Pair(0.0, 0.0)
        field == "Bull"     -> Pair(0.0, 0.0)  // use centre for both bull zones
        field == "Miss"     -> null
        field.startsWith("S") -> {
            val n = field.drop(1).toIntOrNull() ?: return null
            val idx = SEGMENTS.indexOf(n).takeIf { it >= 0 } ?: return null
            val ang = (-90.0 + idx * 18.0) * PI / 180.0
            // Centre of outer single (between triple-outer and double-inner)
            val r = (TREBLE_OUTER_R + DOUBLE_INNER_R) / 2.0
            Pair(sin(ang) * r, -cos(ang) * r)
        }
        field.startsWith("D") -> {
            val n = field.drop(1).toIntOrNull() ?: return null
            val idx = SEGMENTS.indexOf(n).takeIf { it >= 0 } ?: return null
            val ang = (-90.0 + idx * 18.0) * PI / 180.0
            val r = (DOUBLE_INNER_R + DOUBLE_OUTER_R) / 2.0
            Pair(sin(ang) * r, -cos(ang) * r)
        }
        field.startsWith("T") -> {
            val n = field.drop(1).toIntOrNull() ?: return null
            val idx = SEGMENTS.indexOf(n).takeIf { it >= 0 } ?: return null
            val ang = (-90.0 + idx * 18.0) * PI / 180.0
            val r = (TREBLE_INNER_R + TREBLE_OUTER_R) / 2.0
            Pair(sin(ang) * r, -cos(ang) * r)
        }
        else -> null
    }
}

/**
 * Convert a (score, multiplier) pair coming from the dartboard to a field notation string.
 */
internal fun dartToFieldString(score: Int, multiplier: Int): String = when {
    score == 0                -> "Miss"
    score == 25 && multiplier == 2 -> "Bullseye"
    score == 25               -> "Bull"
    multiplier == 1           -> "S$score"
    multiplier == 2           -> "D$score"
    multiplier == 3           -> "T$score"
    else                      -> "Miss"
}
