package com.clubdarts.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.data.model.TrainingDifficulty
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.data.model.TrainingSession
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrainingSetupScreen(
    uiState: TrainingUiState,
    onSelectPlayer: (Player?) -> Unit,
    onSelectMode: (TrainingMode) -> Unit,
    onSelectDifficulty: (TrainingDifficulty) -> Unit,
    onStart: () -> Unit,
    onOpenHeatmap: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
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
        }

        // Player selection
        item {
            Text(
                text = stringResource(R.string.training_player),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        item {
            PlayerSelectionRow(
                players = uiState.players,
                selectedPlayer = uiState.selectedPlayer,
                onSelectPlayer = onSelectPlayer
            )
        }

        // Mode selection
        item {
            Text(
                text = stringResource(R.string.training_mode),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        item {
            ModeSelector(
                selected = uiState.mode,
                onSelect = onSelectMode
            )
        }

        // Difficulty selection
        item {
            Text(
                text = stringResource(R.string.training_difficulty),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
        item {
            DifficultySelector(
                selected = uiState.difficulty,
                onSelect = onSelectDifficulty
            )
        }

        // Recent results
        if (uiState.selectedPlayer != null && uiState.recentResults.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.training_recent_results),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            items(uiState.recentResults) { session ->
                RecentResultRow(session = session, mode = uiState.mode, dateFormat = dateFormat)
            }
        }

        // Start button
        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStart,
                enabled = uiState.selectedPlayer != null,
                modifier = Modifier
                    .fillMaxWidth()
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
    }
}

@Composable
private fun PlayerSelectionRow(
    players: List<Player>,
    selectedPlayer: Player?,
    onSelectPlayer: (Player?) -> Unit
) {
    if (players.isEmpty()) {
        Text(
            text = stringResource(R.string.training_no_players),
            style = MaterialTheme.typography.bodyMedium,
            color = TextTertiary
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        players.forEach { player ->
            val isSelected = player.id == selectedPlayer?.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSelected) AccentDim else Surface,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) Accent else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectPlayer(if (isSelected) null else player) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerAvatar(name = player.name, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
        TrainingDifficulty.BEGINNER    to R.string.training_difficulty_beginner,
        TrainingDifficulty.INTERMEDIATE to R.string.training_difficulty_intermediate,
        TrainingDifficulty.PRO         to R.string.training_difficulty_pro
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
