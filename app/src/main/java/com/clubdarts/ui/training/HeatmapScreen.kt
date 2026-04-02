package com.clubdarts.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*
import kotlin.math.*

@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = TextPrimary
                )
            }
            Text(
                text = stringResource(R.string.heatmap_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = Border)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Player selector
            PlayerSelector(
                players = uiState.players,
                selected = uiState.selectedPlayer,
                onSelect = viewModel::selectPlayer
            )

            // View toggle (Heatmap / Streuung)
            ViewToggle(selected = uiState.view, onSelect = viewModel::setView)

            // Board + overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                when (uiState.view) {
                    AnalyticsView.HEATMAP -> DartboardCanvas(
                        modifier = Modifier.fillMaxSize(),
                        overlay  = uiState.heatmapBitmap
                    )
                    AnalyticsView.DISPERSION -> DartboardCanvas(
                        modifier = Modifier.fillMaxSize(),
                        onDraw   = { cx, cy, scale ->
                            drawDispersionOverlay(cx, cy, scale, uiState.dispersion)
                        }
                    )
                }
                if (uiState.isComputingHeatmap || uiState.isComputingDispersion) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Accent
                    )
                }
            }

            // Filter controls
            when (uiState.view) {
                AnalyticsView.HEATMAP    -> HeatmapFilter(uiState, viewModel)
                AnalyticsView.DISPERSION -> DispersionFilter(uiState, viewModel)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── View toggle ───────────────────────────────────────────────────────────────

@Composable
private fun ViewToggle(selected: AnalyticsView, onSelect: (AnalyticsView) -> Unit) {
    val options = listOf(
        AnalyticsView.HEATMAP    to R.string.heatmap_view_heatmap,
        AnalyticsView.DISPERSION to R.string.heatmap_view_dispersion
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (view, labelRes) ->
            val isSelected = view == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .background(
                        color = if (isSelected) Accent else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(view) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Background else TextSecondary
                )
            }
        }
    }
}

// ── Heatmap filter ────────────────────────────────────────────────────────────

