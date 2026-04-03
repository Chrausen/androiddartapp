package com.clubdarts.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            FilterDropdown(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp)
            }
        } else if (uiState.gameSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
            }
        } else {
            val grouped = uiState.gameSummaries.groupBy { it.dateGroup }
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
                    items(items, key = { it.game.id }) { summary ->
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
private fun FilterDropdown(
    selectedFilter: GameFilter,
    onFilterSelected: (GameFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val label = stringResource(
        when (selectedFilter) {
            GameFilter.ALL -> R.string.history_filter_all
            GameFilter.CASUAL -> R.string.history_filter_casual
            GameFilter.RANKED -> R.string.history_filter_ranked
        }
    )

    Box {
        Surface(
            color = Surface3,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Surface
        ) {
            GameFilter.entries.forEach { filter ->
                val itemLabel = stringResource(
                    when (filter) {
                        GameFilter.ALL -> R.string.history_filter_all
                        GameFilter.CASUAL -> R.string.history_filter_casual
                        GameFilter.RANKED -> R.string.history_filter_ranked
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (filter == selectedFilter) Accent else TextPrimary
                        )
                    },
                    onClick = {
                        onFilterSelected(filter)
                        expanded = false
                    }
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
            Text(
                text = "${summary.game.startScore} · ${summary.game.checkoutRule.name.lowercase().replaceFirstChar { it.uppercaseChar() }} out · Best of ${summary.game.legsToWin * 2 - 1} · $dateTimeStr",
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
