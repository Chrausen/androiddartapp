package com.clubdarts.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.R
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.ui.game.DartInput
import com.clubdarts.ui.game.components.DartBoardInput
import com.clubdarts.ui.game.components.DartNumpad
import com.clubdarts.ui.theme.*

@Composable
fun TrainingLiveScreen(
    uiState: TrainingUiState,
    onRecordDart: (String) -> Unit,
    onRecordBoardDart: (Int, Int, Float, Float) -> Unit,
    onRecordScoringDart: (Int) -> Unit,
    onSetMultiplier: (Int) -> Unit,
    onToggleInputMode: () -> Unit,
    onUndo: () -> Unit,
    onAbort: () -> Unit
) {
    val session = uiState.liveSession ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAbort) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_abort), tint = TextSecondary)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = modeLabel(uiState.mode),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.weight(1f))
            // Board/Numpad toggle (only for TARGET_FIELD and AROUND_THE_CLOCK)
            if (session !is LiveSessionState.ScoringRounds) {
                IconButton(onClick = onToggleInputMode) {
                    Icon(
                        Icons.Default.Adjust,
                        contentDescription = stringResource(
                            if (uiState.showBoardInput) R.string.live_hide_board_input
                            else R.string.live_show_board_input
                        ),
                        tint = if (uiState.showBoardInput) Accent else TextSecondary
                    )
                }
            }
            IconButton(onClick = onUndo, enabled = canUndo(session)) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo(session)) TextPrimary else TextTertiary
                )
            }
        }

        HorizontalDivider(color = Border)

        // Session-specific content
        when (session) {
            is LiveSessionState.TargetField    -> TargetFieldContent(
                session            = session,
                pendingMultiplier  = uiState.pendingMultiplier,
                showBoardInput     = uiState.showBoardInput,
                onRecordDart       = onRecordDart,
                onRecordBoardDart  = onRecordBoardDart,
                onSetMultiplier    = onSetMultiplier
            )
            is LiveSessionState.AroundTheClock -> AroundTheClockContent(
                session            = session,
                pendingMultiplier  = uiState.pendingMultiplier,
                showBoardInput     = uiState.showBoardInput,
                onRecordDart       = onRecordDart,
                onRecordBoardDart  = onRecordBoardDart,
                onSetMultiplier    = onSetMultiplier
            )
            is LiveSessionState.ScoringRounds  -> ScoringRoundsContent(session, onRecordScoringDart)
        }
    }
}

// ── Target Field ──────────────────────────────────────────────────────────────

