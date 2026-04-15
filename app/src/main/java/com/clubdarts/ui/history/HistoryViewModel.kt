package com.clubdarts.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.Game
import com.clubdarts.data.model.GamePlayer
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.EloRepository
import com.clubdarts.data.repository.GameDetail
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class GameFilter { ALL, CASUAL, RANKED }

// ── Match statistics data classes ─────────────────────────────────────────────

data class MatchPlayerStats(
    val playerId: Long,
    val legsWon: Int,
    val avg3Dart: Double,
    val first9Avg: Double,
    val totalDarts: Int,
    val avgDartsPerLeg: Double,
    val highScore: Int,
    val bestLeg: Int?,
    val worstLeg: Int?,
    val count180s: Int,
    val count140plus: Int,
    val count100plus: Int,
    val visitsMid: Int,      // 60–99 non-bust
    val visitsLowMid: Int,   // 26–59 non-bust
    val visitsBelow26: Int,
    val bustCount: Int,
    val highestCheckout: Int?,
    val checkoutAttempts: Int,
    val successfulCheckouts: Int,
    val visitScores: List<Int>,
    val runningAvgByVisit: List<Double>,
    val dartsByLeg: List<Int>
)

data class MatchOverviewStats(
    val legsPlayed: Int,
    val timePlayedMs: Long,
    val avgLegDurationMs: Long,
    val avgTimePerVisitMs: Long,
    val startTimestamp: Long
)

data class MatchFunStat(val icon: String, val label: String, val description: String)

data class MatchStats(
    val overview: MatchOverviewStats,
    val perPlayer: Map<Long, MatchPlayerStats>,
    val funStats: List<MatchFunStat>,
    val raceChartData: Map<Long, List<Int>>
)

data class GameSummary(
    val game: Game,
    val players: List<Player>,
    val gamePlayers: List<GamePlayer>,
    val dateGroup: String,
    /** playerId → signed Elo change; non-null only for ranked games */
    val eloChanges: Map<Long, Double>? = null
)

