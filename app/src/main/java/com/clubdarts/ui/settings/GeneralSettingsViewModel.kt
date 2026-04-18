package com.clubdarts.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.repository.SettingsRepository
import com.clubdarts.util.SoundEffectsService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GeneralSettingsUiState(
    val currentLanguage: String = "en",
    val shouldRecreate: Boolean = false,
    val animationsEnabled: Boolean = true,
    val soundEffectsMuted: Boolean = false,
    val soundEffectsVolume: Float = 1f,
    val randomCommentaryEnabled: Boolean = false
)

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val soundEffectsService: SoundEffectsService
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        GeneralSettingsUiState(
            currentLanguage = prefs.getString(KEY_LANGUAGE, "en") ?: "en",
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, true)
        )
    )
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val muted = settingsRepository.getSoundEffectsMuted()
            val volume = settingsRepository.getSoundEffectsVolume()
            val commentary = settingsRepository.getRandomCommentaryEnabled()
            soundEffectsService.setVolume(volume)
            _uiState.update {
                it.copy(
                    soundEffectsMuted = muted,
                    soundEffectsVolume = volume,
                    randomCommentaryEnabled = commentary
                )
            }
        }
    }

    fun setLanguage(lang: String) {
        if (lang == _uiState.value.currentLanguage) return
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _uiState.update { it.copy(currentLanguage = lang, shouldRecreate = true) }
    }

    fun onRecreated() {
        _uiState.update { it.copy(shouldRecreate = false) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANIMATIONS, enabled).apply()
        _uiState.update { it.copy(animationsEnabled = enabled) }
    }

    fun setSoundEffectsMuted(muted: Boolean) {
        soundEffectsService.setMuted(muted)
        _uiState.update { it.copy(soundEffectsMuted = muted) }
        viewModelScope.launch { settingsRepository.setSoundEffectsMuted(muted) }
    }

    fun setSoundEffectsVolume(volume: Float) {
        soundEffectsService.setVolume(volume)
        _uiState.update { it.copy(soundEffectsVolume = volume) }
        viewModelScope.launch { settingsRepository.setSoundEffectsVolume(volume) }
    }

    fun setRandomCommentaryEnabled(enabled: Boolean) {
        _uiState.update { it.copy(randomCommentaryEnabled = enabled) }
        viewModelScope.launch { settingsRepository.setRandomCommentaryEnabled(enabled) }
    }

    companion object {
        const val PREFS_NAME = "club_darts_prefs"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_ANIMATIONS = "animations_enabled"
    }
}
