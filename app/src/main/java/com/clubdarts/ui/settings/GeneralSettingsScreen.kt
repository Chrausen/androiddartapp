package com.clubdarts.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.clubdarts.R
import com.clubdarts.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                if (!uiState.soundEffectsMuted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.general_sound_effects_volume_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.width(60.dp)
                        )
                        Slider(
                            value = uiState.soundEffectsVolume,
                            onValueChange = { viewModel.setSoundEffectsVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = Surface3
                            )
                        )
                        Text(
                            text = "${(uiState.soundEffectsVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }

        }
    }
}
