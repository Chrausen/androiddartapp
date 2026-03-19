package com.clubdarts.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.data.model.Throw
import com.clubdarts.ui.game.DartInput
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchDetailScreen(
    gameId: Long,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) {
        viewModel.loadMatchDetail(gameId)
    }

    var selectedLegIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Match detail", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }

        val detail = uiState.detail ?: return@Column
        val game = detail.game
        val players = detail.players

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Match header
            item {
                val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                val dateStr = dateFormat.format(Date(game.createdAt))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = DmMono,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${game.startScore} · ${game.checkoutRule.name.lowercase().replaceFirstChar { it.uppercaseChar() }} out",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (players.size == 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatarNameColumn(player = players[0])
                                // Score
                                val p1Legs = detail.legs.count { it.leg.winnerId == players[0].id }
                                val p2Legs = detail.legs.count { it.leg.winnerId == players[1].id }
                                Text(
                                    text = "$p1Legs — $p2Legs",
                                    fontSize = 28.sp,
                                    fontFamily = DmMono,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                PlayerAvatarNameColumn(player = players[1])
                            }
                        } else {
                            players.forEach { player ->
                                val legsWon = detail.legs.count { it.leg.winnerId == player.id }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PlayerAvatar(name = player.name, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                    Text("$legsWon legs", style = MaterialTheme.typography.labelMedium, fontFamily = DmMono, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Leg by leg
            item {
                Text("Leg breakdown", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(detail.legs) { legDetail ->
                val leg = legDetail.leg
                val totalVisits = legDetail.throws.size
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Leg ${leg.legNumber}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("$totalVisits visits", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        players.forEach { player ->
                            val isWinner = leg.winnerId == player.id
                            val playerThrows = legDetail.throws.filter { it.playerId == player.id }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isWinner) Green.copy(alpha = 0.15f) else Surface2,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(player.name, style = MaterialTheme.typography.labelMedium, color = if (isWinner) Green else TextSecondary)
                                        if (isWinner) {
                                            Text("Won · ${playerThrows.size} visits", style = MaterialTheme.typography.labelSmall, color = Green)
                                        } else {
                                            val remaining = game.startScore - playerThrows.sumOf { it.visitTotal }
                                            Text("left: $remaining · ${playerThrows.size} visits", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // Full visit log
            item {
                Text("Visit log", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }

            // Leg selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.legs.forEachIndexed { i, _ ->
                        val isSelected = selectedLegIndex == i
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Accent else Surface2,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedLegIndex = i }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Leg ${i + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Background else TextSecondary
                            )
                        }
                    }
                }
            }

            val selectedLeg = detail.legs.getOrNull(selectedLegIndex)
            if (selectedLeg != null) {
                // Table header
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Player", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(2f))
                        Text("D1", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("D2", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("D3", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Border)
                }
                items(selectedLeg.throws.sortedBy { it.visitNumber }) { throw_ ->
                    val player = players.firstOrNull { it.id == throw_.playerId }
                    VisitLogRow(throw_ = throw_, playerName = player?.name ?: "?")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlayerAvatarNameColumn(player: com.clubdarts.data.model.Player) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlayerAvatar(name = player.name, size = 48.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(player.name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
    }
}

@Composable
private fun VisitLogRow(throw_: Throw, playerName: String) {
    fun dartLabel(score: Int, mult: Int): String = when {
        score == 0 -> "—"
        mult == 2  -> "D$score"
        mult == 3  -> "T$score"
        else       -> "$score"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (throw_.isBust) Red.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent
            )
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(playerName, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(2f))
        Text(dartLabel(throw_.dart1Score, throw_.dart1Mult), fontFamily = DmMono, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(dartLabel(throw_.dart2Score, throw_.dart2Mult), fontFamily = DmMono, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(dartLabel(throw_.dart3Score, throw_.dart3Mult), fontFamily = DmMono, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (throw_.isBust) {
            Text("BUST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Red, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        } else {
            Text(throw_.visitTotal.toString(), fontFamily = DmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (throw_.visitTotal == 180) Accent else TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}