@Composable
private fun HeatmapFilter(state: HeatmapUiState, vm: HeatmapViewModel) {
    when {
        state.selectedPlayer == null -> return
        state.totalGames == 0 -> {
            Text(
                text = stringResource(R.string.heatmap_no_game_data),
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }
        state.totalGames == 1 -> {
            Text(
                text = stringResource(R.string.heatmap_single_game),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        else -> {
            Text(
                text = stringResource(R.string.heatmap_game_range, state.gameFrom, state.gameTo, state.totalGames),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.heatmap_from), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.width(30.dp))
                Slider(
                    value = state.gameFrom.toFloat(),
                    onValueChange = { vm.setGameFrom(it.toInt()) },
                    valueRange = 1f..state.totalGames.toFloat(),
                    steps = (state.totalGames - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.heatmap_to), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.width(30.dp))
                Slider(
                    value = state.gameTo.toFloat(),
                    onValueChange = { vm.setGameTo(it.toInt()) },
                    valueRange = 1f..state.totalGames.toFloat(),
                    steps = (state.totalGames - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
                )
            }
        }
    }
}

// ── Dispersion filter ─────────────────────────────────────────────────────────

@Composable
private fun DispersionFilter(state: HeatmapUiState, vm: HeatmapViewModel) {
    when {
        state.selectedPlayer == null -> return
        state.totalSessions == 0 -> {
            Text(
                text = stringResource(R.string.dispersion_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }
        state.totalSessions == 1 -> {
            Text(
                text = stringResource(R.string.dispersion_single_session),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            if (state.throwCount > 0) {
                Text(
                    text = stringResource(R.string.dispersion_throw_count, state.throwCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
        else -> {
            Text(
                text = stringResource(R.string.dispersion_session_range, state.sessionFrom, state.sessionTo, state.totalSessions),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.heatmap_from), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.width(30.dp))
                Slider(
                    value = state.sessionFrom.toFloat(),
                    onValueChange = { vm.setSessionFrom(it.toInt()) },
                    valueRange = 1f..state.totalSessions.toFloat(),
                    steps = (state.totalSessions - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.heatmap_to), style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.width(30.dp))
                Slider(
                    value = state.sessionTo.toFloat(),
                    onValueChange = { vm.setSessionTo(it.toInt()) },
                    valueRange = 1f..state.totalSessions.toFloat(),
                    steps = (state.totalSessions - 2).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface3)
                )
            }
            if (state.throwCount > 0) {
                Text(
                    text = stringResource(R.string.dispersion_throw_count, state.throwCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            } else if (!state.isComputingDispersion) {
                Text(
                    text = stringResource(R.string.dispersion_no_coords),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

// ── Player selector ───────────────────────────────────────────────────────────

@Composable
private fun PlayerSelector(
    players: List<Player>,
    selected: Player?,
    onSelect: (Player?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null) {
                PlayerAvatar(name = selected.name, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(selected.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            } else {
                Text(stringResource(R.string.heatmap_select_player), style = MaterialTheme.typography.bodyMedium, color = TextTertiary, modifier = Modifier.weight(1f))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Surface2)
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    text = { Text(player.name, color = TextPrimary) },
                    onClick = {
                        onSelect(player)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ── Dispersion overlay drawing ────────────────────────────────────────────────

/**
 * Draws guide rings and the dispersion circle centred on the Triple-20 field.
 */
private fun DrawScope.drawDispersionOverlay(cx: Float, cy: Float, scale: Float, dispersion: Float) {
    fun mm(v: Float) = v * scale

    // Spec centre: T20 field at -90° (top), radius = midpoint of triple ring
    val t20AngleDeg = -90.0
    val tripleR = (TREBLE_INNER_R + TREBLE_OUTER_R) / 2.0
    val refX = cx + mm((cos(t20AngleDeg * PI / 180.0) * tripleR).toFloat())
    val refY = cy + mm((sin(t20AngleDeg * PI / 180.0) * tripleR).toFloat())
    val refCentre = Offset(refX, refY)

    val canvasR   = minOf(size.width, size.height) / 2f
    val minRadius = canvasR * 0.02f
    val maxRadius = canvasR * SCORING_BOUNDARY_NORM

    // Guide rings (10 rings at 0.1..1.0 dispersion steps) — Compose-native so they stay within clip.
    val guideColor  = Color.White.copy(alpha = 0.31f)
    val labelPaint  = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize    = size.minDimension * 0.024f
        textAlign   = android.graphics.Paint.Align.CENTER
        color       = android.graphics.Color.argb(140, 200, 200, 200)
    }

    for (step in 1..10) {
        val d = step / 10f
        val r = minRadius + d * (maxRadius - minRadius)
        drawCircle(
            color  = guideColor,
            radius = r,
            center = refCentre,
            style  = Stroke(width = 1f)
        )
    }

    // Labels: clip native canvas to the composable bounds so text never overflows.
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.clipRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        for (step in 1..10) {
            val d = step / 10f
            val r = minRadius + d * (maxRadius - minRadius)
            val labelY = refY + r + labelPaint.textSize * 0.3f
            canvas.nativeCanvas.drawText("%.1f".format(d), refX, labelY, labelPaint)
        }
        canvas.restore()
    }

    // Dispersion circle
    if (dispersion > 0f) {
        val dispR = minRadius + dispersion * (maxRadius - minRadius)
        drawCircle(
            color  = Color(0xFFE8FF47).copy(alpha = 0.85f),
            radius = dispR,
            center = refCentre,
            style  = Stroke(width = (2.5f * scale).coerceAtLeast(2f))
        )
        // Filled dot at centre
        drawCircle(
            color  = Color(0xFFE8FF47).copy(alpha = 0.6f),
            radius = mm(3f),
            center = refCentre
        )
    }
}
