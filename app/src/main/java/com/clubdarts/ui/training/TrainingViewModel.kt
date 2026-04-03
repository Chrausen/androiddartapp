package com.clubdarts.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.Player
import com.clubdarts.data.model.TrainingDifficulty
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.data.model.TrainingSession
import com.clubdarts.data.model.TrainingThrow
import com.clubdarts.data.repository.PlayerRepository
import com.clubdarts.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// ── Domain helpers ────────────────────────────────────────────────────────────

/** One dart recorded during a training session (in-memory, before DB save). */
data class TrainingThrowRecord(
    val targetField: String,
    val actualField: String,
    val isHit: Boolean,
    val targetMmX: Double? = null,
    val targetMmY: Double? = null,
    val actualMmX: Double? = null,
    val actualMmY: Double? = null
)

/**
 * Builds exactly 10 targets for TARGET_FIELD mode.
 *
 * Easy:   10 random singles (1–20, no Bull).
 * Medium: 2 Doubles + 7 Singles (unique numbers, no repeats across types) + 1 Bull = 10.
 * Hard:   2 Triples + 2 Doubles + 5 Singles (unique numbers) + 1 Bullseye = 10.
 */
private fun buildTargetPool(difficulty: TrainingDifficulty): List<String> {
    val numbers = (1..20).shuffled()
    return when (difficulty) {
        TrainingDifficulty.BEGINNER -> numbers.take(10).map { "S$it" }.shuffled()
        TrainingDifficulty.INTERMEDIATE -> {
            val picks = numbers.take(9)
            (picks.take(2).map { "D$it" } + picks.drop(2).map { "S$it" } + listOf("Bull")).shuffled()
        }
        TrainingDifficulty.PRO -> {
            val picks = numbers.take(9)
            (picks.take(2).map { "T$it" } +
             picks.drop(2).take(2).map { "D$it" } +
             picks.drop(4).map { "S$it" } +
             listOf("Bullseye")).shuffled()
        }
    }
}

/**
 * Returns true when [actual] is an acceptable hit for [target] at [difficulty].
 *
 * Easy:   D and T of the target number also count (any scoring segment).
 * Medium: Singles require exact single. Bull accepts Bullseye too.
 * Hard:   Exact match only.
 */
private fun isTargetFieldHit(target: String, actual: String, difficulty: TrainingDifficulty): Boolean =
    when (difficulty) {
        TrainingDifficulty.BEGINNER -> {
            val n = target.removePrefix("S").toIntOrNull()
            if (n != null) actual == "S$n" || actual == "D$n" || actual == "T$n"
            else actual == target
        }
        TrainingDifficulty.INTERMEDIATE -> when (target) {
            "Bull" -> actual == "Bull" || actual == "Bullseye"
            else   -> actual == target
        }
        TrainingDifficulty.PRO -> actual == target
    }

// ── UI state ──────────────────────────────────────────────────────────────────

enum class TrainingScreenState { SETUP, LIVE, DONE }

/**
 * Per-mode live-session state sealed class. Held inside [TrainingUiState] while
 * [TrainingScreenState.LIVE] is active.
 */
sealed class LiveSessionState {

    abstract val throws: List<TrainingThrowRecord>
    abstract val totalDarts: Int
    abstract val isComplete: Boolean

    data class TargetField(
        val targets: List<String>,
        val difficulty: TrainingDifficulty,
        val currentIdx: Int = 0,
        override val throws: List<TrainingThrowRecord> = emptyList()
    ) : LiveSessionState() {
        val currentTarget: String get() = targets.getOrElse(currentIdx) { "" }
        override val totalDarts: Int  get() = throws.size
        override val isComplete: Boolean get() = currentIdx >= targets.size
    }

