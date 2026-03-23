package com.clubdarts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.Game
import com.clubdarts.data.model.GamePlayer
import com.clubdarts.data.model.Leg
import com.clubdarts.data.model.Player
import com.clubdarts.data.model.Throw
import com.clubdarts.data.model.SettingsKeys
import com.clubdarts.data.repository.EloRepository
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.SettingsRepository
import kotlin.random.Random
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val showDeleteConfirm: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isGeneratingDebugData: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playerDao: PlayerDao,
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository,
    private val eloRepository: EloRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun requestDeleteAll() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDeleteConfirm() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDeleteAll() {
        viewModelScope.launch {
            gameRepository.deleteAll()
            playerDao.deleteAll()
            settingsRepository.set(SettingsKeys.RECENT_PLAYER_IDS, "")
            _uiState.update { it.copy(showDeleteConfirm = false, deleteSuccess = true) }
        }
    }

    fun clearDeleteSuccess() {
        _uiState.update { it.copy(deleteSuccess = false) }
    }

    fun generateDebugData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingDebugData = true) }
            try {
                val names = listOf(
                    "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank",
                    "Grace", "Henry", "Iris", "Jack", "Karen", "Liam",
                    "Mia", "Noah", "Olivia", "Paul", "Quinn", "Rachel",
                    "Sam", "Tina"
                )
                val playerIds = names.map { name ->
                    playerDao.insertPlayer(Player(name = name))
                }

                if (!settingsRepository.getRankingEnabled()) {
                    settingsRepository.setRankingEnabled(true)
                }

                val now = System.currentTimeMillis()
                val monthAgo = now - 30L * 24 * 60 * 60 * 1000

                // Generate sorted timestamps so ELO history is chronological
                val timestamps = (0 until 500)
                    .map { monthAgo + Random.nextLong(now - monthAgo) }
                    .sorted()

                timestamps.forEachIndexed { index, timestamp ->
                    val isRanked = Random.nextFloat() < 0.5f
                    val participantCount = Random.nextInt(2, 5)
                    val participantIds = playerIds.shuffled().take(participantCount)
                    val winnerId = participantIds.random()
                    val duration = Random.nextLong(5 * 60_000L, 30 * 60_000L)

                    val gameId = gameRepository.insertGame(
                        Game(
                            startScore = 501,
                            checkoutRule = CheckoutRule.DOUBLE,
                            legsToWin = 1,
                            createdAt = timestamp,
                            finishedAt = timestamp + duration,
                            winnerId = winnerId
                        )
                    )
                    gameRepository.insertGamePlayers(
                        participantIds.mapIndexed { i, pid ->
                            GamePlayer(gameId = gameId, playerId = pid, throwOrder = i, teamIndex = -1)
                        }
                    )
                    val legId = gameRepository.insertLeg(
                        Leg(
                            gameId = gameId,
                            legNumber = 1,
                            startedAt = timestamp,
                            finishedAt = timestamp + duration,
                            winnerId = winnerId
                        )
                    )

                    // Insert throws for each participant
                    val startScore = 501
                    participantIds.forEach { pid ->
                        var remaining = startScore
                        var visitNum = 1
                        while (remaining > 0) {
                            val dartsUsed = Random.nextInt(1, 4)
                            val maxScore = minOf(remaining, 180)
                            val visitTotal = if (remaining <= 40 && pid == winnerId) {
                                // Simulate a checkout for the winner
                                remaining
                            } else {
                                Random.nextInt(0, minOf(maxScore + 1, 141))
                            }
                            val isBust = visitTotal > remaining
                            val effective = if (isBust) 0 else visitTotal
                            // Split visitTotal across up to dartsUsed darts
                            val d1 = if (dartsUsed == 1) effective else Random.nextInt(0, effective + 1)
                            val d2 = if (dartsUsed <= 1) 0 else if (dartsUsed == 2) effective - d1 else Random.nextInt(0, effective - d1 + 1)
                            val d3 = if (dartsUsed <= 2) 0 else effective - d1 - d2
                            gameRepository.insertThrow(
                                Throw(
                                    legId = legId,
                                    playerId = pid,
                                    visitNumber = visitNum,
                                    dart1Score = d1, dart1Mult = 1,
                                    dart2Score = d2, dart2Mult = if (dartsUsed >= 2) 1 else 0,
                                    dart3Score = d3, dart3Mult = if (dartsUsed >= 3) 1 else 0,
                                    dartsUsed = dartsUsed,
                                    visitTotal = effective,
                                    isBust = isBust,
                                    isCheckoutAttempt = remaining <= 170,
                                    createdAt = timestamp + visitNum * 5_000L
                                )
                            )
                            remaining -= effective
                            if (remaining <= 0 || (pid != winnerId && visitNum >= Random.nextInt(8, 20))) break
                            visitNum++
                        }
                    }

                    if (isRanked) {
                        val players = playerDao.getPlayersByIds(participantIds)
                        eloRepository.recordMatch(players, winnerId, playedAt = timestamp)
                    }
                }
            } finally {
                _uiState.update { it.copy(isGeneratingDebugData = false) }
            }
        }
    }
}
