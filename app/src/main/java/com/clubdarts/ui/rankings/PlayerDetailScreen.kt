package com.clubdarts.ui.rankings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayerDetailScreen(
    onBack: () -> Unit,
    viewModel: PlayerDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val player = uiState.player

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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (player != null) {
                    Text(
                        text = "${player.wins}W — ${player.losses}L",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (player != null) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = "%.0f".format(player.elo),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                    Text(
                        text = "Elo",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ELO Graph
            item {
                EloGraph(points = uiState.eloGraphPoints)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Match History header
            item {
                Text(
                    text = "Match History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.matchHistory) { item ->
                MatchHistoryRow(item = item)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun EloGraph(points: List<EloPoint>) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val pointCount = points.size

    Column {
        Text(
            text = "Elo Progression",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Last $pointCount match${if (pointCount != 1) "es" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (points.isEmpty()) return@Column

        val minElo = points.minOf { it.elo }
        val maxElo = points.maxOf { it.elo }
        // Add some padding so dots aren't clipped
        val eloRange = (maxElo - minElo).coerceAtLeast(50.0)
        val eloPadding = eloRange * 0.15
        val displayMin = minElo - eloPadding
        val displayMax = maxElo + eloPadding
        val displayRange = displayMax - displayMin

        val accentColor = Accent
        val borderColor = Border
        val textTertiaryColor = TextTertiary
        val baselineElo = 1000.0

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .height(180.dp)
                    .width(44.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "%.0f".format(maxElo),
                    style = MaterialTheme.typography.labelSmall,
                    color = textTertiaryColor,
                    fontSize = 10.sp
                )
                Text(
                    text = "%.0f".format(minElo),
                    style = MaterialTheme.typography.labelSmall,
                    color = textTertiaryColor,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            ) {
                val w = size.width
                val h = size.height
                val n = points.size

                fun xOf(i: Int) = if (n == 1) w / 2f else i * w / (n - 1).toFloat()
                fun yOf(elo: Double) = h - ((elo - displayMin) / displayRange * h).toFloat()

                // Grid lines at top, bottom, and baseline (1000) if in range
                drawLine(color = borderColor, start = Offset(0f, yOf(displayMax)), end = Offset(w, yOf(displayMax)), strokeWidth = 1.dp.toPx())
                drawLine(color = borderColor, start = Offset(0f, yOf(displayMin)), end = Offset(w, yOf(displayMin)), strokeWidth = 1.dp.toPx())
                if (baselineElo in displayMin..displayMax) {
                    val y = yOf(baselineElo)
                    drawLine(
                        color = borderColor.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 4.dp.toPx()))
                    )
                }

                // Line path
                val path = Path()
                points.forEachIndexed { i, point ->
                    val x = xOf(i)
                    val y = yOf(point.elo)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = accentColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Dots
                points.forEachIndexed { i, point ->
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(xOf(i), yOf(point.elo))
                    )
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFF0E0F11),
                        radius = 2.dp.toPx(),
                        center = Offset(xOf(i), yOf(point.elo))
                    )
                }
            }
        }

        // X-axis: first and last date
        if (points.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(points.first().timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = textTertiaryColor,
                    fontSize = 10.sp
                )
                Text(
                    text = dateFormat.format(Date(points.last().timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = textTertiaryColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun MatchHistoryRow(item: MatchHistoryItem) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val changeColor = if (item.eloChange >= 0) Green else Red
    val changePrefix = if (item.eloChange >= 0) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // W/L badge
            Surface(
                color = if (item.isWin) Green.copy(alpha = 0.15f) else Red.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (item.isWin) "W" else "L",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isWin) Green else Red
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Date
            Text(
                text = dateFormat.format(Date(item.playedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )

            // ELO change
            Text(
                text = "$changePrefix${"%.0f".format(item.eloChange)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = changeColor,
                modifier = Modifier.width(52.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ELO after
            Text(
                text = "%.0f".format(item.eloAfter),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.width(44.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
