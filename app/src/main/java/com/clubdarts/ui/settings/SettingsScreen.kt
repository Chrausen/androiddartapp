package com.clubdarts.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.ui.theme.*

@Composable
fun SettingsScreen(
    onNavigateToTtsScores: () -> Unit,
    onNavigateToRankingSettings: () -> Unit,
    onNavigateToGeneralSettings: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.deleteSuccess) {
        LaunchedEffect(Unit) { viewModel.clearDeleteSuccess() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(
                icon = Icons.Default.Tune,
                iconTint = Accent,
                title = stringResource(R.string.settings_general),
                subtitle = stringResource(R.string.settings_general_subtitle),
                onClick = onNavigateToGeneralSettings
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.EmojiEvents,
                iconTint = Accent,
                title = stringResource(R.string.settings_ranking_system),
                subtitle = stringResource(R.string.settings_ranking_subtitle),
                onClick = onNavigateToRankingSettings
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.RecordVoiceOver,
                iconTint = Accent,
                title = stringResource(R.string.settings_tts_phrases),
                subtitle = stringResource(R.string.settings_tts_subtitle),
                onClick = onNavigateToTtsScores
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                icon = Icons.Default.DeleteForever,
                iconTint = Red,
                title = stringResource(R.string.settings_delete_all),
                subtitle = stringResource(R.string.settings_delete_all_subtitle),
                titleColor = Red,
                onClick = { viewModel.requestDeleteAll() }
            )
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

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color = Accent,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