    /**
     * Around-the-Clock session state.
     *
     * Easy:   numbers 1–20; any segment (S/D/T) counts.
     * Medium: numbers 1–20 (double only), then Bull (outer bull; Bullseye also accepted).
     * Hard:   numbers 1–20 (triple only), then Bullseye (inner bull).
     */
    data class AroundTheClock(
        val difficulty: TrainingDifficulty,
        val currentNumber: Int = 1,
        override val throws: List<TrainingThrowRecord> = emptyList()
    ) : LiveSessionState() {
        /** 20 for Easy, 21 for Medium/Hard (extra Bull/Bullseye round). */
        val totalTargets: Int get() = if (difficulty == TrainingDifficulty.BEGINNER) 20 else 21

        override val totalDarts: Int get() = throws.size
        override val isComplete: Boolean get() = currentNumber > totalTargets

        /** The field string that must be hit to advance. */
        val currentTargetField: String get() = when (difficulty) {
            TrainingDifficulty.BEGINNER      -> "S$currentNumber"
            TrainingDifficulty.INTERMEDIATE  -> if (currentNumber > 20) "Bull"     else "D$currentNumber"
            TrainingDifficulty.PRO           -> if (currentNumber > 20) "Bullseye" else "T$currentNumber"
        }

        /** Multiplier hint for the board/numpad input (1=any, 2=double, 3=triple). */
        val requiredMultiplier: Int get() = when (difficulty) {
            TrainingDifficulty.BEGINNER     -> 1
            TrainingDifficulty.INTERMEDIATE -> if (currentNumber > 20) 1 else 2
            TrainingDifficulty.PRO          -> if (currentNumber > 20) 2 else 3
        }
    }

    data class ScoringRounds(
        val targetAvg: Int,
        val completedRounds: List<Int> = emptyList(),   // sum per round
        val currentRoundDarts: List<Int> = emptyList()  // individual dart scores
    ) : LiveSessionState() {
        override val throws: List<TrainingThrowRecord> get() = emptyList()
        override val totalDarts: Int get() = completedRounds.size * 3 + currentRoundDarts.size
        override val isComplete: Boolean get() = completedRounds.size >= 10
        val currentRound: Int get() = completedRounds.size + 1
        val runningAverage: Double get() = if (completedRounds.isEmpty()) 0.0 else completedRounds.average()
    }
}

