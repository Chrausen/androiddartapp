package com.clubdarts.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GeneralSettingsUiState(
    val currentLanguage: String = "en",
    val shouldRecreate: Boolean = false,
    val animationsEnabled: Boolean = true
)

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        GeneralSettingsUiState(
            currentLanguage = prefs.getString(KEY_LANGUAGE, "en") ?: "en",
            animationsEnabled = prefs.getBoolean(KEY_ANIMATIONS, true)
        )
    )
    val uiState: StateFlow<GeneralSettingsUiState> = _uiState.asStateFlow()

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

    companion object {
        const val PREFS_NAME = "club_darts_prefs"
        const val KEY_LANGUAGE = "app_language"
        const val KEY_ANIMATIONS = "animations_enabled"
    }
}
