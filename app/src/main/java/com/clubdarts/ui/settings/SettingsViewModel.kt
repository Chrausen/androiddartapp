package com.clubdarts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.SettingsKeys
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.SettingsRepository
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
    val hasActiveGame: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val playerDao: PlayerDao,
    private val gameRepository: GameRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            gameRepository.observeActiveGame().collect { game ->
                _uiState.update { it.copy(hasActiveGame = game != null) }
            }
        }
    }

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
}
