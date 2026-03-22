package com.clubdarts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.repository.EloRepository
import com.clubdarts.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RankingSettingsUiState(
    val rankingEnabled: Boolean = false,
    val kFactor: Int = 32,
    val startScore: Int = 501,
    val checkoutRule: CheckoutRule = CheckoutRule.DOUBLE,
    val legsToWin: Int = 1,
    val showResetConfirm: Boolean = false,
    val resetSuccess: Boolean = false
)

@HiltViewModel
class RankingSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val eloRepository: EloRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingSettingsUiState())
    val uiState: StateFlow<RankingSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    rankingEnabled = settingsRepository.getRankingEnabled(),
                    kFactor = settingsRepository.getRankingKFactor(),
                    startScore = settingsRepository.getRankingStartScore(),
                    checkoutRule = settingsRepository.getRankingCheckoutRule(),
                    legsToWin = settingsRepository.getRankingLegsToWin()
                )
            }
        }
    }

    fun setRankingEnabled(v: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRankingEnabled(v)
            _uiState.update { it.copy(rankingEnabled = v) }
        }
    }

    fun setKFactor(v: Int) {
        viewModelScope.launch {
            settingsRepository.setRankingKFactor(v)
            _uiState.update { it.copy(kFactor = v) }
        }
    }

    fun setStartScore(v: Int) {
        viewModelScope.launch {
            settingsRepository.setRankingStartScore(v)
            _uiState.update { it.copy(startScore = v) }
        }
    }

    fun setCheckoutRule(v: CheckoutRule) {
        viewModelScope.launch {
            settingsRepository.setRankingCheckoutRule(v)
            _uiState.update { it.copy(checkoutRule = v) }
        }
    }

    fun setLegsToWin(v: Int) {
        viewModelScope.launch {
            settingsRepository.setRankingLegsToWin(v)
            _uiState.update { it.copy(legsToWin = v) }
        }
    }

    fun requestReset() {
        _uiState.update { it.copy(showResetConfirm = true) }
    }

    fun dismissReset() {
        _uiState.update { it.copy(showResetConfirm = false) }
    }

    fun confirmReset() {
        viewModelScope.launch {
            eloRepository.resetAllRatings()
            _uiState.update { it.copy(showResetConfirm = false, resetSuccess = true) }
        }
    }

    fun clearResetSuccess() {
        _uiState.update { it.copy(resetSuccess = false) }
    }
}
