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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        Text("Match history", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.gameSummaries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matches yet", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
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
private fun GameCard(summary: GameSummary, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(summary.game.createdAt))

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
                text = summary.players.joinToString(" vs ") { it.name },
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Format line
            Text(
                text = "${summary.game.startScore} · ${summary.game.checkoutRule.name.lowercase().replaceFirstChar { it.uppercaseChar() }} out · Best of ${summary.game.legsToWin * 2 - 1} · $timeStr",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DmMono,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = if (summary.game.isSolo) Surface3 else Blue.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (summary.game.isSolo) "Solo" else "Casual",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (summary.game.isSolo) TextSecondary else Blue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Border)
            Spacer(modifier = Modifier.height(8.dp))

            // Players
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
