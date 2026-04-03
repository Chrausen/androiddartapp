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
                _detailState.update { it.copy(detail = detail, eloChanges = eloChanges, isLoading = false) }
            } catch (e: Exception) {
                _detailState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }
}
