package com.clubdarts.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.*
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.util.CheckoutCalculator
import com.clubdarts.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GameScreen { SETUP, LIVE, RESULT }

data class DartInput(val score: Int, val multiplier: Int) {
    val value: Int get() = score * multiplier
    fun label(): String = when {
        score == 0      -> "Miss"
        multiplier == 2 -> "D$score"
        multiplier == 3 -> "T$score"
        else            -> "$score"
    }
}

data class VisitRecord(
    val playerId: Long,
    val playerName: String,
    val dart1: DartInput?,
    val dart2: DartInput?,
    val dart3: DartInput?,
    val total: Int,
    val isBust: Boolean,
    val scoreAfterVisit: Int
)

data class SetupDefaults(
    val startScore: Int = 301,
    val checkoutRule: CheckoutRule = CheckoutRule.DOUBLE,
    val legsToWin: Int = 1,
    val randomOrder: Boolean = false,
    val recentPlayerIds: List<Long> = emptyList()
)

data class GameUiState(
    val screen: GameScreen = GameScreen.SETUP,
    val config: GameConfig? = null,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val scores: Map<Long, Int> = emptyMap(),
    val legWins: Map<Long, Int> = emptyMap(),
    val currentDarts: List<DartInput> = emptyList(),
    val pendingMultiplier: Int = 1,
    val visitHistory: List<VisitRecord> = emptyList(),
    val currentLegNumber: Int = 1,
    val gameId: Long? = null,
    val legId: Long? = null,
    val winnerId: Long? = null,
    val checkoutHint: String? = null,
    val setupDefaults: SetupDefaults = SetupDefaults(),
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val gameSaved: Boolean = false,
    val isTtsMuted: Boolean = false,
    val showHistory: Boolean = false,
    val setupSelectedPlayerIds: List<Long> = emptyList()
)

