package com.clubdarts.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubdarts.R
import com.clubdarts.data.db.dao.LeaderboardEntry
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
    onOpenHeatmap: () -> Unit,
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
            TextButton(
                onClick = onOpenHeatmap,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.training_analytics),
                    color = Accent,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
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

        // Leaderboard area — two columns on wide screens, stacked on phones
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val isWide = screenWidthDp >= 600

        val hasPersonal = uiState.selectedPlayer != null && uiState.recentResults.isNotEmpty()
        val hasClub = uiState.clubLeaderboard.isNotEmpty()

        if (hasPersonal || hasClub) {
            if (isWide) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left: personal stats
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        PersonalSectionHeader(uiState.selectedPlayer?.name)
                        Spacer(Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.recentResults) { session ->
                                RecentResultRow(session = session, mode = uiState.mode, dateFormat = dateFormat)
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                        }
                    }
                    // Right: club leaderboard
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        ClubSectionHeader()
                        Spacer(Modifier.height(6.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uiState.clubLeaderboard.size) { index ->
                                ClubLeaderboardRow(
                                    rank = index + 1,
                                    entry = uiState.clubLeaderboard[index],
                                    mode = uiState.mode,
                                    isCurrentPlayer = uiState.clubLeaderboard[index].playerId == uiState.selectedPlayer?.id
                                )
                            }
                            item { Spacer(Modifier.height(4.dp)) }
                        }
                    }
                }
            } else {
                // Stacked layout for phones
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasPersonal) {
                        PersonalSectionHeader(uiState.selectedPlayer?.name)
                        Spacer(Modifier.height(2.dp))
                        uiState.recentResults.forEach { session ->
                            RecentResultRow(session = session, mode = uiState.mode, dateFormat = dateFormat)
                        }
                    }
                    if (hasClub) {
                        if (hasPersonal) Spacer(Modifier.height(8.dp))
                        ClubSectionHeader()
                        Spacer(Modifier.height(2.dp))
                        uiState.clubLeaderboard.forEachIndexed { index, entry ->
                            ClubLeaderboardRow(
                                rank = index + 1,
                                entry = entry,
                                mode = uiState.mode,
                                isCurrentPlayer = entry.playerId == uiState.selectedPlayer?.id
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
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
private fun PersonalSectionHeader(playerName: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (playerName != null) {
            PlayerAvatar(name = playerName, size = 18.dp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = playerName ?: "Your Stats",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ClubSectionHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Club Leaderboard",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ClubLeaderboardRow(
    rank: Int,
    entry: LeaderboardEntry,
    mode: TrainingMode,
    isCurrentPlayer: Boolean
) {
    val gold   = Color(0xFFFFD700)
    val silver = Color(0xFFB0BEC5)
    val bronze = Color(0xFFCD7F32)
    val rankColor = when (rank) {
        1    -> gold
        2    -> silver
        3    -> bronze
        else -> TextTertiary
    }
    val resultText = when (mode) {
        TrainingMode.SCORING_ROUNDS -> "Ø %.1f".format(entry.bestResult / 10.0)
        else                        -> "${entry.bestResult} darts"
    }
    val bgColor = if (isCurrentPlayer) AccentDim else Surface
    val borderColor = if (isCurrentPlayer) Accent else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DmMono,
            fontWeight = FontWeight.Bold,
            color = rankColor,
            modifier = Modifier.width(28.dp)
        )
        PlayerAvatar(name = entry.playerName, size = 24.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = entry.playerName,
            style = MaterialTheme.typography.bodySmall,
            color = if (isCurrentPlayer) Accent else TextPrimary,
            fontWeight = if (isCurrentPlayer) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = resultText,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DmMono,
            color = if (isCurrentPlayer) Accent else TextSecondary
        )
    }
}

@Composable
private fun RecentResultRow(
    session: TrainingSession,
    mode: TrainingMode,
    dateFormat: SimpleDateFormat
) {
    val diff = when (session.difficulty) {
        TrainingDifficulty.BEGINNER.name     -> TrainingDifficulty.BEGINNER
        TrainingDifficulty.INTERMEDIATE.name -> TrainingDifficulty.INTERMEDIATE
        else                                 -> TrainingDifficulty.PRO
    }
    val diffColor = when (diff) {
        TrainingDifficulty.BEGINNER     -> Green
        TrainingDifficulty.INTERMEDIATE -> Amber
        TrainingDifficulty.PRO          -> Red
    }

    // For Scoring Rounds: determine win/loss against the target average
    val scoringMeta: Pair<String, Boolean>? = if (mode == TrainingMode.SCORING_ROUNDS) {
        val achieved = session.result / 10.0
        val target   = diff.targetAvg
        "Ø %.1f  /  Ziel: %d".format(achieved, target) to (achieved >= target)
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (scoringMeta != null) scoringMeta.first else "${session.result} Darts",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = DmMono,
                color = TextPrimary
            )
        }
        if (scoringMeta != null) {
            val (winLabel, winColor) = if (scoringMeta.second)
                "Win"  to Green
            else
                "Loss" to Red
            Box(
                modifier = Modifier
                    .background(winColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = winLabel, style = MaterialTheme.typography.labelSmall, color = winColor)
            }
            Spacer(Modifier.width(6.dp))
        }
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
