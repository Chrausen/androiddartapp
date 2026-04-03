package com.clubdarts.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*

@Composable
fun StatsScreen(
    onNavigateToMatchDetail: (Long) -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortedPlayers = remember(uiState.players, uiState.averages) {
        uiState.players.sortedByDescending { uiState.averages[it.id] ?: 0.0 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.selectedPlayer == null) {
            // Club overview
            item {
                Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(stringResource(R.string.stats_games), uiState.clubTotalGames.toString(), modifier = Modifier.weight(1f))
                    MetricCard(stringResource(R.string.stats_players), uiState.clubTotalPlayers.toString(), modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(stringResource(R.string.stats_club_180s), uiState.clubTotal180s.toString(), modifier = Modifier.weight(1f))
                    MetricCard(stringResource(R.string.stats_best_finish), uiState.clubHighestFinish?.toString() ?: "—", modifier = Modifier.weight(1f))
                }
            }
            item {
                val playtimeValue = "${uiState.clubTotalPlaytimeHours}h ${uiState.clubTotalPlaytimeMinutes}m"
                MetricCard(stringResource(R.string.stats_overall_playtime), playtimeValue, modifier = Modifier.fillMaxWidth())
            }

            // Leaderboard
            item {
                Text(stringResource(R.string.stats_top_averages), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            itemsIndexed(sortedPlayers, key = { _, player -> player.id }) { index, player ->
                val avg = uiState.averages[player.id] ?: 0.0
                LeaderboardRow(
                    rank = index + 1,
                    player = player,
                    average = avg,
                    onClick = { viewModel.selectPlayer(player) }
                )
            }
        } else {
            // ── Player detail view ───────────────────────────────────────────
            val stats = uiState.selectedPlayerStats
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.selectPlayer(null) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    PlayerAvatar(name = uiState.selectedPlayer!!.name)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(uiState.selectedPlayer!!.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
            }

            if (stats != null) {
                // ── Section: Ergebnisse ──────────────────────────────────────
                item { SectionHeader(stringResource(R.string.stats_section_results)) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_games_played), stats.gamesPlayed.toString(), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_wins), stats.wins.toString(), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_second_place), stats.secondPlace.toString(), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_third_place), stats.thirdPlace.toString(), modifier = Modifier.weight(1f))
                    }
                }

                // ── Section: Scoring ─────────────────────────────────────────
                item { SectionHeader(stringResource(R.string.stats_section_scoring)) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_avg_per_dart), "%.2f".format(stats.avgPerDart), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_avg_per_round), "%.1f".format(stats.avgPerRound), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_first9_avg), "%.1f".format(stats.first9Avg), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_highest_checkout), stats.highestFinish.toString(), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_highest_round), stats.highestRound.toString(), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_total_darts), stats.totalDarts.toString(), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_total_score_thrown), stats.totalScoreThrown.toString(), modifier = Modifier.weight(1f))
                    }
                }

                // ── Section: Präzision ───────────────────────────────────────
                item { SectionHeader(stringResource(R.string.stats_section_precision)) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_double_rate), "%.1f%%".format(stats.doubleRate), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_triple_rate), "%.1f%%".format(stats.tripleRate), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_oob_rate), "%.1f%%".format(stats.outOfBoundsRate), modifier = Modifier.weight(1f))
                    }
                }

                // ── Section: Quoten ──────────────────────────────────────────
                item { SectionHeader(stringResource(R.string.stats_section_rates)) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_checkout_pct), "%.0f%%".format(stats.checkoutPercent * 100), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_bust_rate), "%.1f%%".format(stats.bustRate), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_rounds_under10), "%.1f%%".format(stats.roundsUnder10Rate), modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(stringResource(R.string.stats_180s), stats.count180s.toString(), modifier = Modifier.weight(1f))
                        MetricCard(stringResource(R.string.stats_100plus), stats.hundredPlus.toString(), modifier = Modifier.weight(1f))
                    }
                }

                // ── Section: Sozial ──────────────────────────────────────────
                item { SectionHeader(stringResource(R.string.stats_section_social)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SocialStatRow(stringResource(R.string.stats_best_buddy), stats.bestBuddy)
                        SocialStatRow(stringResource(R.string.stats_rival), stats.rival)
                        SocialStatRow(stringResource(R.string.stats_easy_win), stats.easyWin)
                    }
                }

                // ── Chart toggle ─────────────────────────────────────────────
                item { SectionHeader(stringResource(R.string.stats_score_buckets)) }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface2, RoundedCornerShape(10.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val topScoresLabel = stringResource(R.string.stats_top_scores)
                        val bucketsLabel = stringResource(R.string.stats_buckets)
                        listOf(topScoresLabel to false, bucketsLabel to true).forEach { (label, isBuckets) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .background(
                                        color = if (uiState.showBuckets == isBuckets) Accent else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { if (uiState.showBuckets != isBuckets) viewModel.toggleView() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.showBuckets == isBuckets) Background else TextSecondary
                                )
                            }
                        }
                    }
                }

                if (!uiState.showBuckets) {
                    item {
                        Text(stringResource(R.string.stats_most_thrown), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                    items(stats.topScores) { sf ->
                        val maxFreq = stats.topScores.maxOfOrNull { it.frequency } ?: 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                sf.visitTotal.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = DmMono,
                                color = TextSecondary,
                                modifier = Modifier.width(40.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(sf.frequency.toFloat() / maxFreq)
                                    .height(24.dp)
                                    .background(Accent, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.weight((1f - sf.frequency.toFloat() / maxFreq).coerceAtLeast(0.01f)))
                            Text(
                                sf.frequency.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                    }
                } else {
                    item {
                        val total = stats.bucketHigh + stats.bucketMid + stats.bucketLow + stats.bucketVeryLow + stats.bucketBusts
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BucketBar("100–180", stats.bucketHigh, total, Blue)
                            BucketBar("60–99", stats.bucketMid, total, Blue.copy(alpha = 0.7f))
                            BucketBar("40–59", stats.bucketLow, total, Blue.copy(alpha = 0.5f))
                            BucketBar("1–39", stats.bucketVeryLow, total, TextTertiary)
                            BucketBar(stringResource(R.string.stats_busts), stats.bucketBusts, total, Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = Accent,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SocialStatRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = DmMono,
            fontWeight = FontWeight.Bold,
            color = if (value != null) TextPrimary else TextTertiary
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface2),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(value, fontSize = 22.sp, fontFamily = DmMono, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    player: Player,
    average: Double,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = DmMono,
            color = if (rank == 1) Accent else TextTertiary,
            modifier = Modifier.width(32.dp)
        )
        PlayerAvatar(name = player.name, size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Text("%.1f".format(average), fontFamily = DmMono, fontSize = 16.sp, color = TextPrimary)
    }
}

@Composable
private fun BucketBar(label: String, count: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    val pct = if (total > 0) count.toFloat() / total else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.width(60.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .background(Surface3, RoundedCornerShape(4.dp))
        ) {
            if (pct > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct)
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("$count (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.width(60.dp))
    }
}
