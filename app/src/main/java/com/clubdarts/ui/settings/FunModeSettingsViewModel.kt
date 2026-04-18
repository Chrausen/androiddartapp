package com.clubdarts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FunModeSettingsUiState(
    val intervalRounds: Int = 1,
    val disabledRuleIds: Set<String> = emptySet(),
)

@HiltViewModel
class FunModeSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FunModeSettingsUiState())
    val uiState: StateFlow<FunModeSettingsUiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            val intervalRounds = settingsRepository.getFunModeIntervalRounds()
            val disabledRuleIds = settingsRepository.getFunModeDisabledRules().toSet()
            _uiState.update { it.copy(intervalRounds = intervalRounds, disabledRuleIds = disabledRuleIds) }
        }
    }

    fun setIntervalRounds(v: Int) {
        val clamped = v.coerceIn(1, 9)
        _uiState.update { it.copy(intervalRounds = clamped) }
        viewModelScope.launch { settingsRepository.setFunModeIntervalRounds(clamped) }
    }

    fun toggleRule(ruleId: String) {
        val current = _uiState.value.disabledRuleIds
        val updated = if (ruleId in current) current - ruleId else current + ruleId
        _uiState.update { it.copy(disabledRuleIds = updated) }
        viewModelScope.launch { settingsRepository.setFunModeDisabledRules(updated.toList()) }
    }
}
