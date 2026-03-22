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
    val average: Double,
    val highestFinish: Int,
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
    val games: List<Game>
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
                val averages = players.associate { p ->
                    p.id to (throwDao.getAverageForPlayer(p.id) ?: 0.0)
                }
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
                val avg = throwDao.getAverageForPlayer(player.id) ?: 0.0
                val highestFinish = throwDao.getHighestFinishForPlayer(player.id) ?: 0
                val checkoutAttempts = throwDao.getCheckoutAttemptsForPlayer(player.id)
                val successfulCheckouts = throwDao.getSuccessfulCheckoutsForPlayer(player.id)
                val checkoutPct = if (checkoutAttempts > 0) successfulCheckouts.toFloat() / checkoutAttempts else 0f
                val count180s = throwDao.get180sForPlayer(player.id)
                val hundredPlus = throwDao.getHundredPlusForPlayer(player.id)
                val bucketHigh = throwDao.getBucketHigh(player.id)
                val bucketMid = throwDao.getBucketMid(player.id)
                val bucketLow = throwDao.getBucketLow(player.id)
                val bucketVeryLow = throwDao.getBucketVeryLow(player.id)
                val bucketBusts = throwDao.getBucketBusts(player.id)
                val topScores = throwDao.getVisitScoreFrequencyForPlayer(player.id)

                val legsWon = gameRepository.getLegWinsForPlayer(player.id)

                val games = gameRepository.getAllGames().first().filter { it.finishedAt != null }

                val stats = PlayerStats(
                    player = player,
                    average = avg,
                    highestFinish = highestFinish,
                    checkoutPercent = checkoutPct,
                    count180s = count180s,
                    hundredPlus = hundredPlus,
                    legsWon = legsWon,
                    bucketHigh = bucketHigh,
                    bucketMid = bucketMid,
                    bucketLow = bucketLow,
                    bucketVeryLow = bucketVeryLow,
                    bucketBusts = bucketBusts,
                    topScores = topScores,
                    games = games
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