@HiltViewModel
class GameViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository,
    private val playerRepository: PlayerRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val ttsManager = TtsManager(application)
    private var ttsScoreSettings: List<TtsScoreSetting> = emptyList()

    init {
        ttsManager.init()
        loadSetupDefaults()
        viewModelScope.launch {
            settingsRepository.observeTtsScoreSettings().collect { settings ->
                ttsScoreSettings = settings
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    fun toggleTtsMute() { _uiState.update { it.copy(isTtsMuted = !it.isTtsMuted) } }

    fun toggleHistory() {
        val newValue = !_uiState.value.showHistory
        _uiState.update { it.copy(showHistory = newValue) }
        viewModelScope.launch { settingsRepository.setShowHistory(newValue) }
    }

    fun updateSetupSelectedPlayers(ids: List<Long>) {
        _uiState.update { it.copy(setupSelectedPlayerIds = ids) }
    }

    fun loadSetupDefaults() {
        viewModelScope.launch {
            try {
                val startScore = settingsRepository.getLastStartScore()
                val checkoutRule = settingsRepository.getLastCheckoutRule()
                val legsToWin = settingsRepository.getLastLegsToWin()
                val randomOrder = settingsRepository.getLastRandomOrder()
                val recentIds = settingsRepository.getRecentPlayerIds()
                val showHistory = settingsRepository.getShowHistory()
                _uiState.update { it.copy(
                    showHistory = showHistory,
                    setupDefaults = SetupDefaults(
                        startScore = startScore,
                        checkoutRule = checkoutRule,
                        legsToWin = legsToWin,
                        randomOrder = randomOrder,
                        recentPlayerIds = recentIds
                    )
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun setMultiplier(mult: Int) {
        _uiState.update { it.copy(pendingMultiplier = mult) }
    }

    fun recordDart(score: Int) {
        val state = _uiState.value
        val mult = state.pendingMultiplier

        // Triple 25 doesn't exist
        if (score == 25 && mult == 3) {
            _uiState.update { it.copy(
                pendingMultiplier = 1,
                snackbarMessage = "Triple bull doesn't exist"
            )}
            return
        }

        val dart = DartInput(score = score, multiplier = mult)
        val newDarts = state.currentDarts + dart
        _uiState.update { it.copy(currentDarts = newDarts, pendingMultiplier = 1) }

        val currentState = _uiState.value
        val currentPlayer = currentState.players.getOrNull(currentState.currentPlayerIndex) ?: return
        val remaining = currentState.scores[currentPlayer.id] ?: return
        val soFar = currentState.currentDarts.sumOf { it.value }
        val rule = currentState.config?.checkoutRule ?: CheckoutRule.DOUBLE

        when {
            soFar > remaining -> resolveVisit()  // definitive bust
            remaining - soFar == 1 && (rule == CheckoutRule.DOUBLE || rule == CheckoutRule.TRIPLE) -> resolveVisit()  // can't finish on 1
            remaining - soFar == 0 -> resolveVisit()  // checkout
            currentState.currentDarts.size >= 3 -> resolveVisit()  // all 3 darts thrown
            else -> updateCheckoutHint()
        }
    }

    fun recordMiss() {
        val dart = DartInput(score = 0, multiplier = 0)
        val state = _uiState.value
        val newDarts = state.currentDarts + dart
        _uiState.update { it.copy(currentDarts = newDarts, pendingMultiplier = 1) }
        if (_uiState.value.currentDarts.size >= 3) {
            resolveVisit()
        } else {
            updateCheckoutHint()
        }
    }

    private fun updateCheckoutHint() {
        val state = _uiState.value
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        val remaining = state.scores[currentPlayer.id] ?: return
        val soFar = state.currentDarts.sumOf { it.value }
        val remainingAfterCurrentDarts = remaining - soFar
        val dartsLeft = 3 - state.currentDarts.size
        val rule = state.config?.checkoutRule ?: CheckoutRule.DOUBLE
        val hint = if (remainingAfterCurrentDarts > 0 && remainingAfterCurrentDarts <= 170) {
            CheckoutCalculator.suggest(remainingAfterCurrentDarts, rule, maxDarts = dartsLeft)
        } else null
        _uiState.update { it.copy(checkoutHint = hint) }
    }

    private fun resolveVisit() {
        val state = _uiState.value
        val config = state.config ?: return
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        val remaining = state.scores[currentPlayer.id] ?: return
        val darts = state.currentDarts
        val visitTotal = darts.sumOf { it.value }
        val isCheckoutAttempt = CheckoutCalculator.isCheckoutPossible(remaining, config.checkoutRule)

        // Determine bust
        val afterVisit = remaining - visitTotal
        val isBust: Boolean
        val effectiveTotal: Int

        if (afterVisit < 0) {
            isBust = true
            effectiveTotal = 0
        } else if (afterVisit == 1 && (config.checkoutRule == CheckoutRule.DOUBLE || config.checkoutRule == CheckoutRule.TRIPLE)) {
            isBust = true
            effectiveTotal = 0
        } else if (afterVisit == 0) {
            // Check checkout validity
            val lastDart = darts.last()
            val validCheckout = CheckoutCalculator.isValidCheckout(
                lastDartScore = lastDart.score,
                lastDartMult = lastDart.multiplier,
                remainingAfter = 0,
                rule = config.checkoutRule
            )
            if (!validCheckout) {
                isBust = true
                effectiveTotal = 0
            } else {
                isBust = false
                effectiveTotal = visitTotal
            }
        } else {
            isBust = false
            effectiveTotal = visitTotal
        }

        // Compute new score (needed for both VisitRecord and score map)
        val newScore = if (isBust) remaining else remaining - effectiveTotal

        // Build visit record
        val visitRecord = VisitRecord(
            playerId = currentPlayer.id,
            playerName = currentPlayer.name,
            dart1 = darts.getOrNull(0),
            dart2 = darts.getOrNull(1),
            dart3 = darts.getOrNull(2),
            total = if (isBust) 0 else effectiveTotal,
            isBust = isBust,
            scoreAfterVisit = newScore
        )

        // Persist throw to DB
        viewModelScope.launch {
            try {
                val legId = state.legId ?: return@launch
                val d1 = darts.getOrNull(0) ?: DartInput(0, 0)
                val d2 = darts.getOrNull(1) ?: DartInput(0, 0)
                val d3 = darts.getOrNull(2) ?: DartInput(0, 0)
                val existingThrows = gameRepository.getThrowsForPlayerInLeg(legId, currentPlayer.id)
                val visitNumber = existingThrows.size + 1

                val throw_ = Throw(
                    legId = legId,
                    playerId = currentPlayer.id,
                    visitNumber = visitNumber,
                    dart1Score = d1.score, dart1Mult = d1.multiplier,
                    dart2Score = d2.score, dart2Mult = d2.multiplier,
                    dart3Score = d3.score, dart3Mult = d3.multiplier,
                    dartsUsed = darts.size,
                    visitTotal = if (isBust) 0 else effectiveTotal,
                    isBust = isBust,
                    isCheckoutAttempt = isCheckoutAttempt
                )
                gameRepository.insertThrow(throw_)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }

        // newScore already computed above
        val newScores = state.scores.toMutableMap().also { it[currentPlayer.id] = newScore }

        // Check if leg won
        val isCheckout = !isBust && newScore == 0

        // TTS
        val announceTotal = if (isBust) 0 else effectiveTotal
        val customPhrases = if (!isBust && !isCheckout) {
            ttsScoreSettings.find { it.score == announceTotal }?.phrases ?: emptyList()
        } else emptyList()
        if (!_uiState.value.isTtsMuted) {
            ttsManager.announce(
                visitTotal = announceTotal,
                isBust = isBust,
                isCheckout = isCheckout,
                customPhrases = customPhrases
            )
        }

        // Advance player
        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size

        val newHistory = (listOf(visitRecord) + state.visitHistory).take(20)

        _uiState.update { it.copy(
            scores = newScores,
            currentDarts = emptyList(),
            pendingMultiplier = 1,
            currentPlayerIndex = nextPlayerIndex,
            visitHistory = newHistory
        )}
        updateCheckoutHint()

        if (isCheckout) {
            onLegWon(currentPlayer.id)
        }
    }

    fun undoLastDart() {
        val state = _uiState.value
        if (state.currentDarts.isNotEmpty()) {
            // Remove last dart from current visit (in-memory only)
            val newDarts = state.currentDarts.dropLast(1)
            _uiState.update { it.copy(currentDarts = newDarts, pendingMultiplier = 1) }
            updateCheckoutHint()
        } else {
            // Undo across visit boundary — delete last throw from DB and restore
            viewModelScope.launch {
                try {
                    val legId = state.legId ?: return@launch
                    val lastThrow = gameRepository.getLastThrowInLeg(legId) ?: return@launch

                    // Restore previous player
                    val prevPlayerIndex = if (state.currentPlayerIndex == 0) {
                        state.players.size - 1
                    } else {
                        state.currentPlayerIndex - 1
                    }
                    val prevPlayer = state.players.getOrNull(prevPlayerIndex) ?: return@launch

                    // Restore score to pre-visit value
                    val prevScore = (state.scores[prevPlayer.id] ?: 0) + lastThrow.visitTotal

                    // Reconstruct darts from the throw record, then drop the last one
                    val allDarts = buildList {
                        if (lastThrow.dartsUsed >= 1) add(DartInput(lastThrow.dart1Score, lastThrow.dart1Mult))
                        if (lastThrow.dartsUsed >= 2) add(DartInput(lastThrow.dart2Score, lastThrow.dart2Mult))
                        if (lastThrow.dartsUsed >= 3) add(DartInput(lastThrow.dart3Score, lastThrow.dart3Mult))
                    }
                    val restoredDarts = allDarts.dropLast(1)

                    gameRepository.deleteThrow(lastThrow)

                    // Restore visit history
                    val newHistory = state.visitHistory.drop(1)

                    _uiState.update { it.copy(
                        scores = it.scores.toMutableMap().also { m -> m[prevPlayer.id] = prevScore },
                        currentPlayerIndex = prevPlayerIndex,
                        currentDarts = restoredDarts,
                        pendingMultiplier = 1,
                        visitHistory = newHistory
                    )}
                    updateCheckoutHint()
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            }
        }
    }

    fun startGame(config: GameConfig) {
        viewModelScope.launch {
            try {
                settingsRepository.setLastGameConfig(
                    score = config.startScore,
                    rule = config.checkoutRule,
                    legs = config.legsToWin,
                    random = config.isSolo
                )
                config.playerIds.forEach { settingsRepository.addRecentPlayer(it) }

                val gameId = gameRepository.startGame(config)
                val players = playerRepository.getPlayersByIds(config.playerIds)
                val leg = gameRepository.getActiveLeg(gameId)

                val scores = players.associate { it.id to config.startScore }
                val legWins = players.associate { it.id to 0 }

                _uiState.update { it.copy(
                    screen = GameScreen.LIVE,
                    config = config,
                    players = players,
                    gameId = gameId,
                    legId = leg?.id,
                    scores = scores,
                    legWins = legWins,
                    currentPlayerIndex = 0,
                    currentDarts = emptyList(),
                    pendingMultiplier = 1,
                    visitHistory = emptyList(),
                    currentLegNumber = 1,
                    winnerId = null,
                    checkoutHint = null
                )}
                updateCheckoutHint()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun onLegWon(winnerId: Long) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val legId = state.legId ?: return@launch
                val gameId = state.gameId ?: return@launch
                val config = state.config ?: return@launch

                gameRepository.finishLeg(legId, winnerId)

                val newLegWins = state.legWins.toMutableMap()
                newLegWins[winnerId] = (newLegWins[winnerId] ?: 0) + 1

                // Check if game is won
                val legsWon = newLegWins[winnerId] ?: 0
                if (legsWon >= config.legsToWin) {
                    // Don't auto-save — the user will choose to save on the result screen.
                    _uiState.update { it.copy(
                        screen = GameScreen.RESULT,
                        legWins = newLegWins,
                        winnerId = winnerId
                    )}
                } else {
                    // Start new leg
                    val newLegNumber = state.currentLegNumber + 1
                    val newLeg = com.clubdarts.data.model.Leg(
                        gameId = gameId,
                        legNumber = newLegNumber
                    )
                    val newLegId = gameRepository.insertLeg(newLeg)
                    val newScores = state.players.associate { it.id to config.startScore }

                    // Rotate starting player
                    val nextStartIndex = newLegNumber % state.players.size

                    _uiState.update { it.copy(
                        legId = newLegId,
                        legWins = newLegWins,
                        scores = newScores,
                        currentLegNumber = newLegNumber,
                        currentPlayerIndex = nextStartIndex,
                        currentDarts = emptyList(),
                        pendingMultiplier = 1,
                        visitHistory = emptyList()
                    )}
                    updateCheckoutHint()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveGame() {
        viewModelScope.launch {
            val state = _uiState.value
            val gameId = state.gameId ?: return@launch
            val winnerId = state.winnerId ?: return@launch
            gameRepository.finishGame(gameId, winnerId)
            _uiState.update { it.copy(gameSaved = true) }
        }
    }

    fun discardGame() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.gameSaved) {
                val gameId = state.gameId ?: return@launch
                gameRepository.deleteGame(gameId)
            }
            resetToSetup()
        }
    }

    fun abortGame() {
        viewModelScope.launch {
            val gameId = _uiState.value.gameId
            if (gameId != null) gameRepository.deleteGame(gameId)
            resetToSetup()
        }
    }

    fun resetToSetup() {
        _uiState.update { it.copy(
            screen = GameScreen.SETUP,
            config = null,
            players = emptyList(),
            currentPlayerIndex = 0,
            scores = emptyMap(),
            legWins = emptyMap(),
            currentDarts = emptyList(),
            pendingMultiplier = 1,
            visitHistory = emptyList(),
            currentLegNumber = 1,
            gameId = null,
            legId = null,
            winnerId = null,
            checkoutHint = null,
            gameSaved = false
        )}
        loadSetupDefaults()
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
