package com.clubdarts.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.BuildConfig
import com.clubdarts.R
import com.clubdarts.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    onDataDeleted: () -> Unit = {},
    viewModel: GeneralSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.shouldRecreate) {
        if (uiState.shouldRecreate) {
            viewModel.onRecreated()
            (context as? Activity)?.recreate()
        }
    }

    if (uiState.deleteSuccess) {
        LaunchedEffect(Unit) {
            onDataDeleted()
            viewModel.clearDeleteSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.general_title), color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back), tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.general_language_label),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val languages = listOf(
                "en" to stringResource(R.string.language_english),
                "de" to stringResource(R.string.language_german)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
            ) {
                languages.forEachIndexed { index, (code, label) ->
                    val isSelected = uiState.currentLanguage == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setLanguage(code) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Accent else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < languages.lastIndex) {
                        HorizontalDivider(
                            color = Border,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.general_animations_label),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.general_animations_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.general_animations_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uiState.animationsEnabled,
                    onCheckedChange = { viewModel.setAnimationsEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Background, checkedTrackColor = Accent)
                )
            }

            Text(
                text = stringResource(R.string.general_sound_effects_label),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.general_sound_effects_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.general_sound_effects_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = !uiState.soundEffectsMuted,
                    onCheckedChange = { viewModel.setSoundEffectsMuted(!it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Background, checkedTrackColor = Accent)
                )
            }

            // Delete all data
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
                    .clickable { viewModel.requestDeleteAll() }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Red,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_delete_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Red
                    )
                    Text(
                        stringResource(R.string.settings_delete_all_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(10.dp))
                        .clickable(
                            enabled = !uiState.isGeneratingDebugData,
                            onClick = { viewModel.generateDebugData() }
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isGeneratingDebugData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Accent
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Generate Debug Data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.isGeneratingDebugData) TextTertiary else TextPrimary
                        )
                        Text(
                            if (uiState.isGeneratingDebugData) "Creating 20 players and 500 games…"
                            else "20 players · 500 games",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text(stringResource(R.string.dialog_delete_all_title), color = TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.dialog_delete_all_message),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteAll() }) {
                    Text(stringResource(R.string.btn_delete), color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = Surface2
        )
    }
}
