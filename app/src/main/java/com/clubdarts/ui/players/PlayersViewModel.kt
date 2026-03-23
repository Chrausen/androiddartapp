package com.clubdarts.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.GameRepository
import com.clubdarts.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayersUiState(
    val players: List<Player> = emptyList(),
    val activeGamePlayerIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val dialogState: PlayerDialogState? = null
)

sealed class PlayerDialogState {
    data class Add(val name: String = "", val error: String? = null) : PlayerDialogState()
    data class Edit(val player: Player, val name: String = player.name, val error: String? = null) : PlayerDialogState()
    data class ConfirmDelete(val player: Player) : PlayerDialogState()
}

@HiltViewModel
class PlayersViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayersUiState())
    val uiState: StateFlow<PlayersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.getAllPlayers().collect { players ->
                _uiState.update { it.copy(players = players) }
            }
        }
        viewModelScope.launch {
            gameRepository.observeActiveGamePlayerIds().collect { ids ->
                _uiState.update { it.copy(activeGamePlayerIds = ids.toSet()) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(dialogState = PlayerDialogState.Add()) }
    }

    fun showEditDialog(player: Player) {
        _uiState.update { it.copy(dialogState = PlayerDialogState.Edit(player)) }
    }

    fun showDeleteConfirmDialog(player: Player) {
        _uiState.update { it.copy(dialogState = PlayerDialogState.ConfirmDelete(player)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    fun updateDialogName(name: String) {
        val trimmed = name.trim()
        _uiState.update {
            it.copy(dialogState = when (val current = it.dialogState) {
                is PlayerDialogState.Add -> {
                    val dup = trimmed.isNotBlank() && it.players.any { p -> p.name.equals(trimmed, ignoreCase = true) }
                    current.copy(name = name, error = if (dup) "Player already exists" else null)
                }
                is PlayerDialogState.Edit -> {
                    val dup = trimmed.isNotBlank() && it.players.any { p -> p.name.equals(trimmed, ignoreCase = true) && p.id != current.player.id }
                    current.copy(name = name, error = if (dup) "Player name already taken" else null)
                }
                else -> current
            })
        }
    }

    fun savePlayer() {
        val state = _uiState.value
        viewModelScope.launch {
            when (val dialog = state.dialogState) {
                is PlayerDialogState.Add -> {
                    val name = dialog.name.trim()
                    if (name.isBlank()) {
                        _uiState.update { it.copy(dialogState = dialog.copy(error = "Name cannot be empty")) }
                        return@launch
                    }
                    val isDuplicate = state.players.any { it.name.equals(name, ignoreCase = true) }
                    if (isDuplicate) {
                        _uiState.update { it.copy(dialogState = dialog.copy(error = "Player already exists")) }
                        return@launch
                    }
                    playerRepository.insertPlayer(Player(name = name))
                    _uiState.update { it.copy(dialogState = null) }
                }
                is PlayerDialogState.Edit -> {
                    val name = dialog.name.trim()
                    if (name.isBlank()) {
                        _uiState.update { it.copy(dialogState = dialog.copy(error = "Name cannot be empty")) }
                        return@launch
                    }
                    val isDuplicate = state.players.any { it.name.equals(name, ignoreCase = true) && it.id != dialog.player.id }
                    if (isDuplicate) {
                        _uiState.update { it.copy(dialogState = dialog.copy(error = "Player name already taken")) }
                        return@launch
                    }
                    playerRepository.updatePlayer(dialog.player.copy(name = name))
                    _uiState.update { it.copy(dialogState = null) }
                }
                else -> {}
            }
        }
    }

    fun insertPlayerByName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val exists = _uiState.value.players.any { it.name.equals(trimmed, ignoreCase = true) }
            if (!exists) playerRepository.insertPlayer(Player(name = trimmed))
        }
    }

    fun confirmDelete() {
        val dialog = _uiState.value.dialogState as? PlayerDialogState.ConfirmDelete ?: return
        viewModelScope.launch {
            playerRepository.deletePlayer(dialog.player)
            _uiState.update { it.copy(dialogState = null) }
        }
    }
}
