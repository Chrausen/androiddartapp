package com.clubdarts.ui.training

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.Player
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.data.repository.PlayerRepository
import com.clubdarts.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.*

enum class AnalyticsView { HEATMAP, DISPERSION }

data class HeatmapUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayer: Player? = null,
    val view: AnalyticsView = AnalyticsView.HEATMAP,

    // Heatmap
    val totalGames: Int = 0,
    val gameFrom: Int = 1,
    val gameTo: Int = 1,
    val heatmapBitmap: ImageBitmap? = null,
    val isComputingHeatmap: Boolean = false,

    // Dispersion
    val totalSessions: Int = 0,
    val sessionFrom: Int = 1,
    val sessionTo: Int = 1,
    val dispersion: Float = 0f,
    val throwCount: Int = 0,
    val isComputingDispersion: Boolean = false
)

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    private var heatmapJob: Job? = null
    private var dispersionJob: Job? = null

    init {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                _uiState.update { it.copy(players = players) }
            }
        }
    }

    fun selectPlayer(player: Player?) {
        _uiState.update { it.copy(
            selectedPlayer = player,
            heatmapBitmap  = null,
            dispersion     = 0f,
            throwCount     = 0,
            gameFrom       = 1,
            gameTo         = 1,
            sessionFrom    = 1,
            sessionTo      = 1
        )}
        if (player != null) {
            when (_uiState.value.view) {
                AnalyticsView.HEATMAP    -> loadGameCount(player)
                AnalyticsView.DISPERSION -> loadSessionsAndComputeDispersion(player)
            }
        }
    }

    fun setView(view: AnalyticsView) {
        _uiState.update { it.copy(view = view) }
        val player = _uiState.value.selectedPlayer ?: return
        when (view) {
            AnalyticsView.HEATMAP    -> computeHeatmap()
            AnalyticsView.DISPERSION -> loadSessionsAndComputeDispersion(player)
        }
    }

    fun setGameFrom(from: Int) {
        val state = _uiState.value
        val clamped = from.coerceIn(1, state.gameTo)
        _uiState.update { it.copy(gameFrom = clamped) }
        computeHeatmap()
    }

    fun setGameTo(to: Int) {
        val state = _uiState.value
        val clamped = to.coerceIn(state.gameFrom, state.totalGames.coerceAtLeast(1))
        _uiState.update { it.copy(gameTo = clamped) }
        computeHeatmap()
    }

    fun setSessionFrom(from: Int) {
        val state = _uiState.value
        val clamped = from.coerceIn(1, state.sessionTo)
        _uiState.update { it.copy(sessionFrom = clamped) }
        computeDispersion()
    }

    fun setSessionTo(to: Int) {
        val state = _uiState.value
        val clamped = to.coerceIn(state.sessionFrom, state.totalSessions.coerceAtLeast(1))
        _uiState.update { it.copy(sessionTo = clamped) }
        computeDispersion()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun loadGameCount(player: Player) {
        viewModelScope.launch {
            val total = trainingRepository.getTotalFinishedGameCountForPlayer(player.id)
            _uiState.update { it.copy(
                totalGames = total,
                gameFrom   = 1,
                gameTo     = total.coerceAtLeast(1)
            )}
            if (total > 0) computeHeatmap()
        }
    }

    private fun loadSessionsAndComputeDispersion(player: Player) {
        viewModelScope.launch {
            // Get ALL sessions (TARGET_FIELD + AROUND_THE_CLOCK) for dispersion
            val tfSessions  = trainingRepository.getSessionsChronological(player.id, TrainingMode.TARGET_FIELD)
            val atcSessions = trainingRepository.getSessionsChronological(player.id, TrainingMode.AROUND_THE_CLOCK)
            val allSessions = (tfSessions + atcSessions).sortedBy { it.completedAt }
            val total = allSessions.size
            _uiState.update { it.copy(
                totalSessions = total,
                sessionFrom   = 1,
                sessionTo     = total.coerceAtLeast(1)
            )}
            if (total > 0) computeDispersion()
        }
    }

    private fun computeHeatmap() {
        val state  = _uiState.value
        val player = state.selectedPlayer ?: return
        heatmapJob?.cancel()
        heatmapJob = viewModelScope.launch {
            _uiState.update { it.copy(isComputingHeatmap = true) }
            val coords = trainingRepository.getDartCoordinatesForHeatmap(player.id, state.gameFrom, state.gameTo)
            val bitmap = withContext(Dispatchers.Default) {
                renderHeatmap(coords.map { Pair(it.x.toFloat(), it.y.toFloat()) })
            }
            _uiState.update { it.copy(heatmapBitmap = bitmap, isComputingHeatmap = false) }
        }
    }

    private fun computeDispersion() {
        val state  = _uiState.value
        val player = state.selectedPlayer ?: return
        dispersionJob?.cancel()
        dispersionJob = viewModelScope.launch {
            _uiState.update { it.copy(isComputingDispersion = true) }
            val tfSessions  = trainingRepository.getSessionsChronological(player.id, TrainingMode.TARGET_FIELD)
            val atcSessions = trainingRepository.getSessionsChronological(player.id, TrainingMode.AROUND_THE_CLOCK)
            val allSessions = (tfSessions + atcSessions).sortedBy { it.completedAt }
            val selected    = allSessions
                .filterIndexed { i, _ -> i + 1 in state.sessionFrom..state.sessionTo }
                .map { it.id }
            val coords = trainingRepository.getCoordinatesForSessions(selected)
            val (disp, count) = withContext(Dispatchers.Default) {
                computeDispersionValue(coords.map { Pair(Pair(it.targetX.toFloat(), it.targetY.toFloat()), Pair(it.actualX.toFloat(), it.actualY.toFloat())) })
            }
            _uiState.update { it.copy(dispersion = disp, throwCount = count, isComputingDispersion = false) }
        }
    }
}

