package com.clubdarts.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.db.dao.ScoreFrequency
import com.clubdarts.data.db.dao.ThrowDao
import com.clubdarts.data.model.Game
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerStats(
    val player: Player,
    // ── Existing stats (also used in leaderboard avg) ─────────────────────
    val average: Double,
    val highestFinish: Int,        // Stat 9: Höchstes Checkout
    val checkoutPercent: Float,
    val count180s: Int,
    val hundredPlus: Int,
    val legsWon: Int,
    val bucketHigh: Int,
    val bucketMid: Int,
    val bucketLow: Int,
    val bucketVeryLow: Int,
    val bucketBusts: Int,
    val topScores: List<ScoreFrequency>,
    val games: List<Game>,
    // ── New stats 1–4: game results ───────────────────────────────────────
    val gamesPlayed: Int,          // Stat 1
    val wins: Int,                 // Stat 2
    val secondPlace: Int,          // Stat 3
    val thirdPlace: Int,           // Stat 4
    // ── New stats 5–10: scoring ───────────────────────────────────────────
    val totalDarts: Int,           // Stat 5
    val avgPerDart: Double,        // Stat 6
    val avgPerRound: Double,       // Stat 7 (excl. bust + checkout)
    val first9Avg: Double,         // Stat 8
    val highestRound: Int,         // Stat 10
    // ── New stats 11–15: rates ────────────────────────────────────────────
    val doubleRate: Float,         // Stat 11
    val tripleRate: Float,         // Stat 12
    val outOfBoundsRate: Float,    // Stat 13
    val roundsUnder10Rate: Float,  // Stat 14
    val bustRate: Float,           // Stat 15
    // ── New stats 16–18: social ───────────────────────────────────────────
    val bestBuddy: String?,        // Stat 16
    val rival: String?,            // Stat 17
    val easyWin: String?,          // Stat 18
    // ── New stats: totals ─────────────────────────────────────────────────
    val totalScoreThrown: Long     // Gesamt geworfene Punktzahl
)

data class StatsUiState(
    val players: List<Player> = emptyList(),
    val averages: Map<Long, Double> = emptyMap(),
    val selectedPlayer: Player? = null,
    val selectedPlayerStats: PlayerStats? = null,
    val clubTotalGames: Int = 0,
    val clubTotalPlayers: Int = 0,
    val clubTotal180s: Int = 0,
    val clubHighestFinish: Int? = null,
    val showBuckets: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
    private val throwDao: ThrowDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadClubStats()
        observePlayers()
    }

    private fun observePlayers() {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                val averages = throwDao.getAllPlayerAverages().associate { it.playerId to it.average }
                _uiState.update { it.copy(players = players, averages = averages) }
            }
        }
    }

    private fun loadClubStats() {
        viewModelScope.launch {
            try {
                val total180s = throwDao.getTotalClub180s()
                val highestFinish = throwDao.getClubHighestFinish()
                val totalPlayers = playerRepository.getPlayerCount()
                _uiState.update { it.copy(
                    clubTotal180s = total180s,
                    clubHighestFinish = highestFinish,
                    clubTotalPlayers = totalPlayers
                )}

                gameRepository.getAllGames().collect { games ->
                    val finishedGames = games.filter { it.finishedAt != null }
                    _uiState.update { it.copy(
                        clubTotalGames = finishedGames.count { !it.isTeamGame }
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun selectPlayer(player: Player?) {
        if (player == null) {
            _uiState.update { it.copy(selectedPlayer = null, selectedPlayerStats = null) }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, selectedPlayer = player) }

                // Basic aggregate (existing)
                val agg = throwDao.getPlayerStatsAggregate(player.id)
                val checkoutPct = if (agg.checkoutAttempts > 0) {
                    agg.successfulCheckouts.toFloat() / agg.checkoutAttempts
                } else 0f

                // Extended throw stats (stats 5–14)
                val ext = throwDao.getExtendedPlayerStats(player.id)
                val first9Avg = throwDao.getFirst9Avg(player.id) ?: 0.0

                // Rates derived from ext counts
                val doubleRate = if (ext.totalDarts > 0) ext.doubleCount.toFloat() / ext.totalDarts * 100f else 0f
                val tripleRate = if (ext.totalDarts > 0) ext.tripleCount.toFloat() / ext.totalDarts * 100f else 0f
                val oobRate = if (ext.totalDarts > 0) ext.outOfBoundsCount.toFloat() / ext.totalDarts * 100f else 0f
                val under10Rate = if (ext.nonBustRoundsCount > 0) ext.roundsUnder10Count.toFloat() / ext.nonBustRoundsCount * 100f else 0f

                // Stat 15: bustRounds / (bustRounds + winningRounds)
                val bustDenom = agg.bucketBusts + agg.successfulCheckouts
                val bustRate = if (bustDenom > 0) agg.bucketBusts.toFloat() / bustDenom * 100f else 0f

                // Game-level stats (stats 1–4)
                val gamesPlayed = gameRepository.getGamesPlayed(player.id)
                val wins = gameRepository.getWins(player.id)
                val secondPlace = gameRepository.getSecondPlaceCount(player.id)
                val thirdPlace = gameRepository.getThirdPlaceCount(player.id)

                // Social stats (stats 16–18)
                val bestBuddy = gameRepository.getBestBuddy(player.id)
                val rival = gameRepository.getRival(player.id)
                val easyWin = gameRepository.getEasyWin(player.id)

                val topScores = throwDao.getVisitScoreFrequencyForPlayer(player.id)
                val legsWon = gameRepository.getLegWinsForPlayer(player.id)
                val games = gameRepository.getAllGames().first().filter { it.finishedAt != null }

                val stats = PlayerStats(
                    player = player,
                    average = agg.average ?: 0.0,
                    highestFinish = agg.highestFinish ?: 0,
                    checkoutPercent = checkoutPct,
                    count180s = agg.count180s,
                    hundredPlus = agg.hundredPlus,
                    legsWon = legsWon,
                    bucketHigh = agg.bucketHigh,
                    bucketMid = agg.bucketMid,
                    bucketLow = agg.bucketLow,
                    bucketVeryLow = agg.bucketVeryLow,
                    bucketBusts = agg.bucketBusts,
                    topScores = topScores,
                    games = games,
                    gamesPlayed = gamesPlayed,
                    wins = wins,
                    secondPlace = secondPlace,
                    thirdPlace = thirdPlace,
                    totalDarts = ext.totalDarts,
                    avgPerDart = ext.avgPerDart ?: 0.0,
                    avgPerRound = ext.avgPerRound ?: 0.0,
                    first9Avg = first9Avg,
                    highestRound = ext.highestRound ?: 0,
                    doubleRate = doubleRate,
                    tripleRate = tripleRate,
                    outOfBoundsRate = oobRate,
                    roundsUnder10Rate = under10Rate,
                    bustRate = bustRate,
                    bestBuddy = bestBuddy,
                    rival = rival,
                    easyWin = easyWin,
                    totalScoreThrown = ext.totalScoreThrown
                )
                _uiState.update { it.copy(selectedPlayerStats = stats, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    fun toggleView() {
        _uiState.update { it.copy(showBuckets = !it.showBuckets) }
    }
}