@Composable
private fun TargetFieldContent(
    session: LiveSessionState.TargetField,
    pendingMultiplier: Int,
    showBoardInput: Boolean,
    onRecordDart: (String) -> Unit,
    onRecordBoardDart: (Int, Int, Float, Float) -> Unit,
    onSetMultiplier: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { session.currentIdx / 10f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Accent,
            trackColor = Surface2
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${session.currentIdx} / 10",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.training_target),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = session.currentTarget,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DmMono,
            color = Accent
        )

        val missCount = session.throws.takeLastWhile { it.targetField == session.currentTarget }.size
        if (missCount > 0) {
            Text(
                text = stringResource(R.string.training_misses_on_target, missCount),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.training_total_darts, session.totalDarts),
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )

        Spacer(Modifier.weight(1f))

        // Darts at the current target — used by DartBoardInput to sync undo / target-change clears
        val dartsAtCurrentTarget = remember(session.throws, session.currentTarget) {
            session.throws.takeLastWhile { it.targetField == session.currentTarget }
        }

        TrainingDartInput(
            showBoardInput    = showBoardInput,
            pendingMultiplier = pendingMultiplier,
            dartsAtTarget     = dartsAtCurrentTarget.size,
            onRecordDart      = onRecordDart,
            onRecordBoardDart = onRecordBoardDart,
            onSetMultiplier   = onSetMultiplier
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ── Around the Clock ──────────────────────────────────────────────────────────

@Composable
private fun AroundTheClockContent(
    session: LiveSessionState.AroundTheClock,
    pendingMultiplier: Int,
    showBoardInput: Boolean,
    onRecordDart: (String) -> Unit,
    onRecordBoardDart: (Int, Int, Float, Float) -> Unit,
    onSetMultiplier: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { (session.currentNumber - 1) / 20f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Accent,
            trackColor = Surface2
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${session.currentNumber - 1} / 20",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.training_target),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(4.dp))

        val needsDouble = session.requiresDouble(session.currentNumber)
        val currentTargetField = if (needsDouble) "D${session.currentNumber}" else "S${session.currentNumber}"
        Text(
            text = if (needsDouble) "D${session.currentNumber}" else "${session.currentNumber}",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = DmMono,
            color = Accent
        )
        if (needsDouble) {
            Text(
                text = stringResource(R.string.training_double_required),
                style = MaterialTheme.typography.labelSmall,
                color = Amber
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.training_total_darts, session.totalDarts),
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )

        Spacer(Modifier.weight(1f))

        val dartsAtCurrentTarget = remember(session.throws, session.currentNumber) {
            session.throws.takeLastWhile { it.targetField == currentTargetField }
        }

        TrainingDartInput(
            showBoardInput    = showBoardInput,
            pendingMultiplier = if (needsDouble) 2 else pendingMultiplier,
            dartsAtTarget     = dartsAtCurrentTarget.size,
            onRecordDart      = onRecordDart,
            onRecordBoardDart = onRecordBoardDart,
            onSetMultiplier   = onSetMultiplier
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ── Shared dart input (Numpad ↔ Board toggle) ─────────────────────────────────

@Composable
private fun TrainingDartInput(
    showBoardInput: Boolean,
    pendingMultiplier: Int,
    dartsAtTarget: Int,
    onRecordDart: (String) -> Unit,
    onRecordBoardDart: (Int, Int, Float, Float) -> Unit,
    onSetMultiplier: (Int) -> Unit
) {
    if (showBoardInput) {
        // Sync DartBoardInput's confirmed-dot count with darts at the current target.
        // DartBoardInput only uses currentDarts.size, so we pass a dummy list of the right length.
        val dummyDarts = remember(dartsAtTarget) {
            List(dartsAtTarget) { DartInput(0, 1) }
        }
        DartBoardInput(
            currentDarts      = dummyDarts,
            onDartConfirmed   = { score, mult, bx, by -> onRecordBoardDart(score, mult, bx, by) },
            modifier          = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
    } else {
        DartNumpad(
            pendingMultiplier = pendingMultiplier,
            onMultiplierChange = onSetMultiplier,
            onDart = { num ->
                val field = when {
                    num == 25 && pendingMultiplier == 2 -> "Bullseye"
                    num == 25                           -> "Bull"
                    pendingMultiplier == 3              -> "T$num"
                    pendingMultiplier == 2              -> "D$num"
                    else                               -> "S$num"
                }
                onRecordDart(field)
            },
            onMiss = { onRecordDart("Miss") }
        )
    }
}

// ── Scoring Rounds ────────────────────────────────────────────────────────────

@Composable
private fun ScoringRoundsContent(
    session: LiveSessionState.ScoringRounds,
    onScoringDart: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { session.completedRounds.size / 10f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Accent,
            trackColor = Surface2
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.training_round_progress, session.completedRounds.size, 10),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                val score = session.currentRoundDarts.getOrNull(i)
                DartSlot(score = score)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip(
                label = stringResource(R.string.training_avg),
                value = if (session.completedRounds.isEmpty()) "—" else "%.1f".format(session.runningAverage),
                modifier = Modifier.weight(1f)
            )
            StatChip(
                label = stringResource(R.string.training_target_avg),
                value = session.targetAvg.toString(),
                modifier = Modifier.weight(1f),
                valueColor = Amber
            )
        }

        Spacer(Modifier.weight(1f))

        ScoringNumpad(onScore = onScoringDart)
        Spacer(Modifier.height(16.dp))
    }
}

// ── Scoring numpad ────────────────────────────────────────────────────────────

@Composable
private fun ScoringNumpad(onScore: (Int) -> Unit) {
    var inputBuffer by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = if (inputBuffer.isEmpty()) "0" else inputBuffer,
                fontSize = 28.sp,
                fontFamily = DmMono,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("⌫", "0", "✓")
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .background(
                                color = when (label) {
                                    "✓"  -> Accent
                                    "⌫"  -> Surface3
                                    else -> Surface2
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                when (label) {
                                    "⌫" -> inputBuffer = inputBuffer.dropLast(1)
                                    "✓" -> {
                                        val v = inputBuffer.toIntOrNull()?.coerceIn(0, 180) ?: 0
                                        onScore(v)
                                        inputBuffer = ""
                                    }
                                    else -> {
                                        val next = inputBuffer + label
                                        if ((next.toIntOrNull() ?: 0) <= 180) inputBuffer = next
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = if (label == "✓" || label == "⌫") null else DmMono,
                            color = if (label == "✓") Background else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun DartSlot(score: Int?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = if (score != null) AccentDim else Surface2,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (score != null) 1.dp else 0.dp,
                color = if (score != null) Accent else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (score != null) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = DmMono,
                color = Accent
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface2),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(value, fontSize = 20.sp, fontFamily = DmMono, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

private fun canUndo(session: LiveSessionState): Boolean = when (session) {
    is LiveSessionState.TargetField    -> session.throws.isNotEmpty()
    is LiveSessionState.AroundTheClock -> session.throws.isNotEmpty()
    is LiveSessionState.ScoringRounds  -> session.currentRoundDarts.isNotEmpty() || session.completedRounds.isNotEmpty()
}

@Composable
private fun modeLabel(mode: TrainingMode): String = stringResource(when (mode) {
    TrainingMode.TARGET_FIELD      -> R.string.training_mode_target
    TrainingMode.AROUND_THE_CLOCK  -> R.string.training_mode_atc
    TrainingMode.SCORING_ROUNDS    -> R.string.training_mode_scoring
})