// ── Heatmap rendering (off-thread) ────────────────────────────────────────────

private const val GRID_SIZE = 220

/**
 * Renders a heatmap bitmap from a list of (mmX, mmY) dart positions.
 * Returns null if there are no points.
 */
private fun renderHeatmap(points: List<Pair<Float, Float>>): ImageBitmap? {
    if (points.isEmpty()) return null

    val grid   = Array(GRID_SIZE) { FloatArray(GRID_SIZE) }
    val sigma  = GRID_SIZE * 0.02f         // ≈ 4.4 pixels
    val sigSq  = sigma * sigma
    val radius = (3 * sigma).toInt()

    // Scale: map mm coordinates to grid pixels.
    // SCALE_BOUNDARY_R mm == half the grid (centre to edge).
    val scale = (GRID_SIZE / 2f) / SCALE_BOUNDARY_R

    for ((mmX, mmY) in points) {
        val gx = (mmX * scale + GRID_SIZE / 2f).toInt()
        val gy = (mmY * scale + GRID_SIZE / 2f).toInt()
        for (dy in -radius..radius) {
            val py = gy + dy
            if (py < 0 || py >= GRID_SIZE) continue
            for (dx in -radius..radius) {
                val px = gx + dx
                if (px < 0 || px >= GRID_SIZE) continue
                grid[py][px] += exp(-(dx * dx + dy * dy).toFloat() / (2f * sigSq))
            }
        }
    }

    val maxVal = grid.maxOf { row -> row.maxOrNull() ?: 0f }.takeIf { it > 0f } ?: return null

    val bitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888)
    val halfG  = GRID_SIZE / 2f
    val scoringRadiusPx = SCORING_BOUNDARY_NORM * GRID_SIZE / 2f

    for (py in 0 until GRID_SIZE) {
        for (px in 0 until GRID_SIZE) {
            val normR = sqrt((px - halfG) * (px - halfG) + (py - halfG) * (py - halfG)) / (GRID_SIZE / 2f)
            if (normR > SCORING_BOUNDARY_NORM + 0.01f) {
                bitmap.setPixel(px, py, android.graphics.Color.TRANSPARENT)
                continue
            }
            val t = grid[py][px] / maxVal
            if (t < 0.01f) {
                bitmap.setPixel(px, py, android.graphics.Color.TRANSPARENT)
            } else {
                bitmap.setPixel(px, py, heatColor(t, alpha = 0.8f))
            }
        }
    }
    return bitmap.asImageBitmap()
}

/** Classic heat-map gradient: blue → cyan → green → yellow → red */
private fun heatColor(t: Float, alpha: Float): Int {
    val (r, g, b) = when {
        t < 0.25f -> {
            val f = t / 0.25f
            Triple(0f, f, 1f)
        }
        t < 0.50f -> {
            val f = (t - 0.25f) / 0.25f
            Triple(0f, 1f, 1f - f)
        }
        t < 0.75f -> {
            val f = (t - 0.50f) / 0.25f
            Triple(f, 1f, 0f)
        }
        else -> {
            val f = (t - 0.75f) / 0.25f
            Triple(1f, 1f - f, 0f)
        }
    }
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val ri = (r * 255).toInt().coerceIn(0, 255)
    val gi = (g * 255).toInt().coerceIn(0, 255)
    val bi = (b * 255).toInt().coerceIn(0, 255)
    return (a shl 24) or (ri shl 16) or (gi shl 8) or bi
}

// ── Dispersion computation (off-thread) ──────────────────────────────────────

/**
 * Computes the normalised RMS dispersion from a list of (target, actual) coordinate pairs in mm.
 * Returns (dispersion 0..1, throwCount).
 */
private fun computeDispersionValue(
    pairs: List<Pair<Pair<Float, Float>, Pair<Float, Float>>>
): Pair<Float, Int> {
    if (pairs.isEmpty()) return Pair(0f, 0)
    val meanSqDist = pairs.sumOf { (target, actual) ->
        val dx = (actual.first  - target.first).toDouble()
        val dy = (actual.second - target.second).toDouble()
        dx * dx + dy * dy
    } / pairs.size
    val rmsMm = sqrt(meanSqDist).toFloat()
    val dispersion = (rmsMm / DOUBLE_OUTER_R).coerceIn(0f, 1f)
    return Pair(dispersion, pairs.size)
}
