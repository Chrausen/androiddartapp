package com.clubdarts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.CommentaryPhrases
import com.clubdarts.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CommentaryTier { BAD, NORMAL, GOOD }

data class CommentarySettingsUiState(
    val bad: List<String> = emptyList(),
    val normal: List<String> = emptyList(),
    val good: List<String> = emptyList(),
    val addDialogTier: CommentaryTier? = null,
    val addDialogText: String = ""
)

@HiltViewModel
class CommentarySettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentarySettingsUiState())
    val uiState: StateFlow<CommentarySettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeCommentaryPhrases().collect { phrases ->
                _uiState.update {
                    it.copy(bad = phrases.bad, normal = phrases.normal, good = phrases.good)
                }
            }
        }
    }

    fun showAddDialog(tier: CommentaryTier) {
        _uiState.update { it.copy(addDialogTier = tier, addDialogText = "") }
    }

    fun updateDialogText(text: String) {
        _uiState.update { it.copy(addDialogText = text) }
    }

    fun confirmAdd() {
        val state = _uiState.value
        val text = state.addDialogText.trim()
        if (text.isEmpty() || state.addDialogTier == null) {
            _uiState.update { it.copy(addDialogTier = null, addDialogText = "") }
            return
        }
        val updated = when (state.addDialogTier) {
            CommentaryTier.BAD    -> state.copy(bad    = state.bad    + text, addDialogTier = null, addDialogText = "")
            CommentaryTier.NORMAL -> state.copy(normal = state.normal + text, addDialogTier = null, addDialogText = "")
            CommentaryTier.GOOD   -> state.copy(good   = state.good   + text, addDialogTier = null, addDialogText = "")
        }
        _uiState.update { updated }
        persist(updated)
    }

    fun deletePhrase(tier: CommentaryTier, index: Int) {
        val state = _uiState.value
        val updated = when (tier) {
            CommentaryTier.BAD    -> state.copy(bad    = state.bad.filterIndexed    { i, _ -> i != index })
            CommentaryTier.NORMAL -> state.copy(normal = state.normal.filterIndexed { i, _ -> i != index })
            CommentaryTier.GOOD   -> state.copy(good   = state.good.filterIndexed  { i, _ -> i != index })
        }
        _uiState.update { updated }
        persist(updated)
    }

    fun dismissDialog() {
        _uiState.update { it.copy(addDialogTier = null, addDialogText = "") }
    }

    private fun persist(state: CommentarySettingsUiState) {
        viewModelScope.launch {
            settingsRepository.saveCommentaryPhrases(
                CommentaryPhrases(bad = state.bad, normal = state.normal, good = state.good)
            )
        }
    }
}
