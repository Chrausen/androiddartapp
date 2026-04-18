package com.clubdarts.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.*
import com.clubdarts.data.model.FunRule
import com.clubdarts.data.model.FunRules
import com.clubdarts.data.model.ScoreModifier
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GameScreen { SETUP, LIVE, RESULT }
enum class GameMode { SINGLE, TEAMS }

data class DartInput(
    val score: Int,
    val multiplier: Int,
    // Board mm coordinates from centre (null when thrown via numpad)
    val boardX: Float? = null,
    val boardY: Float? = null
) {
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
    val gameMode: GameMode = GameMode.SINGLE,
    val funModeEnabled: Boolean = false,
)

/**
 * Immutable snapshot of everything the game screens need to render.
 *
 * Fields are grouped logically:
 * - **Screen routing**: [screen]
 * - **Single-player mode**: [scores], [legWins] — keyed by `playerId`
 * - **Team mode**: [isTeamGame], [teamAssignments], [teamScores], [teamLegWins] — keyed by team index (0=A, 1=B)
 * - **Current visit**: [currentDarts], [pendingMultiplier], [checkoutHint]
 * - **Setup persistence**: `setup*` fields remember the UI selection across tab switches so the
 *   player/team picker doesn't reset when the user navigates away and back.
 * - **Ranked / Elo**: `ranked*` fields hold the *locked* config loaded from ranking settings;
 *   [eloResults] and [eloMatchId] are populated after a ranked game ends and are cleared by undo.
 */
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
    // Team turn tracking (team games only)
    val currentTeamIndex: Int = -1,             // -1 = non-team game, 0/1 for team games
    val teamPlayerIndexes: Map<Int, Int> = emptyMap(), // teamIndex → current player pos within team
    // Current visit
    val currentDarts: List<DartInput> = emptyList(),
    val boardVisitKey: Int = 0,          // increments on every resolveVisit(); drives board clear
    val playerVisitTotals: Map<Long, Int> = emptyMap(), // playerId → cumulative scored (busts = +0)
    val playerVisitCounts: Map<Long, Int> = emptyMap(), // playerId → visit count (busts included)
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
    // Setup persistence: remembered across tab switches so the picker doesn't reset on navigation
    val setupSelectedPlayerIds: List<Long> = emptyList(),
    val setupGameMode: GameMode = GameMode.SINGLE,
    val setupTeamAPlayerIds: List<Long> = emptyList(),
    val setupTeamBPlayerIds: List<Long> = emptyList(),
    // Ranking
    val rankingEnabled: Boolean = false,  // mirrors the global ranking-enabled setting
    val isRanked: Boolean = true,         // whether the user toggled ranked mode for THIS game
    // Locked config for ranked matches (loaded once from ranking settings; cannot be changed mid-game)
    val rankedStartScore: Int = 501,
    val rankedCheckoutRule: CheckoutRule = CheckoutRule.DOUBLE,
    val rankedLegsToWin: Int = 1,
    // playerId → signed Elo change (positive = gain, negative = loss); null until game ends
    val eloResults: Map<Long, Double>? = null,
    // ID of the EloMatch record created when a ranked game ended (used for undo via EloRepository.revertMatch)
    val eloMatchId: Long? = null,
    // Fun mode
    val activeFunRule: FunRule? = null,
    val pendingFunRuleAnnouncement: FunRule? = null,  // non-null → show overlay
    val shuffledFunRules: List<FunRule> = emptyList(),
    val funRuleIndex: Int = 0,
    val funRuleIntervalRounds: Int = 1,
    val funVisitsSinceRuleChange: Int = 0,
)