data class TrainingUiState(
    val screen: TrainingScreenState = TrainingScreenState.SETUP,
    val players: List<Player> = emptyList(),
    val selectedPlayer: Player? = null,
    val mode: TrainingMode = TrainingMode.TARGET_FIELD,
    val difficulty: TrainingDifficulty = TrainingDifficulty.BEGINNER,
    val recentResults: List<TrainingSession> = emptyList(),
    val bestResult: com.clubdarts.data.db.dao.BestSessionWithPlayer? = null,
    val liveSession: LiveSessionState? = null,
    val lastResult: TrainingSession? = null,
    val isSaving: Boolean = false,
    val pendingMultiplier: Int = 1,
    val showBoardInput: Boolean = true
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var sessionStartedAt: Long = 0L

    init {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                _uiState.update { it.copy(players = players) }
            }
        }
        reloadRecentResults()
    }

    // ── Setup actions ─────────────────────────────────────────────────────────

    fun selectPlayer(player: Player?) {
        _uiState.update { it.copy(selectedPlayer = player) }
        reloadRecentResults()
    }

    fun selectMode(mode: TrainingMode) {
        _uiState.update { it.copy(mode = mode) }
        reloadRecentResults()
    }

    fun selectDifficulty(difficulty: TrainingDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        reloadRecentResults()
    }

    private fun reloadRecentResults() {
        val mode = _uiState.value.mode
        val difficulty = _uiState.value.difficulty
        viewModelScope.launch {
            val best = trainingRepository.getBestResult(mode, difficulty)
            val player = _uiState.value.selectedPlayer
            val results = if (player != null)
                trainingRepository.getRecentResults(player.id, mode, 10)
            else emptyList()
            _uiState.update { it.copy(recentResults = results, bestResult = best) }
        }
    }

    fun setMultiplier(mult: Int) {
        _uiState.update { it.copy(pendingMultiplier = if (it.pendingMultiplier == mult) 1 else mult) }
    }

    fun toggleInputMode() {
        _uiState.update { it.copy(showBoardInput = !it.showBoardInput, pendingMultiplier = 1) }
    }

    // ── Session start ─────────────────────────────────────────────────────────

    fun startSession() {
        val state = _uiState.value
        val player = state.selectedPlayer ?: return

        val liveState: LiveSessionState = when (state.mode) {
            TrainingMode.TARGET_FIELD -> {
                LiveSessionState.TargetField(
                    targets    = buildTargetPool(state.difficulty),
                    difficulty = state.difficulty
                )
            }
            TrainingMode.AROUND_THE_CLOCK -> {
                LiveSessionState.AroundTheClock(difficulty = state.difficulty)
            }
            TrainingMode.SCORING_ROUNDS -> {
                LiveSessionState.ScoringRounds(targetAvg = state.difficulty.targetAvg)
            }
        }
        sessionStartedAt = System.currentTimeMillis()
        _uiState.update { it.copy(screen = TrainingScreenState.LIVE, liveSession = liveState) }
    }

    // ── Dart recording ────────────────────────────────────────────────────────

    /**
     * Record a dart in TARGET_FIELD or AROUND_THE_CLOCK mode.
     * [fieldString] is in the notation "S20", "D5", "T17", "Bull", "Bullseye", "Miss".
     * [actualMmX]/[actualMmY] are board coordinates in mm from centre (only set via board input).
     */
    fun recordDart(fieldString: String, actualMmX: Double? = null, actualMmY: Double? = null) {
        val session = _uiState.value.liveSession ?: return
        _uiState.update { it.copy(pendingMultiplier = 1) }
        when (session) {
            is LiveSessionState.TargetField    -> recordTargetFieldDart(session, fieldString, actualMmX, actualMmY)
            is LiveSessionState.AroundTheClock -> recordAtcDart(session, fieldString, actualMmX, actualMmY)
            is LiveSessionState.ScoringRounds  -> { /* use recordScoringDart */ }
        }
    }

    /** Record a dart from the board input widget (score+multiplier + exact mm coordinates). */
    fun recordBoardDart(score: Int, mult: Int, mmX: Float, mmY: Float) {
        if (_uiState.value.liveSession is LiveSessionState.ScoringRounds) {
            recordScoringDart(score * mult)
        } else {
            recordDart(dartToFieldString(score, mult), mmX.toDouble(), mmY.toDouble())
        }
    }

    private fun recordTargetFieldDart(
        session: LiveSessionState.TargetField,
        fieldString: String,
        actualMmX: Double?,
        actualMmY: Double?
    ) {
        if (session.isComplete) return
        val isHit = isTargetFieldHit(session.currentTarget, fieldString, session.difficulty)
        val (targetX, targetY) = fieldCentroid(session.currentTarget) ?: Pair(null, null)
        val record = TrainingThrowRecord(
            targetField = session.currentTarget,
            actualField = fieldString,
            isHit       = isHit,
            targetMmX   = targetX,
            targetMmY   = targetY,
            actualMmX   = actualMmX,
            actualMmY   = actualMmY
        )
        val newThrows = session.throws + record
        val newIdx    = if (isHit) session.currentIdx + 1 else session.currentIdx
        val updated   = session.copy(throws = newThrows, currentIdx = newIdx)
        _uiState.update { it.copy(liveSession = updated) }
        if (updated.isComplete) finishSession()
    }

    private fun recordAtcDart(
        session: LiveSessionState.AroundTheClock,
        fieldString: String,
        actualMmX: Double?,
        actualMmY: Double?
    ) {
        if (session.isComplete) return
        val isHit = isAtcHit(fieldString, session.currentNumber, session.difficulty)
        val targetField = session.currentTargetField
        val (targetX, targetY) = fieldCentroid(targetField) ?: Pair(null, null)
        val record = TrainingThrowRecord(
            targetField = targetField,
            actualField = fieldString,
            isHit       = isHit,
            targetMmX   = targetX,
            targetMmY   = targetY,
            actualMmX   = actualMmX,
            actualMmY   = actualMmY
        )
        val newThrows = session.throws + record
        val newNumber = if (isHit) session.currentNumber + 1 else session.currentNumber
        val updated   = session.copy(throws = newThrows, currentNumber = newNumber)
        _uiState.update { it.copy(liveSession = updated) }
        if (updated.isComplete) finishSession()
    }

    /** Record a single dart score (0-60) in SCORING_ROUNDS mode. Auto-closes round after 3 darts. */
    fun recordScoringDart(score: Int) {
        val session = _uiState.value.liveSession as? LiveSessionState.ScoringRounds ?: return
        if (session.isComplete) return
        val newRoundDarts = session.currentRoundDarts + score
        val updated = if (newRoundDarts.size >= 3) {
            // Round complete
            session.copy(
                completedRounds    = session.completedRounds + newRoundDarts.sum(),
                currentRoundDarts  = emptyList()
            )
        } else {
            session.copy(currentRoundDarts = newRoundDarts)
        }
        _uiState.update { it.copy(liveSession = updated) }
        if (updated.isComplete) finishSession()
    }

    // ── Undo ─────────────────────────────────────────────────────────────────

    fun undoLastDart() {
        val session = _uiState.value.liveSession ?: return
        when (session) {
            is LiveSessionState.TargetField    -> undoTargetField(session)
            is LiveSessionState.AroundTheClock -> undoAtc(session)
            is LiveSessionState.ScoringRounds  -> undoScoring(session)
        }
    }

    private fun undoTargetField(session: LiveSessionState.TargetField) {
        if (session.throws.isEmpty()) return
        val last    = session.throws.last()
        val newThrows = session.throws.dropLast(1)
        val newIdx  = if (last.isHit) session.currentIdx - 1 else session.currentIdx
        _uiState.update { it.copy(liveSession = session.copy(throws = newThrows, currentIdx = newIdx.coerceAtLeast(0))) }
    }

    private fun undoAtc(session: LiveSessionState.AroundTheClock) {
        if (session.throws.isEmpty()) return
        val last      = session.throws.last()
        val newThrows = session.throws.dropLast(1)
        val newNumber = if (last.isHit) session.currentNumber - 1 else session.currentNumber
        _uiState.update { it.copy(liveSession = session.copy(throws = newThrows, currentNumber = newNumber.coerceAtLeast(1))) }
    }

    private fun undoScoring(session: LiveSessionState.ScoringRounds) {
        if (session.currentRoundDarts.isNotEmpty()) {
            // Undo dart within current round
            _uiState.update { it.copy(liveSession = session.copy(currentRoundDarts = session.currentRoundDarts.dropLast(1))) }
        } else if (session.completedRounds.isNotEmpty()) {
            // Undo last completed round — restore 3rd dart as current round dart again
            val lastRoundTotal = session.completedRounds.last()
            _uiState.update { it.copy(liveSession = session.copy(
                completedRounds   = session.completedRounds.dropLast(1),
                currentRoundDarts = listOf(lastRoundTotal)   // show total as single "dart" for simplicity
            ))}
        }
    }

    // ── Session completion ────────────────────────────────────────────────────

    private fun finishSession() {
        val state   = _uiState.value
        val session = state.liveSession ?: return
        val player  = state.selectedPlayer ?: return

        val (result, completedCount) = when (session) {
            is LiveSessionState.TargetField    -> Pair(session.totalDarts, session.currentIdx)
            is LiveSessionState.AroundTheClock -> Pair(session.totalDarts, session.currentNumber - 1)
            is LiveSessionState.ScoringRounds  -> {
                val avg10 = (session.runningAverage * 10).roundToInt()
                Pair(avg10, session.completedRounds.size)
            }
        }

        _uiState.update { it.copy(screen = TrainingScreenState.DONE, isSaving = true) }

        viewModelScope.launch {
            val throws = when (session) {
                is LiveSessionState.TargetField, is LiveSessionState.AroundTheClock -> {
                    session.throws.mapIndexed { idx, r ->
                        TrainingThrow(
                            sessionId    = 0L,   // set by repository
                            throwIndex   = idx,
                            targetField  = r.targetField,
                            actualField  = r.actualField,
                            isHit        = r.isHit,
                            targetX      = r.targetMmX,
                            targetY      = r.targetMmY,
                            actualX      = r.actualMmX,
                            actualY      = r.actualMmY
                        )
                    }
                }
                is LiveSessionState.ScoringRounds -> emptyList()
            }

            val sessionId = trainingRepository.saveSession(
                playerId       = player.id,
                mode           = state.mode,
                difficulty     = state.difficulty,
                result         = result,
                completedCount = completedCount,
                throws         = throws,
                startedAt      = sessionStartedAt
            )
            val saved = trainingRepository.getRecentResults(player.id, state.mode, 10)
            _uiState.update { it.copy(
                recentResults = saved,
                isSaving      = false,
                lastResult    = saved.firstOrNull { it.id == sessionId }
            )}
        }
    }

    // ── Post-session actions ──────────────────────────────────────────────────

    fun repeatSession() {
        startSession()
    }

    fun backToSetup() {
        _uiState.update { it.copy(screen = TrainingScreenState.SETUP, liveSession = null) }
        reloadRecentResults()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isAtcHit(fieldString: String, number: Int, difficulty: TrainingDifficulty): Boolean =
        when (difficulty) {
            TrainingDifficulty.BEGINNER     ->
                fieldString == "S$number" || fieldString == "D$number" || fieldString == "T$number"
            TrainingDifficulty.INTERMEDIATE ->
                if (number > 20) fieldString == "Bull" || fieldString == "Bullseye"
                else fieldString == "D$number"
            TrainingDifficulty.PRO          ->
                if (number > 20) fieldString == "Bullseye"
                else fieldString == "T$number"
        }
}
