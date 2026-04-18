package com.clubdarts.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.db.dao.ScoreFrequency
import com.clubdarts.data.db.dao.ThrowDao
import com.clubdarts.data.db.dao.TrainingSessionDao
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

enum class LeaderboardMetric {
    AVERAGE, AVG_PER_ROUND, AVG_PER_DART, FIRST_9_AVG,
    WINS, GAMES_PLAYED, LEGS_WON,
    COUNT_180S, HUNDRED_PLUS,
    HIGHEST_FINISH, HIGHEST_ROUND,
    CHECKOUT_PCT, TOTAL_DARTS, TOTAL_SCORE
}

/** Merged per-player leaderboard data (throw stats + game/leg stats). */
data class PlayerLeaderboardStats(
    val average: Double = 0.0,
    val avgPerDart: Double = 0.0,
    val avgPerRound: Double = 0.0,
    val first9Avg: Double = 0.0,
    val wins: Int = 0,
    val gamesPlayed: Int = 0,
    val legsWon: Int = 0,
    val count180s: Int = 0,
    val hundredPlus: Int = 0,
    val highestFinish: Int = 0,
    val highestRound: Int = 0,
    val checkoutPct: Float = 0f,
    val totalDarts: Int = 0,
    val totalScore: Long = 0
)

data class StatsUiState(
    val players: List<Player> = emptyList(),
    val leaderboardStats: Map<Long, PlayerLeaderboardStats> = emptyMap(),
    val leaderboardMetric: LeaderboardMetric = LeaderboardMetric.AVERAGE,
    val selectedPlayer: Player? = null,
    val selectedPlayerStats: PlayerStats? = null,
    val clubTotalGames: Int = 0,
    val clubTotalPlayers: Int = 0,
    val clubTotal180s: Int = 0,
    val clubHighestFinish: Int? = null,
    val clubTotalPlaytimeHours: Long = 0,
    val clubTotalPlaytimeMinutes: Int = 0,
    val showBuckets: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository,
    private val throwDao: ThrowDao,
    private val trainingSessionDao: TrainingSessionDao
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
                val throwRows = throwDao.getAllPlayerLeaderboardRows().associateBy { it.playerId }
                val first9Map = throwDao.getAllPlayerFirst9Avg().associate { it.playerId to it.value }
                val winsMap = gameRepository.getAllPlayerWins()
                val gamesPlayedMap = gameRepository.getAllPlayerGamesPlayed()
                val legsWonMap = gameRepository.getAllPlayerLegsWon()

                val leaderboardStats = players.associate { player ->
                    val row = throwRows[player.id]
                    val checkoutPct = if ((row?.checkoutAttempts ?: 0) > 0)
                        (row!!.successfulCheckouts.toFloat() / row.checkoutAttempts)
                    else 0f
                    player.id to PlayerLeaderboardStats(
                        average = row?.average ?: 0.0,
                        avgPerDart = row?.avgPerDart ?: 0.0,
                        avgPerRound = row?.avgPerRound ?: 0.0,
                        first9Avg = first9Map[player.id] ?: 0.0,
                        wins = winsMap[player.id] ?: 0,
                        gamesPlayed = gamesPlayedMap[player.id] ?: 0,
                        legsWon = legsWonMap[player.id] ?: 0,
                        count180s = row?.count180s ?: 0,
                        hundredPlus = row?.hundredPlus ?: 0,
                        highestFinish = row?.highestFinish ?: 0,
                        highestRound = row?.highestRound ?: 0,
                        checkoutPct = checkoutPct,
                        totalDarts = row?.totalDarts ?: 0,
                        totalScore = row?.totalScoreThrown ?: 0
                    )
                }
                _uiState.update { it.copy(players = players, leaderboardStats = leaderboardStats) }
            }
        }
    }

    fun setLeaderboardMetric(metric: LeaderboardMetric) {
        _uiState.update { it.copy(leaderboardMetric = metric) }
    }

    private fun loadClubStats() {
        viewModelScope.launch {
            try {
                val total180s = throwDao.getTotalClub180s()
                val highestFinish = throwDao.getClubHighestFinish()
                val totalPlayers = playerRepository.getPlayerCount()
                val gamePlaytimeMs = gameRepository.getTotalGamePlaytimeMs()
                val trainingPlaytimeMs = trainingSessionDao.getTotalTrainingPlaytimeMs() ?: 0L
                val totalMs = gamePlaytimeMs + trainingPlaytimeMs
                val totalHours = totalMs / (1000L * 60 * 60)
                val totalMinutes = ((totalMs % (1000L * 60 * 60)) / (1000L * 60)).toInt()
                _uiState.update { it.copy(
                    clubTotal180s = total180s,
                    clubHighestFinish = highestFinish,
                    clubTotalPlayers = totalPlayers,
                    clubTotalPlaytimeHours = totalHours,
                    clubTotalPlaytimeMinutes = totalMinutes
                )}

                gameRepository.getAllGames().collect { games ->
                    val finishedGames = games.filter { it.finishedAt != null && !it.isFunMode }
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