/**
 * Single ViewModel for the entire game flow (SETUP → LIVE → RESULT).
 *
 * All three screens — GameSetupScreen, LiveGameScreen, and GameResultScreen — share this
 * ViewModel so that game state survives navigation between them without being serialised to
 * SavedState or re-fetched from the database.
 *
 * ## State machine
 * ```
 *   SETUP ──startGame()──► LIVE ──onLegWon() (enough legs)──► RESULT
 *     ▲                      │                                    │
 *     └──abortGame()─────────┘   undoLastThrowOnResult() ───────►LIVE
 *     └──discardGame() / repeatGame() ◄──────────────────────────┘
 * ```
 *
 * ## Key design notes
 * - Scoring / bust logic lives in ScoringEngine and CheckoutCalculator (pure Kotlin, unit-tested).
 * - Both single-player mode (per-player [GameUiState.scores] map) and team mode
 *   ([GameUiState.teamScores] map) are tracked in the same state; [GameUiState.isTeamGame] selects
 *   which is active at runtime.
 * - Ranked games auto-save and record Elo atomically inside [onLegWon] when the match is decided.
 */
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
    private var randomCommentaryEnabled = false
    private var commentaryPhrases: CommentaryPhrases? = null

    // In-memory visit counter per player (reset each leg). Avoids a DB query on every throw.
    private val visitCounters = HashMap<Long, Int>()

    // ── Initialisation & lifecycle ────────────────────────────────────────────

    init {
        ttsManager.init()
        loadSetupDefaults()
        viewModelScope.launch {
            // Clean up any stale active game left in the DB from a previous session
            // (e.g. the app was killed while a game was in progress). The ViewModel starts
            // fresh at SETUP, so there is no way to resume — delete the orphaned record.
            val staleGame = gameRepository.getActiveGame()
            if (staleGame != null) {
                gameRepository.deleteGame(staleGame.id)
            }
        }
        viewModelScope.launch {
            settingsRepository.observeTtsScoreSettings().collect { settings ->
                ttsScoreSettings = settings
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRandomCommentaryEnabled().collect {
                randomCommentaryEnabled = it
            }
        }
        viewModelScope.launch {
            settingsRepository.observeCommentaryPhrases().collect {
                commentaryPhrases = it
            }
        }
        viewModelScope.launch {
            combine(
                settingsRepository.observeRankingEnabled(),
                settingsRepository.observeRankingStartScore(),
                settingsRepository.observeRankingCheckoutRule(),
                settingsRepository.observeRankingLegsToWin()
            ) { enabled, startScore, checkoutRule, legsToWin ->
                _uiState.update { s ->
                    s.copy(
                        rankingEnabled = enabled,
                        isRanked = if (!enabled) false else s.isRanked,
                        rankedStartScore = startScore,
                        rankedCheckoutRule = checkoutRule,
                        rankedLegsToWin = legsToWin
                    )
                }
            }.collect {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }

    // ── Audio helpers (available on all screens) ─────────────────────────────

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

    // ── Setup screen ──────────────────────────────────────────────────────────

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

    fun dismissFunRuleAnnouncement() {
        _uiState.update { it.copy(pendingFunRuleAnnouncement = null) }
    }

    fun showFunRuleInfo() {
        _uiState.update { it.copy(pendingFunRuleAnnouncement = it.activeFunRule) }
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
                val soundEffectsVolume = settingsRepository.getSoundEffectsVolume()
                soundEffectsService.setVolume(soundEffectsVolume)
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
                        gameMode = gameMode,
                        funModeEnabled = false,
                    )
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // ── Fun mode: scoring modifier helpers ───────────────────────────────────

    private fun DartInput.effectiveValue(modifier: ScoreModifier): Int =
        modifier.apply(score, multiplier)

    private fun DartInput.effectiveMultiplier(modifier: ScoreModifier): Int =
        modifier.applyMultiplier(multiplier)

    private fun activeModifier(): ScoreModifier =
        _uiState.value.activeFunRule?.scoreModifier ?: ScoreModifier.NONE

    // ── Live game: dart entry ─────────────────────────────────────────────────

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
        _uiState.update { it.copy(currentDarts = it.currentDarts + dart, pendingMultiplier = 1) }
        if (!_uiState.value.isSoundEffectsMuted) soundEffectsService.playRandomThrow()
        evaluateAfterDart()
    }

    fun recordMiss() {
        val dart = DartInput(score = 0, multiplier = 0)
        _uiState.update { it.copy(currentDarts = it.currentDarts + dart, pendingMultiplier = 1) }
        if (!_uiState.value.isSoundEffectsMuted) soundEffectsService.playRandomThrow()
        evaluateAfterDart()
    }

    /** Record a dart thrown via the touch board. Coordinates are in board mm from centre. */
    fun recordBoardDart(score: Int, multiplier: Int, boardX: Float, boardY: Float) {
        val dart = DartInput(score = score, multiplier = multiplier, boardX = boardX, boardY = boardY)
        _uiState.update { it.copy(currentDarts = it.currentDarts + dart, pendingMultiplier = 1) }
        if (!_uiState.value.isSoundEffectsMuted) soundEffectsService.playRandomThrow()
        evaluateAfterDart()
    }

    private fun evaluateAfterDart() {
        val state = _uiState.value
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        val remaining = state.remainingFor(currentPlayer.id)
        val modifier = activeModifier()
        val soFar = state.currentDarts.sumOf { it.effectiveValue(modifier) }
        val rule = state.config?.checkoutRule ?: CheckoutRule.DOUBLE
        when {
            ScoringEngine.isImmediateBust(remaining, soFar, rule) -> resolveVisit()
            remaining - soFar == 0 -> resolveVisit()  // checkout
            state.currentDarts.size >= 3 -> resolveVisit()  // all 3 darts thrown
            else -> updateCheckoutHint()
        }
    }

    private fun updateCheckoutHint() {
        val state = _uiState.value
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        val remaining = state.remainingFor(currentPlayer.id)
        val modifier = activeModifier()
        // Suppress checkout hints when a scoring modifier changes point values
        if (modifier != ScoreModifier.NONE) {
            _uiState.update { it.copy(checkoutHint = null) }
            return
        }
        val soFar = state.currentDarts.sumOf { it.value }
        val remainingAfterCurrentDarts = remaining - soFar
        val dartsLeft = 3 - state.currentDarts.size
        val rule = state.config?.checkoutRule ?: CheckoutRule.DOUBLE
        val maxCheckout = if (rule == CheckoutRule.DOUBLE) 170 else 180
        val hint = if (remainingAfterCurrentDarts > 0 && remainingAfterCurrentDarts <= maxCheckout) {
            CheckoutCalculator.suggest(remainingAfterCurrentDarts, rule, maxDarts = dartsLeft)
        } else null
        _uiState.update { it.copy(checkoutHint = hint) }
    }

    // ── Team turn-order helpers ───────────────────────────────────────────────

    /**
     * Returns the indices into [players] for each team, in the order players
     * were assigned to that team (i.e. their throw order within the team).
     * Key 0 = Team A, Key 1 = Team B.
     */
    private fun computeTeamPlayerIndices(
        players: List<Player>,
        teamAssignments: Map<Long, Int>
    ): Map<Int, List<Int>> {
        val result = mutableMapOf<Int, MutableList<Int>>()
        players.forEachIndexed { idx, p ->
            val team = teamAssignments[p.id] ?: return@forEachIndexed
            result.getOrPut(team) { mutableListOf() }.add(idx)
        }
        return result
    }

    // ── Live game: visit resolution ───────────────────────────────────────────

    private fun resolveVisit() {
        val state = _uiState.value
        val config = state.config ?: return
        val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
        val remaining = state.remainingFor(currentPlayer.id)
        val darts = state.currentDarts
        val modifier = state.activeFunRule?.scoreModifier ?: ScoreModifier.NONE
        val visitTotal = darts.sumOf { it.effectiveValue(modifier) }
        val isCheckoutAttempt = CheckoutCalculator.isCheckoutPossible(remaining, config.checkoutRule)

        // Determine bust via ScoringEngine using effective values
        val lastDart = darts.last()
        val visitResult = ScoringEngine.resolveVisit(
            remaining = remaining,
            visitTotal = visitTotal,
            lastDartScore = lastDart.score,
            lastDartMult = lastDart.effectiveMultiplier(modifier),
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

        // Increment in-memory visit counter (avoids a DB round-trip per throw)
        val visitNumber = (visitCounters[currentPlayer.id] ?: 0) + 1
        visitCounters[currentPlayer.id] = visitNumber

        // Persist throw to DB
        viewModelScope.launch {
            try {
                val legId = state.legId ?: return@launch
                val d1 = darts.getOrNull(0) ?: DartInput(0, 0)
                val d2 = darts.getOrNull(1) ?: DartInput(0, 0)
                val d3 = darts.getOrNull(2) ?: DartInput(0, 0)
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
                    isCheckoutAttempt = isCheckoutAttempt,
                    dart1X = d1.boardX?.toDouble(), dart1Y = d1.boardY?.toDouble(),
                    dart2X = d2.boardX?.toDouble(), dart2Y = d2.boardY?.toDouble(),
                    dart3X = d3.boardX?.toDouble(), dart3Y = d3.boardY?.toDouble()
                )
                gameRepository.insertThrow(throw_)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }

        // Update the appropriate score map
        var updatedState = if (state.isTeamGame) {
            val teamIdx = state.teamAssignments[currentPlayer.id] ?: 0
            state.copy(teamScores = state.teamScores + (teamIdx to newScore))
        } else {
            state.copy(scores = state.scores + (currentPlayer.id to newScore))
        }

        // Apply transfer mechanics (EVEN_STOLEN / MIRROR_THROW) to opponent scores
        if (!isBust) {
            updatedState = applyTransferModifier(updatedState, currentPlayer.id, darts, modifier, effectiveTotal)
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
                customPhrases = customPhrases,
                commentaryPhrases = if (randomCommentaryEnabled) commentaryPhrases else null
            )
        }

        if (!_uiState.value.isSoundEffectsMuted) {
            when {
                isCheckout -> soundEffectsService.playCheckout()
                isBust     -> soundEffectsService.playBust()
                else       -> Unit
            }
        }

        val newHistory = listOf(visitRecord) + state.visitHistory.take(19)

        val newVisitTotals = if (!isBust)
            updatedState.playerVisitTotals + (currentPlayer.id to
                ((updatedState.playerVisitTotals[currentPlayer.id] ?: 0) + effectiveTotal))
        else updatedState.playerVisitTotals

        val newVisitCounts = updatedState.playerVisitCounts +
            (currentPlayer.id to ((updatedState.playerVisitCounts[currentPlayer.id] ?: 0) + 1))

        val (nextPlayerIndex, nextTeamIndex, nextTeamPlayerIndexes) = if (state.isTeamGame) {
            val teamPlayerIndices = computeTeamPlayerIndices(state.players, state.teamAssignments)
            val prevTeam = state.currentTeamIndex
            val nextTeam = 1 - prevTeam
            val prevTeamSize = teamPlayerIndices[prevTeam]?.size ?: 1
            val advancedPrevIdx = ((state.teamPlayerIndexes[prevTeam] ?: 0) + 1) % prevTeamSize
            val nextIdxInTeam = state.teamPlayerIndexes[nextTeam] ?: 0
            val nextIdx = teamPlayerIndices[nextTeam]?.get(nextIdxInTeam) ?: 0
            Triple(nextIdx, nextTeam, state.teamPlayerIndexes + (prevTeam to advancedPrevIdx))
        } else {
            Triple((state.currentPlayerIndex + 1) % state.players.size, state.currentTeamIndex, state.teamPlayerIndexes)
        }

        // Fun mode: advance rule after N rounds (N × players.size visits)
        val newFunVisits = if (state.config?.funModeEnabled == true) state.funVisitsSinceRuleChange + 1 else 0
        val shouldAdvanceFunRule = state.config?.funModeEnabled == true &&
            state.shuffledFunRules.isNotEmpty() &&
            newFunVisits >= state.players.size * state.funRuleIntervalRounds.coerceAtLeast(1)
        val nextFunRuleIdx = if (shouldAdvanceFunRule)
            (state.funRuleIndex + 1) % state.shuffledFunRules.size.coerceAtLeast(1)
        else state.funRuleIndex
        val nextActiveFunRule = if (shouldAdvanceFunRule) state.shuffledFunRules.getOrNull(nextFunRuleIdx) else state.activeFunRule
        val nextFunAnnouncement = if (shouldAdvanceFunRule) nextActiveFunRule else null
        val nextFunVisits = if (shouldAdvanceFunRule) 0 else newFunVisits

        _uiState.update {
            updatedState.copy(
                currentDarts = emptyList(),
                pendingMultiplier = 1,
                boardVisitKey = updatedState.boardVisitKey + 1,
                playerVisitTotals = newVisitTotals,
                playerVisitCounts = newVisitCounts,
                currentPlayerIndex = nextPlayerIndex,
                currentTeamIndex = nextTeamIndex,
                teamPlayerIndexes = nextTeamPlayerIndexes,
                visitHistory = newHistory,
                activeFunRule = nextActiveFunRule,
                funRuleIndex = nextFunRuleIdx,
                pendingFunRuleAnnouncement = nextFunAnnouncement,
                funVisitsSinceRuleChange = nextFunVisits,
            )
        }
        updateCheckoutHint()

        if (isCheckout) {
            onLegWon(currentPlayer.id)
        }
    }

    // ── Live game: undo ───────────────────────────────────────────────────────

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

                    // Compute the previous player index and (for team games) team state
                    val prevPlayerIndex: Int
                    val prevTeamIndex: Int
                    val prevTeamPlayerIndexes: Map<Int, Int>
                    if (state.isTeamGame) {
                        val teamPlayerIndices = computeTeamPlayerIndices(state.players, state.teamAssignments)
                        // The team that just played is the one before the current team
                        val justPlayedTeam = 1 - state.currentTeamIndex
                        val justPlayedTeamSize = teamPlayerIndices[justPlayedTeam]?.size ?: 1
                        // Their pointer was advanced after playing; rewind it
                        val rewindedIdx = ((state.teamPlayerIndexes[justPlayedTeam] ?: 0) - 1 + justPlayedTeamSize) % justPlayedTeamSize
                        prevPlayerIndex = teamPlayerIndices[justPlayedTeam]?.get(rewindedIdx) ?: 0
                        prevTeamIndex = justPlayedTeam
                        prevTeamPlayerIndexes = state.teamPlayerIndexes + (justPlayedTeam to rewindedIdx)
                    } else {
                        prevPlayerIndex = if (state.currentPlayerIndex == 0) {
                            state.players.size - 1
                        } else {
                            state.currentPlayerIndex - 1
                        }
                        prevTeamIndex = state.currentTeamIndex
                        prevTeamPlayerIndexes = state.teamPlayerIndexes
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

                    // Keep visitCounter in sync with the deleted throw
                    visitCounters[prevPlayer.id] = ((visitCounters[prevPlayer.id] ?: 1) - 1).coerceAtLeast(0)

                    val newHistory = state.visitHistory.drop(1)

                    if (state.isTeamGame) {
                        val teamIdx = state.teamAssignments[prevPlayer.id] ?: 0
                        _uiState.update { it.copy(
                            teamScores = it.teamScores + (teamIdx to prevScore),
                            currentPlayerIndex = prevPlayerIndex,
                            currentTeamIndex = prevTeamIndex,
                            teamPlayerIndexes = prevTeamPlayerIndexes,
                            currentDarts = restoredDarts,
                            pendingMultiplier = 1,
                            visitHistory = newHistory
                        )}
                    } else {
                        _uiState.update { it.copy(
                            scores = it.scores + (prevPlayer.id to prevScore),
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

                // The checkout player advanced currentPlayerIndex (and team pointers) in resolveVisit().
                // Reverse that to recover the index of the player who checked out.
                val checkerOutIndex: Int
                val checkerOutTeamIndex: Int
                val checkerOutTeamPlayerIndexes: Map<Int, Int>
                if (state.isTeamGame) {
                    val teamPlayerIndices = computeTeamPlayerIndices(state.players, state.teamAssignments)
                    val justPlayedTeam = 1 - state.currentTeamIndex
                    val justPlayedTeamSize = teamPlayerIndices[justPlayedTeam]?.size ?: 1
                    val rewindedIdx = ((state.teamPlayerIndexes[justPlayedTeam] ?: 0) - 1 + justPlayedTeamSize) % justPlayedTeamSize
                    checkerOutIndex = teamPlayerIndices[justPlayedTeam]?.get(rewindedIdx) ?: 0
                    checkerOutTeamIndex = justPlayedTeam
                    checkerOutTeamPlayerIndexes = state.teamPlayerIndexes + (justPlayedTeam to rewindedIdx)
                } else {
                    checkerOutIndex = (state.currentPlayerIndex - 1 + state.players.size) % state.players.size
                    checkerOutTeamIndex = state.currentTeamIndex
                    checkerOutTeamPlayerIndexes = state.teamPlayerIndexes
                }

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

                // Keep visitCounter in sync with the deleted throw
                val checkerOutPlayer = state.players.getOrNull(checkerOutIndex)
                if (checkerOutPlayer != null) {
                    visitCounters[checkerOutPlayer.id] = ((visitCounters[checkerOutPlayer.id] ?: 1) - 1).coerceAtLeast(0)
                }

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
                    val adjustedLegWins = ((state.teamLegWins[teamIdx] ?: 1) - 1).coerceAtLeast(0)
                    _uiState.update { it.copy(
                        screen = GameScreen.LIVE,
                        teamLegWins = it.teamLegWins + (teamIdx to adjustedLegWins),
                        teamScores = it.teamScores + (teamIdx to restoredScore),
                        winningTeamIndex = null,
                        currentPlayerIndex = checkerOutIndex,
                        currentTeamIndex = checkerOutTeamIndex,
                        teamPlayerIndexes = checkerOutTeamPlayerIndexes,
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
                    val newLegWins = if (winnerId != null) {
                        val adjusted = ((state.legWins[winnerId] ?: 1) - 1).coerceAtLeast(0)
                        state.legWins + (winnerId to adjusted)
                    } else state.legWins
                    val newScores = if (winnerId != null) state.scores + (winnerId to restoredScore) else state.scores
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

    // ── Game start & leg/game completion ─────────────────────────────────────

    fun startGame(config: GameConfig) {
        viewModelScope.launch {
            try {
                settingsRepository.setLastGameConfig(
                    score = config.startScore,
                    rule = config.checkoutRule,
                    legs = config.legsToWin,
                    random = config.randomOrder
                )
                settingsRepository.setLastGameMode(if (config.isTeamGame) GameMode.TEAMS.name else GameMode.SINGLE.name)
                config.playerIds.forEach { settingsRepository.addRecentPlayer(it) }

                val gameId = gameRepository.startGame(config)
                val players = playerRepository.getPlayersByIds(config.playerIds)
                    .sortedBy { p -> config.playerIds.indexOf(p.id) }
                val leg = gameRepository.getActiveLeg(gameId)

                visitCounters.clear()

                // Prepare fun mode rule list
                val funIntervalRounds = settingsRepository.getFunModeIntervalRounds()
                val disabledRuleIds = settingsRepository.getFunModeDisabledRules().toSet()
                val shuffled = if (config.funModeEnabled) {
                    FunRules.all
                        .filter { !it.teamsOnly || config.isTeamGame }
                        .filter { it.id !in disabledRuleIds }
                        .shuffled()
                } else emptyList()
                val firstRule = shuffled.firstOrNull()

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
                        currentTeamIndex = 0,
                        teamPlayerIndexes = mapOf(0 to 0, 1 to 0),
                        currentPlayerIndex = 0,
                        currentDarts = emptyList(),
                        boardVisitKey = 0,
                        playerVisitTotals = emptyMap(),
                        playerVisitCounts = emptyMap(),
                        pendingMultiplier = 1,
                        visitHistory = emptyList(),
                        currentLegNumber = 1,
                        winnerId = null,
                        checkoutHint = null,
                        eloResults = null,
                        shuffledFunRules = shuffled,
                        activeFunRule = firstRule,
                        funRuleIndex = 0,
                        funRuleIntervalRounds = funIntervalRounds,
                        funVisitsSinceRuleChange = 0,
                        pendingFunRuleAnnouncement = firstRule,
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
                        boardVisitKey = 0,
                        playerVisitTotals = emptyMap(),
                        playerVisitCounts = emptyMap(),
                        pendingMultiplier = 1,
                        visitHistory = emptyList(),
                        currentLegNumber = 1,
                        winnerId = null,
                        checkoutHint = null,
                        eloResults = null,
                        shuffledFunRules = shuffled,
                        activeFunRule = firstRule,
                        funRuleIndex = 0,
                        funRuleIntervalRounds = funIntervalRounds,
                        funVisitsSinceRuleChange = 0,
                        pendingFunRuleAnnouncement = firstRule,
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
                    val newTeamLegWins = state.teamLegWins + (teamIdx to (state.teamLegWins[teamIdx] ?: 0) + 1)

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
                        visitCounters.clear()
                        _uiState.update { it.copy(
                            legId = newLegId,
                            teamLegWins = newTeamLegWins,
                            teamScores = newTeamScores,
                            currentLegNumber = newLegNumber,
                            currentTeamIndex = 0,
                            teamPlayerIndexes = mapOf(0 to 0, 1 to 0),
                            currentPlayerIndex = 0,
                            currentDarts = emptyList(),
                            pendingMultiplier = 1,
                            visitHistory = emptyList(),
                            funVisitsSinceRuleChange = 0,
                            pendingFunRuleAnnouncement = if (config.funModeEnabled) it.activeFunRule else null,
                        )}
                        updateCheckoutHint()
                    }
                } else {
                    val newLegWins = state.legWins + (winnerId to (state.legWins[winnerId] ?: 0) + 1)

                    val legsWon = newLegWins[winnerId] ?: 0
                    if (legsWon >= config.legsToWin) {
                        // Record Elo for ranked games (2+ players)
                        val eloMatchResult = if (state.isRanked && state.players.size >= 2) {
                            try {
                                eloRepository.recordMatch(state.players, winnerId, gameId = gameId)
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

                        visitCounters.clear()
                        _uiState.update { it.copy(
                            legId = newLegId,
                            legWins = newLegWins,
                            scores = newScores,
                            currentLegNumber = newLegNumber,
                            currentPlayerIndex = nextStartIndex,
                            currentDarts = emptyList(),
                            pendingMultiplier = 1,
                            visitHistory = emptyList(),
                            funVisitsSinceRuleChange = 0,
                            pendingFunRuleAnnouncement = if (config.funModeEnabled) it.activeFunRule else null,
                        )}
                        updateCheckoutHint()
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    // ── Result screen ─────────────────────────────────────────────────────────

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
        visitCounters.clear()
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
            isRanked = true,
            eloResults = null,
            eloMatchId = null,
            activeFunRule = null,
            pendingFunRuleAnnouncement = null,
            shuffledFunRules = emptyList(),
            funRuleIndex = 0,
            funRuleIntervalRounds = 1,
            funVisitsSinceRuleChange = 0,
        )}
        loadSetupDefaults()
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Applies transfer scoring mechanics (EVEN_STOLEN / MIRROR_THROW) to opponent scores.
    private fun applyTransferModifier(
        state: GameUiState,
        currentPlayerId: Long,
        darts: List<DartInput>,
        modifier: ScoreModifier,
        effectiveTotal: Int,
    ): GameUiState {
        return when (modifier) {
            ScoreModifier.EVEN_STOLEN -> {
                val stolenAmount = darts.sumOf { d ->
                    if (d.score > 0 && d.score % 2 == 0) d.value / 2 else 0
                }
                if (stolenAmount == 0) return state
                if (state.isTeamGame) {
                    val currentTeam = state.teamAssignments[currentPlayerId] ?: 0
                    val opponentTeam = 1 - currentTeam
                    val opponentScore = (state.teamScores[opponentTeam] ?: 0) + stolenAmount
                    state.copy(teamScores = state.teamScores + (opponentTeam to opponentScore))
                } else {
                    val updatedScores = state.scores.mapValues { (pid, score) ->
                        if (pid != currentPlayerId) score + stolenAmount else score
                    }
                    state.copy(scores = updatedScores)
                }
            }
            ScoreModifier.MIRROR_THROW -> {
                if (effectiveTotal == 0) return state
                if (state.isTeamGame) {
                    val currentTeam = state.teamAssignments[currentPlayerId] ?: 0
                    val opponentTeam = 1 - currentTeam
                    val opponentScore = ((state.teamScores[opponentTeam] ?: 0) - effectiveTotal)
                        .coerceAtLeast(1)
                    state.copy(teamScores = state.teamScores + (opponentTeam to opponentScore))
                } else {
                    val updatedScores = state.scores.mapValues { (pid, score) ->
                        if (pid != currentPlayerId) (score - effectiveTotal).coerceAtLeast(1) else score
                    }
                    state.copy(scores = updatedScores)
                }
            }
            else -> state
        }
    }

    // Returns the remaining score for a player, routing through the team score map in team mode.
    private fun GameUiState.remainingFor(playerId: Long): Int {
        return if (isTeamGame) {
            val teamIdx = teamAssignments[playerId] ?: 0
            teamScores[teamIdx] ?: 0
        } else {
            scores[playerId] ?: 0
        }
    }
}
