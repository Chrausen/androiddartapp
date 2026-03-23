package com.clubdarts.ui.rankings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.db.dao.EloMatchDao
import com.clubdarts.data.db.dao.EloMatchEntryDao
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.Player
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchHistoryItem(
    val matchId: Long,
    val playedAt: Long,
    val isWin: Boolean,
    val eloBefore: Double,
    val eloAfter: Double,
    val eloChange: Double
)

data class EloPoint(
    val timestamp: Long,
    val elo: Double
)

data class PlayerDetailUiState(
    val player: Player? = null,
    val matchHistory: List<MatchHistoryItem> = emptyList(),
    val eloGraphPoints: List<EloPoint> = emptyList()
)

@HiltViewModel
class PlayerDetailViewModel @Inject constructor(
    private val playerDao: PlayerDao,
    private val eloMatchEntryDao: EloMatchEntryDao,
    private val eloMatchDao: EloMatchDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playerId: Long = checkNotNull(savedStateHandle["playerId"])

    private val _uiState = MutableStateFlow(PlayerDetailUiState())
    val uiState: StateFlow<PlayerDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val player = playerDao.getPlayerById(playerId)

            // getEntriesForPlayer returns newest first (ORDER BY rowid DESC)
            val entries = eloMatchEntryDao.getEntriesForPlayer(playerId)

            val matchMap = eloMatchDao.getMatchesByIds(entries.map { it.matchId })
                .associateBy { it.id }

            // Match history: newest first
            val matchHistory = entries.mapNotNull { entry ->
                val match = matchMap[entry.matchId] ?: return@mapNotNull null
                MatchHistoryItem(
                    matchId = entry.matchId,
                    playedAt = match.playedAt,
                    isWin = match.winnerId == playerId,
                    eloBefore = entry.eloBefore,
                    eloAfter = entry.eloAfter,
                    eloChange = entry.eloChange
                )
            }

            // Graph: last 100 matches in chronological order (oldest → newest)
            val eloGraphPoints = matchHistory
                .take(100)         // take 100 newest
                .reversed()        // flip to chronological order
                .mapNotNull { item ->
                    val match = matchMap[item.matchId] ?: return@mapNotNull null
                    EloPoint(timestamp = match.playedAt, elo = item.eloAfter)
                }

            _uiState.value = PlayerDetailUiState(
                player = player,
                matchHistory = matchHistory,
                eloGraphPoints = eloGraphPoints
            )
        }
    }
}
