package com.clubdarts.ui.settings.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubdarts.data.repository.BackupException
import com.clubdarts.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface BackupUiState {
    object Idle : BackupUiState
    object Loading : BackupUiState
    data class Success(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onExportFileCreated(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            try {
                val json = backupRepository.exportJson()
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val jsonWithPrefs = appendSharedPreferences(json, prefs)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonWithPrefs.toByteArray(Charsets.UTF_8))
                    } ?: throw Exception("Could not open output stream")
                }
                _uiState.value = BackupUiState.Success("export")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("export")
            }
        }
    }

    fun onImportFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Loading
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    } ?: throw Exception("Could not open input stream")
                }
                backupRepository.importJson(json)
                restoreSharedPreferences(json)
                _uiState.value = BackupUiState.Success("import")
            } catch (e: BackupException) {
                _uiState.value = BackupUiState.Error("parse")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("read")
            }
        }
    }

    fun dismissResult() {
        _uiState.value = BackupUiState.Idle
    }

    private fun appendSharedPreferences(json: String, prefs: SharedPreferences): String {
        return try {
            val root = org.json.JSONObject(json)
            val prefsObj = org.json.JSONObject().apply {
                put(KEY_LANGUAGE, prefs.getString(KEY_LANGUAGE, "en") ?: "en")
                put(KEY_ANIMATIONS, prefs.getBoolean(KEY_ANIMATIONS, true))
            }
            root.put("sharedPreferences", prefsObj)
            root.toString(2)
        } catch (_: Exception) {
            json
        }
    }

    private fun restoreSharedPreferences(json: String) {
        val prefsObj = backupRepository.parseSharedPreferences(json) ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_LANGUAGE, prefsObj.optString(KEY_LANGUAGE, "en"))
            putBoolean(KEY_ANIMATIONS, prefsObj.optBoolean(KEY_ANIMATIONS, true))
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "club_darts_prefs"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_ANIMATIONS = "animations_enabled"
    }
}
