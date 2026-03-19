package com.clubdarts.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.Game
import com.clubdarts.data.model.GamePlayer
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.GameDetail
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class GameSummary(
    val game: Game,
    val players: List<Player>,
    val gamePlayers: List<GamePlayer>,
    val dateGroup: String
)

data class HistoryUiState(
    val gameSummaries: List<GameSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class MatchDetailUiState(
    val detail: GameDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository
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
                    val summaries = games.mapNotNull { game ->
                        try {
                            val gamePlayers = gameRepository.getGamePlayers(game.id)
                            val players = playerRepository.getPlayersByIds(gamePlayers.map { it.playerId })
                            val dateStr = dateFormat.format(Date(game.createdAt))
                            val group = when (dateStr) {
                                todayStr      -> "Today"
                                yesterdayStr  -> "Yesterday"
                                else          -> dateStr
                            }
                            GameSummary(game = game, players = players, gamePlayers = gamePlayers, dateGroup = group)
                        } catch (e: Exception) { null }
                    }
                    _uiState.update { it.copy(gameSummaries = summaries, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    fun loadMatchDetail(gameId: Long) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            try {
                val detail = gameRepository.getFullGameDetail(gameId)
                _detailState.update { it.copy(detail = detail, isLoading = false) }
            } catch (e: Exception) {
                _detailState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }
}
