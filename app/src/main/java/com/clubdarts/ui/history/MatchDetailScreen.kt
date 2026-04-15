package com.clubdarts.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Throw
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val playerColors = listOf(Blue, Green, Amber, Red, Accent, Color(0xFFFF69B4))

private fun formatDuration(ms: Long): String {
    val mins = ms / 60_000
    val secs = (ms % 60_000) / 1000
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

private fun visitScoreColor(score: Int): Color = when {
    score == 180 -> Color.White
    score >= 140 -> Accent
    score >= 100 -> Blue
    score >= 60  -> Green
    score >= 26  -> Amber
    else         -> Red
}

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
    var selectedPlayerIndex by remember { mutableIntStateOf(0) }

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back), tint = TextPrimary)
            }
            Text(stringResource(R.string.match_detail_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
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
        val eloChanges = uiState.eloChanges
        val matchStats = uiState.matchStats

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Match header ─────────────────────────────────────────────────
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
                                PlayerAvatarNameColumn(
                                    player = players[0],
                                    eloChange = eloChanges?.get(players[0].id)
                                )
                                val p1Legs = detail.legs.count { it.leg.winnerId == players[0].id }
                                val p2Legs = detail.legs.count { it.leg.winnerId == players[1].id }
                                Text(
                                    text = "$p1Legs — $p2Legs",
                                    fontSize = 28.sp,
                                    fontFamily = DmMono,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                PlayerAvatarNameColumn(
                                    player = players[1],
                                    eloChange = eloChanges?.get(players[1].id)
                                )
                            }
                        } else {
                            players.forEach { player ->
                                val legsWon = detail.legs.count { it.leg.winnerId == player.id }
                                val eloChange = eloChanges?.get(player.id)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PlayerAvatar(name = player.name, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                                    if (eloChange != null) {
                                        val sign = if (eloChange >= 0) "+" else ""
                                        Text(
                                            text = "$sign${eloChange.toInt()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = DmMono,
                                            color = if (eloChange >= 0) Green else Red
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(stringResource(R.string.match_detail_legs_won, legsWon), style = MaterialTheme.typography.labelMedium, fontFamily = DmMono, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // ── Match overview stats ──────────────────────────────────────────
            if (matchStats != null) {
                item {
                    MatchOverviewCard(stats = matchStats.overview)
                }

                // ── Leaderboard bars ──────────────────────────────────────────
                item {
                    Text(stringResource(R.string.match_stats_leaderboard), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                item {
                    LeaderboardBars(
                        players = players,
                        perPlayer = matchStats.perPlayer,
                        playerColors = playerColors
                    )
                }

                // ── Race chart ────────────────────────────────────────────────
                item {
                    Text(stringResource(R.string.match_stats_race_chart), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                item {
                    RaceChart(
                        players = players,
                        raceChartData = matchStats.raceChartData,
                        startScore = game.startScore,
                        playerColors = playerColors
                    )
                }

                // ── Per-player stats ──────────────────────────────────────────
                item {
                    Text(stringResource(R.string.match_stats_player_stats), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                item {
                    // Player selector tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        players.forEachIndexed { i, player ->
                            val isSelected = selectedPlayerIndex == i
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) playerColors.getOrElse(i) { Accent } else Surface2,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedPlayerIndex = i }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    player.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Background else TextSecondary
                                )
                            }
                        }
                    }
                }
                item {
                    val playerStats = matchStats.perPlayer[players.getOrNull(selectedPlayerIndex)?.id]
                    if (playerStats != null) {
                        PlayerStatsSection(
                            stats = playerStats,
                            playerColor = playerColors.getOrElse(selectedPlayerIndex) { Accent }
                        )
                    }
                }

                // ── Fun highlights ────────────────────────────────────────────
                if (matchStats.funStats.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.match_stats_highlights), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    item {
                        FunHighlightsRow(funStats = matchStats.funStats)
                    }
                }
            }

            // ── Leg breakdown (existing) ──────────────────────────────────────
            item {
                Text(stringResource(R.string.match_detail_leg_breakdown), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                            Text(stringResource(R.string.match_detail_leg, leg.legNumber), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text(stringResource(R.string.match_detail_visits, totalVisits), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
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
                                            Text(stringResource(R.string.match_detail_won_visits, playerThrows.size), style = MaterialTheme.typography.labelSmall, color = Green)
                                        } else {
                                            val remaining = game.startScore - playerThrows.sumOf { it.visitTotal }
                                            Text(stringResource(R.string.match_detail_left, remaining, playerThrows.size), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // ── Visit log (existing) ──────────────────────────────────────────
            item {
                Text(stringResource(R.string.match_detail_visit_log), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
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
                                stringResource(R.string.match_detail_leg, i + 1),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Background else TextSecondary
                            )
                        }
                    }
                }
            }

            val selectedLeg = detail.legs.getOrNull(selectedLegIndex)
            if (selectedLeg != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.match_detail_player), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(2f))
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

// ── Match overview card ───────────────────────────────────────────────────────

@Composable
private fun MatchOverviewCard(stats: MatchOverviewStats) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.match_stats_overview), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OverviewCell(label = stringResource(R.string.match_stats_legs_played), value = "${stats.legsPlayed}", modifier = Modifier.weight(1f))
                OverviewCell(label = stringResource(R.string.match_stats_time_played), value = formatDuration(stats.timePlayedMs), modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OverviewCell(label = stringResource(R.string.match_stats_avg_leg), value = formatDuration(stats.avgLegDurationMs), modifier = Modifier.weight(1f))
                OverviewCell(label = stringResource(R.string.match_stats_time_of_day), value = timeFormat.format(Date(stats.startTimestamp)), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OverviewCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = DmMono, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

// ── Leaderboard bars ──────────────────────────────────────────────────────────

@Composable
private fun LeaderboardBars(
    players: List<com.clubdarts.data.model.Player>,
    perPlayer: Map<Long, MatchPlayerStats>,
    playerColors: List<Color>
) {
    val sorted = players.sortedByDescending { perPlayer[it.id]?.legsWon ?: 0 }
    val maxLegs = sorted.maxOfOrNull { perPlayer[it.id]?.legsWon ?: 0 }.takeIf { it != null && it > 0 } ?: 1

    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sorted.forEach { player ->
                val stats = perPlayer[player.id] ?: return@forEach
                val colorIndex = players.indexOf(player)
                val color = playerColors.getOrElse(colorIndex) { Accent }
                val fraction = stats.legsWon.toFloat() / maxLegs.toFloat()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Surface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                                .background(color, RoundedCornerShape(4.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${stats.legsWon}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = DmMono,
                        color = color
                    )
                }
            }
        }
    }
}

// ── Race chart ────────────────────────────────────────────────────────────────

@Composable
private fun RaceChart(
    players: List<com.clubdarts.data.model.Player>,
    raceChartData: Map<Long, List<Int>>,
    startScore: Int,
    playerColors: List<Color>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                players.forEachIndexed { i, player ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(playerColors.getOrElse(i) { Accent }, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(player.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val w = size.width
                val h = size.height
                val maxX = raceChartData.values.maxOfOrNull { it.size }?.toFloat()?.coerceAtLeast(2f) ?: 2f

                // Grid lines
                val gridSteps = 5
                for (i in 0..gridSteps) {
                    val y = h * (1f - i.toFloat() / gridSteps)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                players.forEachIndexed { idx, player ->
                    val points = raceChartData[player.id] ?: return@forEachIndexed
                    val color = playerColors.getOrElse(idx) { Accent }
                    if (points.size < 2) return@forEachIndexed

                    val path = Path()
                    points.forEachIndexed { i, score ->
                        val x = if (maxX <= 1f) 0f else w * i / (maxX - 1f)
                        val y = h * (1f - score.toFloat() / startScore)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}

// ── Per-player stats section ──────────────────────────────────────────────────

@Composable
private fun PlayerStatsSection(stats: MatchPlayerStats, playerColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Stats grid
            PlayerStatGrid(stats = stats)

            // Visit score bar chart
            if (stats.visitScores.isNotEmpty()) {
                Text(stringResource(R.string.match_stats_visit_chart), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                VisitBarChart(visitScores = stats.visitScores)
            }

            // Score distribution donut
            Text(stringResource(R.string.match_stats_distribution), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            ScoreDistributionRow(stats = stats)

            // Darts per leg chart
            if (stats.dartsByLeg.any { it > 0 }) {
                Text(stringResource(R.string.match_stats_darts_per_leg), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                DartsPerLegChart(dartsByLeg = stats.dartsByLeg, playerColor = playerColor)
            }

            // Running average
            if (stats.runningAvgByVisit.size >= 2) {
                Text(stringResource(R.string.match_stats_running_avg), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                RunningAverageChart(runningAvg = stats.runningAvgByVisit, playerColor = playerColor)
            }
        }
    }
}

@Composable
private fun PlayerStatGrid(stats: MatchPlayerStats) {
    val rows = listOf(
        stringResource(R.string.match_stats_avg) to String.format("%.1f", stats.avg3Dart),
        stringResource(R.string.match_stats_first9) to String.format("%.1f", stats.first9Avg),
        stringResource(R.string.match_stats_total_darts) to "${stats.totalDarts}",
        stringResource(R.string.match_stats_avg_darts_leg) to String.format("%.1f", stats.avgDartsPerLeg),
        stringResource(R.string.match_stats_high_score) to "${stats.highScore}",
        stringResource(R.string.match_stats_best_leg) to (stats.bestLeg?.let { "$it ${stringResource(R.string.match_stats_darts_suffix)}" } ?: "—"),
        stringResource(R.string.match_stats_worst_leg) to (stats.worstLeg?.let { "$it ${stringResource(R.string.match_stats_darts_suffix)}" } ?: "—"),
        stringResource(R.string.match_stats_180s) to "${stats.count180s}",
        stringResource(R.string.match_stats_140plus) to "${stats.count140plus}",
        stringResource(R.string.match_stats_100plus) to "${stats.count100plus}",
        stringResource(R.string.match_stats_below_26) to "${stats.visitsBelow26}",
        stringResource(R.string.match_stats_busts) to "${stats.bustCount}",
        stringResource(R.string.match_stats_highest_checkout) to (stats.highestCheckout?.toString() ?: "—"),
        stringResource(R.string.match_stats_checkout_attempts) to "${stats.checkoutAttempts}"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.chunked(2).forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { idx, (label, value) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.weight(1f))
                        Text(value, style = MaterialTheme.typography.labelSmall, fontFamily = DmMono, color = TextPrimary)
                    }
                    if (pair.size == 2 && idx == 0) {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}

// ── Visit bar chart ───────────────────────────────────────────────────────────

@Composable
private fun VisitBarChart(visitScores: List<Int>) {
    val maxScore = visitScores.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        visitScores.forEach { score ->
            val fraction = score.toFloat() / maxScore.toFloat()
            val color = visitScoreColor(score)
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                    .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
            )
        }
    }
}

// ── Score distribution ────────────────────────────────────────────────────────

@Composable
private fun ScoreDistributionRow(stats: MatchPlayerStats) {
    data class Band(val label: String, val count: Int, val color: Color)
    val bands = listOf(
        Band("180", stats.count180s, Color.White),
        Band("140+", stats.count140plus, Accent),
        Band("100+", stats.count100plus, Blue),
        Band("60+", stats.visitsMid, Green),
        Band("26+", stats.visitsLowMid, Amber),
        Band("<26", stats.visitsBelow26, Red),
        Band("Bust", stats.bustCount, Red.copy(alpha = 0.5f))
    ).filter { it.count > 0 }

    val total = bands.sumOf { it.count }.coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            bands.forEach { band ->
                val frac = band.count.toFloat() / total
                Box(
                    modifier = Modifier
                        .weight(frac)
                        .fillMaxHeight()
                        .background(band.color)
                )
            }
        }
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            bands.forEach { band ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(band.color, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${band.label} (${band.count})", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }
        }
    }
}

// ── Darts per leg chart ───────────────────────────────────────────────────────

@Composable
private fun DartsPerLegChart(dartsByLeg: List<Int>, playerColor: Color) {
    val maxDarts = dartsByLeg.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        dartsByLeg.forEachIndexed { i, darts ->
            if (darts > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(darts.toFloat() / maxDarts.toFloat())
                                .background(playerColor.copy(alpha = 0.7f), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                    Text("L${i + 1}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TextTertiary)
                }
            }
        }
    }
}

// ── Running average line chart ────────────────────────────────────────────────

@Composable
private fun RunningAverageChart(runningAvg: List<Double>, playerColor: Color) {
    val maxAvg = runningAvg.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val w = size.width
        val h = size.height
        val n = runningAvg.size.toFloat().coerceAtLeast(2f)

        // Grid
        drawLine(
            color = Color.White.copy(alpha = 0.06f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.dp.toPx()
        )

        val path = Path()
        runningAvg.forEachIndexed { i, avg ->
            val x = w * i / (n - 1f)
            val y = h * (1f - (avg / maxAvg).toFloat())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = playerColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ── Fun highlights ────────────────────────────────────────────────────────────

@Composable
private fun FunHighlightsRow(funStats: List<MatchFunStat>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(funStats) { stat ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stat.icon, fontSize = 20.sp)
                    Text(stat.label, style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(stat.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }
    }
}

// ── Existing helper composables ───────────────────────────────────────────────

@Composable
private fun PlayerAvatarNameColumn(
    player: com.clubdarts.data.model.Player,
    eloChange: Double? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlayerAvatar(name = player.name, size = 48.dp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(player.name, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        if (eloChange != null) {
            val sign = if (eloChange >= 0) "+" else ""
            Text(
                text = "$sign${eloChange.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DmMono,
                color = if (eloChange >= 0) Green else Red
            )
        }
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
                color = if (throw_.isBust) Red.copy(alpha = 0.12f) else Color.Transparent
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
