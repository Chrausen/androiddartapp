package com.clubdarts.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Game
import com.clubdarts.data.model.GameType
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        // Game type filter tabs
        val filterOptions = GameTypeFilter.values().toList()
        val filterLabels = mapOf(
            GameTypeFilter.ALL to stringResource(R.string.history_filter_all),
            GameTypeFilter.X01 to stringResource(R.string.history_filter_x01),
            GameTypeFilter.CRICKET to stringResource(R.string.history_filter_cricket)
        )
        HistoryFilterRow(
            options = filterOptions,
            selected = uiState.gameTypeFilter,
            onSelect = { viewModel.setGameTypeFilter(it) },
            label = { filterLabels[it] ?: it.name }
        )
        Spacer(modifier = Modifier.height(12.dp))

        val filteredSummaries = uiState.gameSummaries.filter { summary ->
            when (uiState.gameTypeFilter) {
                GameTypeFilter.ALL     -> true
                GameTypeFilter.X01     -> summary.game.gameType == GameType.X01
                GameTypeFilter.CRICKET -> summary.game.gameType == GameType.CRICKET
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
        } else if (filteredSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            }
        } else {
            val grouped = filteredSummaries.groupBy { it.dateGroup }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.forEach { (group, items) ->
                    item {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(items) { summary ->
                        GameCard(
                            summary = summary,
                            onClick = { onNavigateToDetail(summary.game.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun <T> HistoryFilterRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .background(
                        color = if (option == selected) Accent else androidx.compose.ui.graphics.Color.Transparent,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = if (option == selected) Background else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun GameCard(summary: GameSummary, onClick: () -> Unit) {
    val dateTimeFormat = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    val dateTimeStr = dateTimeFormat.format(Date(summary.game.createdAt))

    val isTeamGame = summary.game.isTeamGame
    val teamAPlayers = if (isTeamGame) summary.players.filter { p ->
        summary.gamePlayers.firstOrNull { it.playerId == p.id }?.teamIndex == 0
    } else emptyList()
    val teamBPlayers = if (isTeamGame) summary.players.filter { p ->
        summary.gamePlayers.firstOrNull { it.playerId == p.id }?.teamIndex == 1
    } else emptyList()

    val title = if (isTeamGame) {
        teamAPlayers.joinToString(" & ") { it.name } + " vs " + teamBPlayers.joinToString(" & ") { it.name }
    } else {
        summary.players.joinToString(" vs ") { it.name }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Format line
            val isCricketGame = summary.game.gameType == GameType.CRICKET
            val formatText = if (isCricketGame) {
                "Cricket · Best of ${summary.game.legsToWin * 2 - 1} · $dateTimeStr"
            } else {
                "${summary.game.startScore} · ${summary.game.checkoutRule.name.lowercase().replaceFirstChar { it.uppercaseChar() }} out · Best of ${summary.game.legsToWin * 2 - 1} · $dateTimeStr"
            }
            Text(
                text = formatText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DmMono,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = when {
                    isTeamGame -> Red.copy(alpha = 0.15f)
                    summary.game.isSolo -> Surface3
                    summary.game.isRanked -> Accent.copy(alpha = 0.15f)
                    else -> Blue.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = when {
                        isTeamGame -> stringResource(R.string.history_teams)
                        summary.game.isSolo -> stringResource(R.string.history_solo)
                        summary.game.isRanked -> stringResource(R.string.history_ranked)
                        else -> stringResource(R.string.history_casual)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isTeamGame -> Red
                        summary.game.isSolo -> TextSecondary
                        summary.game.isRanked -> Accent
                        else -> Blue
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Border)
            Spacer(modifier = Modifier.height(8.dp))

            if (isTeamGame) {
                // Team A players
                teamAPlayers.forEach { player ->
                    val isWinner = summary.game.winningTeamIndex == 0
                    TeamPlayerRow(name = player.name, teamColor = Red, isWinner = isWinner)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Team B players
                teamBPlayers.forEach { player ->
                    val isWinner = summary.game.winningTeamIndex == 1
                    TeamPlayerRow(name = player.name, teamColor = Blue, isWinner = isWinner)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                summary.players.forEach { player ->
                    val isWinner = player.id == summary.game.winnerId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isWinner) Green else androidx.compose.ui.graphics.Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        PlayerAvatar(name = player.name, size = 28.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isWinner) TextPrimary else TextSecondary,
                            fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isWinner) {
                            Text("W", style = MaterialTheme.typography.labelSmall, color = Green)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun TeamPlayerRow(
    name: String,
    teamColor: androidx.compose.ui.graphics.Color,
    isWinner: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(teamColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        PlayerAvatar(name = name, size = 28.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isWinner) TextPrimary else TextSecondary,
            fontWeight = if (isWinner) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isWinner) {
            Text("W", style = MaterialTheme.typography.labelSmall, color = teamColor)
        }
    }
}
