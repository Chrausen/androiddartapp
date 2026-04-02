package com.clubdarts.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
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
import com.clubdarts.data.db.dao.BestSessionWithPlayer
import com.clubdarts.data.model.TrainingDifficulty
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.data.model.TrainingSession
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.game.components.PlayerPickerSheet
import com.clubdarts.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrainingSetupScreen(
    uiState: TrainingUiState,
    onSelectPlayer: (com.clubdarts.data.model.Player?) -> Unit,
    onSelectMode: (TrainingMode) -> Unit,
    onSelectDifficulty: (TrainingDifficulty) -> Unit,
    onStart: () -> Unit,
    onOpenHeatmap: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }
    var showPlayerPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.training_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            TextButton(onClick = onOpenHeatmap) {
                Text(
                    text = stringResource(R.string.training_analytics),
                    color = Accent,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Player selection
        Text(
            text = stringResource(R.string.training_player),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        PlayerChip(
            selectedName = uiState.selectedPlayer?.name,
            onClick = { showPlayerPicker = true }
        )

        Spacer(Modifier.height(16.dp))

        // Mode selection
        Text(
            text = stringResource(R.string.training_mode),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        ModeSelector(selected = uiState.mode, onSelect = onSelectMode)

        Spacer(Modifier.height(16.dp))

        // Difficulty selection
        Text(
            text = stringResource(R.string.training_difficulty),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        DifficultySelector(selected = uiState.difficulty, onSelect = onSelectDifficulty)

        Spacer(Modifier.height(16.dp))

        // Best result + recent results — scrollable, fills remaining space
        val hasBest   = uiState.bestResult != null
        val hasRecent = uiState.selectedPlayer != null && uiState.recentResults.isNotEmpty()
        if (hasBest || hasRecent) {
            Text(
                text = stringResource(R.string.training_recent_results),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasBest) {
                    item {
                        BestResultRow(
                            best = uiState.bestResult!!,
                            mode = uiState.mode,
                            dateFormat = dateFormat
                        )
                    }
                }
                items(uiState.recentResults) { session ->
                    RecentResultRow(session = session, mode = uiState.mode, dateFormat = dateFormat)
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Start button pinned at bottom
        Button(
            onClick = onStart,
            enabled = uiState.selectedPlayer != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = Surface3,
                contentColor = Background,
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(R.string.training_start),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }

    if (showPlayerPicker) {
        PlayerPickerSheet(
            allPlayers = uiState.players,
            recentPlayers = emptyList(),
            selectedPlayerIds = setOfNotNull(uiState.selectedPlayer?.id),
            onPlayerSelected = { player ->
                onSelectPlayer(player)
                showPlayerPicker = false
            },
            onDismiss = { showPlayerPicker = false }
        )
    }
}

@Composable
private fun PlayerChip(
    selectedName: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selectedName != null) AccentDim else Surface,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (selectedName != null) Accent else Border2,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedName != null) {
            PlayerAvatar(name = selectedName, size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = selectedName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.training_change_player),
                style = MaterialTheme.typography.labelSmall,
                color = Accent
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.training_select_player),
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeSelector(
    selected: TrainingMode,
    onSelect: (TrainingMode) -> Unit
) {
    val modes = listOf(
        TrainingMode.TARGET_FIELD to R.string.training_mode_target,
        TrainingMode.AROUND_THE_CLOCK to R.string.training_mode_atc,
        TrainingMode.SCORING_ROUNDS to R.string.training_mode_scoring
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        modes.forEach { (mode, labelRes) ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .background(
                        color = if (isSelected) Accent else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Background else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DifficultySelector(
    selected: TrainingDifficulty,
    onSelect: (TrainingDifficulty) -> Unit
) {
    val difficulties = listOf(
        TrainingDifficulty.BEGINNER     to R.string.training_difficulty_beginner,
        TrainingDifficulty.INTERMEDIATE to R.string.training_difficulty_intermediate,
        TrainingDifficulty.PRO          to R.string.training_difficulty_pro
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        difficulties.forEach { (diff, labelRes) ->
            val isSelected = diff == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isSelected) AccentDim else Surface,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) Accent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(diff) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Accent else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BestResultRow(
    best: BestSessionWithPlayer,
    mode: TrainingMode,
    dateFormat: SimpleDateFormat
) {
    val resultText = when (mode) {
        TrainingMode.SCORING_ROUNDS -> "Ø %.1f".format(best.session.result / 10.0)
        else                         -> "${best.session.result} Darts"
    }
    val gold = Color(0xFFFFD700)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(gold.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .border(1.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = gold,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = DmMono,
                fontWeight = FontWeight.Bold,
                color = gold
            )
            Text(
                text = best.playerName,
                style = MaterialTheme.typography.labelSmall,
                color = gold.copy(alpha = 0.75f)
            )
        }
        Text(
            text = dateFormat.format(java.util.Date(best.session.completedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = gold.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun RecentResultRow(
    session: TrainingSession,
    mode: TrainingMode,
    dateFormat: SimpleDateFormat
) {
    val resultText = when (mode) {
        TrainingMode.SCORING_ROUNDS -> "Ø %.1f".format(session.result / 10.0)
        else                         -> "${session.result} Darts"
    }
    val difficultyLabel = when (session.difficulty) {
        TrainingDifficulty.BEGINNER.name     -> TrainingDifficulty.BEGINNER
        TrainingDifficulty.INTERMEDIATE.name -> TrainingDifficulty.INTERMEDIATE
        else                                 -> TrainingDifficulty.PRO
    }
    val diffColor = when (difficultyLabel) {
        TrainingDifficulty.BEGINNER     -> Green
        TrainingDifficulty.INTERMEDIATE -> Amber
        TrainingDifficulty.PRO          -> Red
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = DmMono,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .background(diffColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = session.difficulty,
                style = MaterialTheme.typography.labelSmall,
                color = diffColor
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = dateFormat.format(Date(session.completedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
    }
}