data class HistoryUiState(
    val gameSummaries: List<GameSummary> = emptyList(),
    val allSummaries: List<GameSummary> = emptyList(),
    val selectedFilter: GameFilter = GameFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class MatchDetailUiState(
    val detail: GameDetail? = null,
    /** playerId → signed Elo change; non-null only for ranked games */
    val eloChanges: Map<Long, Double>? = null,
    val matchStats: MatchStats? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    private val eloRepository: EloRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(MatchDetailUiState())
    val detailState: StateFlow<MatchDetailUiState> = _detailState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val todayStr = dateFormat.format(Date())
    private val yesterdayStr = dateFormat.format(Date(System.currentTimeMillis() - 86400000L))

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                gameRepository.getAllGames().collect { games ->
                    val finishedGames = games.filter { it.finishedAt != null }
                    if (finishedGames.isEmpty()) {
                        _uiState.update { it.copy(gameSummaries = emptyList(), isLoading = false) }
                        return@collect
                    }

                    // Batch: one query for all game-players, one for all unique players
                    val gameIds = finishedGames.map { it.id }
                    val allGamePlayers = gameRepository.getGamePlayersByGameIds(gameIds)
                        .groupBy { it.gameId }
                    val allPlayerIds = allGamePlayers.values
                        .flatMapTo(mutableSetOf()) { gps -> gps.map { it.playerId } }
                    val playerById = playerRepository.getPlayersByIds(allPlayerIds.toList())
                        .associateBy { it.id }

                    // Batch fetch Elo changes for all ranked games
                    val rankedGameIds = finishedGames.filter { it.isRanked }.map { it.id }
                    val eloChangesByGame = eloRepository.getEloChangesForGames(rankedGameIds)

                    val summaries = finishedGames.map { game ->
                        val gamePlayers = (allGamePlayers[game.id] ?: emptyList())
                            .sortedBy { it.throwOrder }
                        val players = gamePlayers.mapNotNull { playerById[it.playerId] }
                        val dateStr = dateFormat.format(Date(game.createdAt))
                        val group = when (dateStr) {
                            todayStr     -> "Today"
                            yesterdayStr -> "Yesterday"
                            else         -> dateStr
                        }
                        GameSummary(
                            game = game,
                            players = players,
                            gamePlayers = gamePlayers,
                            dateGroup = group,
                            eloChanges = if (game.isRanked) eloChangesByGame[game.id] else null
                        )
                    }
                    _uiState.update { state ->
                        val filtered = applyFilter(summaries, state.selectedFilter)
                        state.copy(gameSummaries = filtered, allSummaries = summaries, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    fun setFilter(filter: GameFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                gameSummaries = applyFilter(state.allSummaries, filter)
            )
        }
    }

    private fun applyFilter(summaries: List<GameSummary>, filter: GameFilter): List<GameSummary> =
        when (filter) {
            GameFilter.ALL -> summaries
            GameFilter.CASUAL -> summaries.filter { !it.game.isRanked }
            GameFilter.RANKED -> summaries.filter { it.game.isRanked }
        }

    fun loadMatchDetail(gameId: Long) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            try {
                val detail = gameRepository.getFullGameDetail(gameId)
                val eloChanges = if (detail?.game?.isRanked == true) {
                    eloRepository.getEloChangesForGame(gameId)
                } else null
                val matchStats = if (detail != null) computeMatchStats(detail) else null
                _detailState.update {
                    it.copy(detail = detail, eloChanges = eloChanges, matchStats = matchStats, isLoading = false)
                }
            } catch (e: Exception) {
                _detailState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    private fun computeMatchStats(detail: GameDetail): MatchStats {
        val game = detail.game
        val legs = detail.legs
        val players = detail.players

        val timePlayedMs = if (game.finishedAt != null) game.finishedAt - game.createdAt else 0L
        val totalVisits = legs.sumOf { it.throws.size }
        val overview = MatchOverviewStats(
            legsPlayed = legs.size,
            timePlayedMs = timePlayedMs,
            avgLegDurationMs = if (legs.isNotEmpty()) timePlayedMs / legs.size else 0L,
            avgTimePerVisitMs = if (totalVisits > 0) timePlayedMs / totalVisits else 0L,
            startTimestamp = game.createdAt
        )

        val perPlayer = players.associate { player ->
            val allThrows = legs.flatMap { it.throws }.filter { it.playerId == player.id }
            val nonBust = allThrows.filter { !it.isBust }
            val legsWon = legs.count { it.leg.winnerId == player.id }

            val avg3Dart = if (nonBust.isNotEmpty()) nonBust.map { it.visitTotal }.average() else 0.0

            val first9Avg = legs.mapNotNull { ld ->
                val legThrows = ld.throws.filter { it.playerId == player.id }.sortedBy { it.visitNumber }
                if (legThrows.isEmpty()) null
                else legThrows.take(3).sumOf { it.visitTotal }.toDouble()
            }.let { if (it.isNotEmpty()) it.average() else 0.0 }

            val totalDarts = allThrows.sumOf { it.dartsUsed }
            val legsParticipated = legs.count { ld -> ld.throws.any { it.playerId == player.id } }
            val avgDartsPerLeg = if (legsParticipated > 0) totalDarts.toDouble() / legsParticipated else 0.0
            val highScore = nonBust.maxOfOrNull { it.visitTotal } ?: 0

            val dartsByLeg = legs.map { ld ->
                ld.throws.filter { it.playerId == player.id }.sumOf { it.dartsUsed }
            }
            val finishedLegDarts = legs.mapIndexedNotNull { i, ld ->
                if (ld.throws.any { it.playerId == player.id }) dartsByLeg[i] else null
            }
            val bestLeg = finishedLegDarts.filter { it > 0 }.minOrNull()
            val worstLeg = finishedLegDarts.maxOrNull()

            val count180s = allThrows.count { it.visitTotal == 180 }
            val count140plus = nonBust.count { it.visitTotal in 140..179 }
            val count100plus = nonBust.count { it.visitTotal in 100..139 }
            val visitsMid = nonBust.count { it.visitTotal in 60..99 }
            val visitsLowMid = nonBust.count { it.visitTotal in 26..59 }
            val visitsBelow26 = nonBust.count { it.visitTotal < 26 }
            val bustCount = allThrows.count { it.isBust }

            val highestCheckout = legs.mapNotNull { ld ->
                if (ld.leg.winnerId != player.id) null
                else ld.throws.filter { it.playerId == player.id && it.isCheckoutAttempt && !it.isBust }
                              .maxOfOrNull { it.visitTotal }
            }.maxOrNull()

            val checkoutAttempts = allThrows.count { it.isCheckoutAttempt }
            val successfulCheckouts = legs.count { ld ->
                ld.leg.winnerId == player.id &&
                ld.throws.any { it.playerId == player.id && it.isCheckoutAttempt && !it.isBust }
            }

            val visitScores = allThrows
                .sortedWith(compareBy({ legs.indexOfFirst { ld -> ld.leg.id == it.legId } }, { it.visitNumber }))
                .map { it.visitTotal }

            val runningAvg = visitScores.mapIndexed { i, _ ->
                val nonZero = visitScores.take(i + 1).filter { it > 0 }
                if (nonZero.isNotEmpty()) nonZero.average() else 0.0
            }

            player.id to MatchPlayerStats(
                playerId = player.id,
                legsWon = legsWon,
                avg3Dart = avg3Dart,
                first9Avg = first9Avg,
                totalDarts = totalDarts,
                avgDartsPerLeg = avgDartsPerLeg,
                highScore = highScore,
                bestLeg = bestLeg,
                worstLeg = worstLeg,
                count180s = count180s,
                count140plus = count140plus,
                count100plus = count100plus,
                visitsMid = visitsMid,
                visitsLowMid = visitsLowMid,
                visitsBelow26 = visitsBelow26,
                bustCount = bustCount,
                highestCheckout = highestCheckout,
                checkoutAttempts = checkoutAttempts,
                successfulCheckouts = successfulCheckouts,
                visitScores = visitScores,
                runningAvgByVisit = runningAvg,
                dartsByLeg = dartsByLeg
            )
        }

        val raceChartData = players.associate { player ->
            var remaining = game.startScore
            val points = mutableListOf(remaining)
            legs.forEachIndexed { legIndex, ld ->
                ld.throws.filter { it.playerId == player.id }.sortedBy { it.visitNumber }
                    .forEach { t ->
                        remaining = if (t.isBust) remaining else (remaining - t.visitTotal).coerceAtLeast(0)
                        points.add(remaining)
                    }
                if (legIndex < legs.lastIndex) {
                    remaining = game.startScore
                    points.add(remaining)
                }
            }
            player.id to points.toList()
        }

        val funStats = buildFunStats(detail, perPlayer, players)
        return MatchStats(overview, perPlayer, funStats, raceChartData)
    }

    private fun buildFunStats(
        detail: GameDetail,
        perPlayer: Map<Long, MatchPlayerStats>,
        players: List<Player>
    ): List<MatchFunStat> {
        val result = mutableListOf<MatchFunStat>()
        fun name(id: Long) = players.firstOrNull { it.id == id }?.name ?: "?"

        perPlayer.maxByOrNull { it.value.avg3Dart }?.let { (id, s) ->
            result += MatchFunStat("🔥", "Match MVP", "${name(id)} · avg ${String.format("%.1f", s.avg3Dart)}")
        }
        perPlayer.minByOrNull { it.value.bestLeg ?: Int.MAX_VALUE }
            ?.takeIf { it.value.bestLeg != null }?.let { (id, s) ->
            result += MatchFunStat("⚡", "Lightning Leg", "${name(id)} won a leg in ${s.bestLeg} darts")
        }
        perPlayer.maxByOrNull { it.value.worstLeg ?: 0 }
            ?.takeIf { it.value.worstLeg != null }?.let { (id, s) ->
            result += MatchFunStat("🐢", "The Grind", "${name(id)} · ${s.worstLeg} darts in worst leg")
        }
        val allCheckouts = detail.legs.flatMap { ld ->
            ld.throws.filter { it.isCheckoutAttempt && !it.isBust && ld.leg.winnerId == it.playerId }
        }
        allCheckouts.minByOrNull { it.visitTotal }?.let { t ->
            result += MatchFunStat("🎯", "Sharpest Finish", "${name(t.playerId)} checked out on ${t.visitTotal}")
        }
        perPlayer.maxByOrNull { it.value.bustCount }
            ?.takeIf { it.value.bustCount > 0 }?.let { (id, s) ->
            result += MatchFunStat("💀", "Bust King", "${name(id)} · ${s.bustCount} busts")
        }
        val firstVisits = players.mapNotNull { p ->
            val firsts = detail.legs.mapNotNull { ld ->
                ld.throws.filter { it.playerId == p.id }.minByOrNull { it.visitNumber }?.visitTotal
            }
            if (firsts.isEmpty()) null else p.id to firsts.average()
        }
        firstVisits.minByOrNull { it.second }?.let { (id, v) ->
            result += MatchFunStat("🧊", "Cold Start", "${name(id)} · avg first visit ${String.format("%.0f", v)}")
        }
        perPlayer.filter { it.value.visitScores.size >= 3 }.minByOrNull { (_, s) ->
            val avg = s.visitScores.average()
            s.visitScores.map { (it - avg) * (it - avg) }.average()
        }?.let { (id, _) ->
            result += MatchFunStat("🔁", "Most Consistent", name(id))
        }
        perPlayer.maxByOrNull { it.value.checkoutAttempts - it.value.successfulCheckouts }
            ?.takeIf { it.value.checkoutAttempts - it.value.successfulCheckouts > 0 }?.let { (id, s) ->
            result += MatchFunStat(
                "😤", "So Close",
                "${name(id)} · ${s.checkoutAttempts - s.successfulCheckouts} missed checkouts"
            )
        }
        return result
    }
}
