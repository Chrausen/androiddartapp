package com.clubdarts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.TtsPhrase
import com.clubdarts.data.model.TtsScoreSetting
import com.clubdarts.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TtsSettingsUiState(
    val scoreSettings: List<TtsScoreSetting> = emptyList(),
    val dialogState: TtsDialogState? = null
)

sealed class TtsDialogState {
    data class Add(
        val score: String = "",
        val phrases: List<TtsPhrase> = listOf(TtsPhrase("", "")),
        val scoreError: String? = null
    ) : TtsDialogState()

    data class Edit(
        val originalScore: Int,
        val score: String,
        val phrases: List<TtsPhrase>,
        val scoreError: String? = null
    ) : TtsDialogState()
}

@HiltViewModel
class TtsSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TtsSettingsUiState())
    val uiState: StateFlow<TtsSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeTtsScoreSettings().collect { settings ->
                _uiState.update { it.copy(scoreSettings = settings) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(dialogState = TtsDialogState.Add()) }
    }

    fun showEditDialog(setting: TtsScoreSetting) {
        _uiState.update {
            it.copy(
                dialogState = TtsDialogState.Edit(
                    originalScore = setting.score,
                    score = setting.score.toString(),
                    phrases = if (setting.phrases.isEmpty()) listOf(TtsPhrase("", "")) else setting.phrases
                )
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    fun updateDialogScore(score: String) {
        _uiState.update { state ->
            when (val d = state.dialogState) {
                is TtsDialogState.Add  -> state.copy(dialogState = d.copy(score = score, scoreError = null))
                is TtsDialogState.Edit -> state.copy(dialogState = d.copy(score = score, scoreError = null))
                null -> state
            }
        }
    }

    fun addPhrase() {
        _uiState.update { state ->
            when (val d = state.dialogState) {
                is TtsDialogState.Add  -> state.copy(dialogState = d.copy(phrases = d.phrases + TtsPhrase("", "")))
                is TtsDialogState.Edit -> state.copy(dialogState = d.copy(phrases = d.phrases + TtsPhrase("", "")))
                null -> state
            }
        }
    }

    fun updatePhrase(index: Int, before: String, after: String) {
        _uiState.update { state ->
            fun List<TtsPhrase>.updated() = toMutableList().also { it[index] = TtsPhrase(before, after) }
            when (val d = state.dialogState) {
                is TtsDialogState.Add  -> state.copy(dialogState = d.copy(phrases = d.phrases.updated()))
                is TtsDialogState.Edit -> state.copy(dialogState = d.copy(phrases = d.phrases.updated()))
                null -> state
            }
        }
    }

    fun removePhrase(index: Int) {
        _uiState.update { state ->
            fun List<TtsPhrase>.removed(): List<TtsPhrase> {
                val list = toMutableList()
                list.removeAt(index)
                return if (list.isEmpty()) listOf(TtsPhrase("", "")) else list
            }
            when (val d = state.dialogState) {
                is TtsDialogState.Add  -> state.copy(dialogState = d.copy(phrases = d.phrases.removed()))
                is TtsDialogState.Edit -> state.copy(dialogState = d.copy(phrases = d.phrases.removed()))
                null -> state
            }
        }
    }

    fun saveSetting() {
        val dialog = _uiState.value.dialogState ?: return
        val (scoreStr, phrases, originalScore) = when (dialog) {
            is TtsDialogState.Add  -> Triple(dialog.score, dialog.phrases, null as Int?)
            is TtsDialogState.Edit -> Triple(dialog.score, dialog.phrases, dialog.originalScore)
        }

        val score = scoreStr.trim().toIntOrNull()
        if (score == null || score < 0 || score > 180) {
            val error = "Enter a valid score (0–180)"
            _uiState.update { state ->
                when (val d = state.dialogState) {
                    is TtsDialogState.Add  -> state.copy(dialogState = d.copy(scoreError = error))
                    is TtsDialogState.Edit -> state.copy(dialogState = d.copy(scoreError = error))
                    null -> state
                }
            }
            return
        }

        // Check for duplicate score (only when adding, or when score changed during edit)
        val currentSettings = _uiState.value.scoreSettings
        val isDuplicate = currentSettings.any { it.score == score && it.score != originalScore }
        if (isDuplicate) {
            val error = "Score $score already has settings"
            _uiState.update { state ->
                when (val d = state.dialogState) {
                    is TtsDialogState.Add  -> state.copy(dialogState = d.copy(scoreError = error))
                    is TtsDialogState.Edit -> state.copy(dialogState = d.copy(scoreError = error))
                    null -> state
                }
            }
            return
        }

        val cleanedPhrases = phrases.filter { it.before.isNotBlank() || it.after.isNotBlank() }

        viewModelScope.launch {
            val updated = currentSettings.toMutableList().apply {
                removeAll { it.score == originalScore || it.score == score && originalScore == null }
                add(TtsScoreSetting(score = score, phrases = cleanedPhrases))
                sortBy { it.score }
            }
            settingsRepository.saveTtsScoreSettings(updated)
            _uiState.update { it.copy(dialogState = null) }
        }
    }

    fun deleteSetting(score: Int) {
        viewModelScope.launch {
            val updated = _uiState.value.scoreSettings.filter { it.score != score }
            settingsRepository.saveTtsScoreSettings(updated)
        }
    }
}
