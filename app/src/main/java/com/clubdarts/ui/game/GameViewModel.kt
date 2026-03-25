package com.clubdarts.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.*
import com.clubdarts.data.repository.EloRepository
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.util.CheckoutCalculator
import com.clubdarts.util.ScoringEngine
import com.clubdarts.util.SoundEffectsService
import com.clubdarts.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GameScreen { SETUP, LIVE, RESULT }
enum class GameMode { SINGLE, TEAMS }

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
    val recentPlayerIds: List<Long> = emptyList(),
    val gameMode: GameMode = GameMode.SINGLE
)

data class GameUiState(
    val screen: GameScreen = GameScreen.SETUP,
    val config: GameConfig? = null,
    val players: List<Player> = emptyList(),
    val currentPlayerIndex: Int = 0,
    // Single mode scores
    val scores: Map<Long, Int> = emptyMap(),
    val legWins: Map<Long, Int> = emptyMap(),
    // Team mode scores
    val isTeamGame: Boolean = false,
    val teamAssignments: Map<Long, Int> = emptyMap(),   // playerId → 0 (Team A) or 1 (Team B)
    val teamScores: Map<Int, Int> = emptyMap(),          // teamIndex → remaining score
    val teamLegWins: Map<Int, Int> = emptyMap(),         // teamIndex → legs won
    val winningTeamIndex: Int? = null,
    // Current visit
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
    val isSoundEffectsMuted: Boolean = false,
    val showHistory: Boolean = false,
    // Setup persistence across tab switches
    val setupSelectedPlayerIds: List<Long> = emptyList(),
    val setupGameMode: GameMode = GameMode.SINGLE,
    val setupTeamAPlayerIds: List<Long> = emptyList(),
    val setupTeamBPlayerIds: List<Long> = emptyList(),
    // Ranking
    val rankingEnabled: Boolean = false,
    val isRanked: Boolean = false,
    // Locked config for ranked matches (loaded from ranking settings)
    val rankedStartScore: Int = 501,
    val rankedCheckoutRule: CheckoutRule = CheckoutRule.DOUBLE,
    val rankedLegsToWin: Int = 1,
    // playerId -> signed elo change (positive = gain, negative = loss)
    val eloResults: Map<Long, Double>? = null,
    // ID of the EloMatch record created when a ranked game ended (used for undo)
    val eloMatchId: Long? = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository,
    private val playerRepository: PlayerRepository,
    private val eloRepository: EloRepository,
    private val soundEffectsService: SoundEffectsService
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
        viewModelScope.launch {
            settingsRepository.observeRankingEnabled().collect { enabled ->
                _uiState.update { it.copy(rankingEnabled = enabled) }
                // If ranking gets disabled, reset ranked mode
                if (!enabled) {
                    _uiState.update { it.copy(isRanked = false) }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRankingStartScore().collect { v ->
                _uiState.update { it.copy(rankedStartScore = v) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRankingCheckoutRule().collect { v ->
                _uiState.update { it.copy(rankedCheckoutRule = v) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRankingLegsToWin().collect { v ->
                _uiState.update { it.copy(rankedLegsToWin = v) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    fun toggleTtsMute() { _uiState.update { it.copy(isTtsMuted = !it.isTtsMuted) } }

    fun toggleSoundEffectsMute() {
        val newValue = !_uiState.value.isSoundEffectsMuted
        _uiState.update { it.copy(isSoundEffectsMuted = newValue) }
        soundEffectsService.setMuted(newValue)
        viewModelScope.launch { settingsRepository.setSoundEffectsMuted(newValue) }
    }

    /** Play a UI click sound — silenced when the live game is active. */
    fun playUiClick() {
        if (_uiState.value.screen != GameScreen.LIVE) {
            soundEffectsService.playClick()
        }
    }

    fun toggleHistory() {
        val newValue = !_uiState.value.showHistory
        _uiState.update { it.copy(showHistory = newValue) }
        viewModelScope.launch { settingsRepository.setShowHistory(newValue) }
    }

    fun setRanked(v: Boolean) {
        _uiState.update { it.copy(isRanked = v) }
    }

    fun updateSetupSelectedPlayers(ids: List<Long>) {
        _uiState.update { it.copy(setupSelectedPlayerIds = ids) }
    }

    fun updateSetupTeamPlayers(teamA: List<Long>, teamB: List<Long>) {
        _uiState.update { it.copy(setupTeamAPlayerIds = teamA, setupTeamBPlayerIds = teamB) }
    }

    fun updateSetupGameMode(mode: GameMode) {
        _uiState.update { it.copy(setupGameMode = mode) }
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
                val soundEffectsMuted = settingsRepository.getSoundEffectsMuted()
                val gameMode = try { GameMode.valueOf(settingsRepository.getLastGameMode()) } catch (e: Exception) { GameMode.SINGLE }
                val rankedStartScore = settingsRepository.getRankingStartScore()
                val rankedCheckoutRule = settingsRepository.getRankingCheckoutRule()
                val rankedLegsToWin = settingsRepository.getRankingLegsToWin()
                soundEffectsService.setMuted(soundEffectsMuted)
                _uiState.update { it.copy(
                    showHistory = showHistory,
                    isSoundEffectsMuted = soundEffectsMuted,
                    setupGameMode = gameMode,
                    rankedStartScore = rankedStartScore,
                    rankedCheckoutRule = rankedCheckoutRule,
                    rankedLegsToWin = rankedLegsToWin,
                    setupDefaults = SetupDefaults(
                        startScore = startScore,
                        checkoutRule = checkoutRule,
                        legsToWin = legsToWin,
                        randomOrder = randomOrder,
                        recentPlayerIds = recentIds,
                        gameMode = gameMode
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

        if (!_uiState.value.isSoundEffectsMuted) soundEffectsService.playRandomThrow()

        val currentState = _uiState.value
        val currentPlayer = currentState.players.getOrNull(currentState.currentPlayerIndex) ?: return
        val remaining = currentState.remainingFor(currentPlayer.id)
        val soFar = currentState.currentDarts.sumOf { it.value }
        val rule = currentState.config?.checkoutRule ?: CheckoutRule.DOUBLE

        when {
            ScoringEngine.isImmediateBust(remaining, soFar, rule) -> resolveVisit()
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
        if (!_uiState.value.isSoundEffectsMuted) soundEffectsService.playRandomThrow()
        if (_uiState.value.currentDarts.size >= 3) {
            resolveVisit()
        } else {
            updateCheckoutHint()
        }
    }

    private fun updateCheckoutHint() {
        val state = _uiState.value
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        val remaining = state.remainingFor(currentPlayer.id)
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
        val remaining = state.remainingFor(currentPlayer.id)
        val darts = state.currentDarts
        val visitTotal = darts.sumOf { it.value }
        val isCheckoutAttempt = CheckoutCalculator.isCheckoutPossible(remaining, config.checkoutRule)

        // Determine bust via ScoringEngine
        val lastDart = darts.last()
        val visitResult = ScoringEngine.resolveVisit(
            remaining = remaining,
            visitTotal = visitTotal,
            lastDartScore = lastDart.score,
            lastDartMult = lastDart.multiplier,
            rule = config.checkoutRule
        )
        val isBust = visitResult.isBust
        val effectiveTotal = visitResult.effectiveScore

        val newScore = if (isBust) remaining else remaining - effectiveTotal

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

        // Update the appropriate score map
        val updatedState = if (state.isTeamGame) {
            val teamIdx = state.teamAssignments[currentPlayer.id] ?: 0
            val newTeamScores = state.teamScores.toMutableMap().also { it[teamIdx] = newScore }
            state.copy(teamScores = newTeamScores)
        } else {
            val newScores = state.scores.toMutableMap().also { it[currentPlayer.id] = newScore }
            state.copy(scores = newScores)
        }

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

        if (!_uiState.value.isSoundEffectsMuted) {
            when {
                isCheckout -> soundEffectsService.playCheckout()
                isBust     -> soundEffectsService.playBust()
                else       -> Unit
            }
        }

        val nextPlayerIndex = (state.currentPlayerIndex + 1) % state.players.size
        val newHistory = (listOf(visitRecord) + state.visitHistory).take(20)

        _uiState.update {
            updatedState.copy(
                currentDarts = emptyList(),
                pendingMultiplier = 1,
                currentPlayerIndex = nextPlayerIndex,
                visitHistory = newHistory
            )
        }
        updateCheckoutHint()

        if (isCheckout) {
            onLegWon(currentPlayer.id)
        }
    }

    fun undoLastDart() {
        val state = _uiState.value
        if (state.currentDarts.isNotEmpty()) {
            val newDarts = state.currentDarts.dropLast(1)
            _uiState.update { it.copy(currentDarts = newDarts, pendingMultiplier = 1) }
            updateCheckoutHint()
        } else {
            viewModelScope.launch {
                try {
                    val legId = state.legId ?: return@launch
                    val lastThrow = gameRepository.getLastThrowInLeg(legId) ?: return@launch

                    val prevPlayerIndex = if (state.currentPlayerIndex == 0) {
                        state.players.size - 1
                    } else {
                        state.currentPlayerIndex - 1
                    }
                    val prevPlayer = state.players.getOrNull(prevPlayerIndex) ?: return@launch

                    val prevScore = state.remainingFor(prevPlayer.id) + lastThrow.visitTotal

                    val allDarts = buildList {
                        if (lastThrow.dartsUsed >= 1) add(DartInput(lastThrow.dart1Score, lastThrow.dart1Mult))
                        if (lastThrow.dartsUsed >= 2) add(DartInput(lastThrow.dart2Score, lastThrow.dart2Mult))
                        if (lastThrow.dartsUsed >= 3) add(DartInput(lastThrow.dart3Score, lastThrow.dart3Mult))
                    }
                    val restoredDarts = allDarts.dropLast(1)

                    gameRepository.deleteThrow(lastThrow)

                    val newHistory = state.visitHistory.drop(1)

                    if (state.isTeamGame) {
                        val teamIdx = state.teamAssignments[prevPlayer.id] ?: 0
                        val newTeamScores = state.teamScores.toMutableMap().also { it[teamIdx] = prevScore }
                        _uiState.update { it.copy(
                            teamScores = newTeamScores,
                            currentPlayerIndex = prevPlayerIndex,
                            currentDarts = restoredDarts,
                            pendingMultiplier = 1,
                            visitHistory = newHistory
                        )}
                    } else {
                        _uiState.update { it.copy(
                            scores = it.scores.toMutableMap().also { m -> m[prevPlayer.id] = prevScore },
                            currentPlayerIndex = prevPlayerIndex,
                            currentDarts = restoredDarts,
                            pendingMultiplier = 1,
                            visitHistory = newHistory
                        )}
                    }
                    updateCheckoutHint()
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
            }
        }
    }

    /**
     * Undo the checkout throw that ended the game.
     * Reverts DB state (throw, leg finish, game finish, ELO if ranked) and
     * returns to the LIVE screen with the player's score restored.
     */
    fun undoLastThrowOnResult() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val legId = state.legId ?: return@launch
                val gameId = state.gameId ?: return@launch

                val lastThrow = gameRepository.getLastThrowInLeg(legId) ?: return@launch

                // The checkout player advanced currentPlayerIndex by 1 in resolveVisit().
                // Reverse that to recover the index of the player who checked out.
                val checkerOutIndex = (state.currentPlayerIndex - 1 + state.players.size) % state.players.size

                // Reconstruct the darts of the finishing visit, then drop the last dart —
                // matching the behaviour of undoLastDart on the live screen.
                val allDarts = buildList {
                    if (lastThrow.dartsUsed >= 1) add(DartInput(lastThrow.dart1Score, lastThrow.dart1Mult))
                    if (lastThrow.dartsUsed >= 2) add(DartInput(lastThrow.dart2Score, lastThrow.dart2Mult))
                    if (lastThrow.dartsUsed >= 3) add(DartInput(lastThrow.dart3Score, lastThrow.dart3Mult))
                }
                // Darts to restore as "already entered" (all but the accidental last dart)
                val restoredDarts = allDarts.dropLast(1)
                // Score displayed = remaining before the entire visit (same as undoLastDart)
                val restoredScore = lastThrow.visitTotal

                // Revert DB: delete throw, unfinish leg, unfinish game
                gameRepository.deleteThrow(lastThrow)
                gameRepository.unfinishLeg(legId)
                gameRepository.unfinishGame(gameId)

                // Revert ELO if this was a ranked game that was auto-saved
                val eloMatchId = state.eloMatchId
                if (state.isRanked && eloMatchId != null) {
                    eloRepository.revertMatch(eloMatchId)
                }

                // Drop the checkout visit from history
                val newHistory = state.visitHistory.drop(1)

                // Restore in-memory game state and go back to LIVE
                if (state.isTeamGame) {
                    val teamIdx = state.winningTeamIndex ?: 0
                    val newTeamLegWins = state.teamLegWins.toMutableMap()
                    newTeamLegWins[teamIdx] = ((newTeamLegWins[teamIdx] ?: 1) - 1).coerceAtLeast(0)
                    val newTeamScores = state.teamScores.toMutableMap().also { it[teamIdx] = restoredScore }
                    _uiState.update { it.copy(
                        screen = GameScreen.LIVE,
                        teamLegWins = newTeamLegWins,
                        teamScores = newTeamScores,
                        winningTeamIndex = null,
                        currentPlayerIndex = checkerOutIndex,
                        currentDarts = restoredDarts,
                        pendingMultiplier = 1,
                        visitHistory = newHistory,
                        winnerId = null,
                        eloResults = null,
                        eloMatchId = null,
                        gameSaved = false
                    )}
                } else {
                    val winnerId = state.winnerId
                    val newLegWins = state.legWins.toMutableMap()
                    if (winnerId != null) {
                        newLegWins[winnerId] = ((newLegWins[winnerId] ?: 1) - 1).coerceAtLeast(0)
                    }
                    val newScores = state.scores.toMutableMap()
                    if (winnerId != null) newScores[winnerId] = restoredScore
                    _uiState.update { it.copy(
                        screen = GameScreen.LIVE,
                        legWins = newLegWins,
                        scores = newScores,
                        winnerId = null,
                        currentPlayerIndex = checkerOutIndex,
                        currentDarts = restoredDarts,
                        pendingMultiplier = 1,
                        visitHistory = newHistory,
                        eloResults = null,
                        eloMatchId = null,
                        gameSaved = false
                    )}
                }
                updateCheckoutHint()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
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
                settingsRepository.setLastGameMode(if (config.isTeamGame) GameMode.TEAMS.name else GameMode.SINGLE.name)
                config.playerIds.forEach { settingsRepository.addRecentPlayer(it) }

                val gameId = gameRepository.startGame(config)
                val players = playerRepository.getPlayersByIds(config.playerIds)
                    .sortedBy { p -> config.playerIds.indexOf(p.id) }
                val leg = gameRepository.getActiveLeg(gameId)

                if (config.isTeamGame) {
                    val teamScores = mapOf(0 to config.startScore, 1 to config.startScore)
                    val teamLegWins = mapOf(0 to 0, 1 to 0)
                    _uiState.update { it.copy(
                        screen = GameScreen.LIVE,
                        config = config,
                        players = players,
                        gameId = gameId,
                        legId = leg?.id,
                        scores = emptyMap(),
                        legWins = emptyMap(),
                        isTeamGame = true,
                        teamAssignments = config.teamAssignments,
                        teamScores = teamScores,
                        teamLegWins = teamLegWins,
                        winningTeamIndex = null,
                        currentPlayerIndex = 0,
                        currentDarts = emptyList(),
                        pendingMultiplier = 1,
                        visitHistory = emptyList(),
                        currentLegNumber = 1,
                        winnerId = null,
                        checkoutHint = null,
                        eloResults = null
                    )}
                } else {
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
                        isTeamGame = false,
                        teamAssignments = emptyMap(),
                        teamScores = emptyMap(),
                        teamLegWins = emptyMap(),
                        winningTeamIndex = null,
                        currentPlayerIndex = 0,
                        currentDarts = emptyList(),
                        pendingMultiplier = 1,
                        visitHistory = emptyList(),
                        currentLegNumber = 1,
                        winnerId = null,
                        checkoutHint = null,
                        eloResults = null
                    )}
                }
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

                if (state.isTeamGame) {
                    val teamIdx = state.teamAssignments[winnerId] ?: 0
                    val newTeamLegWins = state.teamLegWins.toMutableMap()
                    newTeamLegWins[teamIdx] = (newTeamLegWins[teamIdx] ?: 0) + 1

                    val legsWon = newTeamLegWins[teamIdx] ?: 0
                    if (legsWon >= config.legsToWin) {
                        gameRepository.finishGame(gameId, null, winningTeamIndex = teamIdx)
                        _uiState.update { it.copy(
                            screen = GameScreen.RESULT,
                            teamLegWins = newTeamLegWins,
                            winningTeamIndex = teamIdx
                        )}
                    } else {
                        val newLegNumber = state.currentLegNumber + 1
                        val newLeg = Leg(gameId = gameId, legNumber = newLegNumber)
                        val newLegId = gameRepository.insertLeg(newLeg)
                        val newTeamScores = mapOf(0 to config.startScore, 1 to config.startScore)
                        _uiState.update { it.copy(
                            legId = newLegId,
                            teamLegWins = newTeamLegWins,
                            teamScores = newTeamScores,
                            currentLegNumber = newLegNumber,
                            currentPlayerIndex = 0,
                            currentDarts = emptyList(),
                            pendingMultiplier = 1,
                            visitHistory = emptyList()
                        )}
                        updateCheckoutHint()
                    }
                } else {
                    val newLegWins = state.legWins.toMutableMap()
                    newLegWins[winnerId] = (newLegWins[winnerId] ?: 0) + 1

                    val legsWon = newLegWins[winnerId] ?: 0
                    if (legsWon >= config.legsToWin) {
                        // Record Elo for ranked games (2+ players)
                        val eloMatchResult = if (state.isRanked && state.players.size >= 2) {
                            try {
                                eloRepository.recordMatch(state.players, winnerId)
                            } catch (e: Exception) {
                                _uiState.update { it.copy(errorMessage = e.message) }
                                null
                            }
                        } else null

                        // Auto-save ranked games immediately
                        val autoSaved = state.isRanked
                        if (autoSaved) {
                            gameRepository.finishGame(gameId, winnerId)
                        }

                        _uiState.update { it.copy(
                            screen = GameScreen.RESULT,
                            legWins = newLegWins,
                            winnerId = winnerId,
                            eloResults = eloMatchResult?.changes,
                            eloMatchId = eloMatchResult?.matchId,
                            gameSaved = autoSaved
                        )}
                    } else {
                        val newLegNumber = state.currentLegNumber + 1
                        val newLeg = Leg(gameId = gameId, legNumber = newLegNumber)
                        val newLegId = gameRepository.insertLeg(newLeg)
                        val newScores = state.players.associate { it.id to config.startScore }

                        // Rotate starting player for single mode
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
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun repeatGame() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.gameSaved) {
                val gameId = state.gameId
                if (gameId != null) gameRepository.deleteGame(gameId)
            }
            val config = state.config
            val sameGameMode = if (state.isTeamGame) GameMode.TEAMS else GameMode.SINGLE
            val sameTeamA = state.players.filter { state.teamAssignments[it.id] == 0 }.map { it.id }
            val sameTeamB = state.players.filter { state.teamAssignments[it.id] == 1 }.map { it.id }
            _uiState.update { current ->
                GameUiState(
                    rankingEnabled = current.rankingEnabled,
                    isRanked = current.isRanked,
                    rankedStartScore = current.rankedStartScore,
                    rankedCheckoutRule = current.rankedCheckoutRule,
                    rankedLegsToWin = current.rankedLegsToWin,
                    showHistory = current.showHistory,
                    isTtsMuted = current.isTtsMuted,
                    setupSelectedPlayerIds = state.players.map { it.id },
                    setupGameMode = sameGameMode,
                    setupTeamAPlayerIds = sameTeamA,
                    setupTeamBPlayerIds = sameTeamB,
                    setupDefaults = current.setupDefaults.copy(
                        startScore = config?.startScore ?: current.setupDefaults.startScore,
                        checkoutRule = config?.checkoutRule ?: current.setupDefaults.checkoutRule,
                        legsToWin = config?.legsToWin ?: current.setupDefaults.legsToWin,
                        gameMode = sameGameMode
                    )
                )
            }
        }
    }

    fun saveGame() {
        viewModelScope.launch {
            val state = _uiState.value
            val gameId = state.gameId ?: return@launch
            if (state.isTeamGame) {
                val teamIdx = state.winningTeamIndex ?: return@launch
                gameRepository.finishGame(gameId, null, winningTeamIndex = teamIdx)
            } else {
                val winnerId = state.winnerId ?: return@launch
                gameRepository.finishGame(gameId, winnerId)
            }
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
            isTeamGame = false,
            teamAssignments = emptyMap(),
            teamScores = emptyMap(),
            teamLegWins = emptyMap(),
            winningTeamIndex = null,
            currentDarts = emptyList(),
            pendingMultiplier = 1,
            visitHistory = emptyList(),
            currentLegNumber = 1,
            gameId = null,
            legId = null,
            winnerId = null,
            checkoutHint = null,
            gameSaved = false,
            isRanked = false,
            eloResults = null,
            eloMatchId = null
        )}
        loadSetupDefaults()
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Helper: get the remaining score for a player (team or individual)
    private fun GameUiState.remainingFor(playerId: Long): Int {
        return if (isTeamGame) {
            val teamIdx = teamAssignments[playerId] ?: 0
            teamScores[teamIdx] ?: 0
        } else {
            scores[playerId] ?: 0
        }
    }
}
