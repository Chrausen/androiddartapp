package com.clubdarts.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.FunRuleCategory
import com.clubdarts.data.model.FunRules
import com.clubdarts.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunModeSettingsScreen(
    onBack: () -> Unit,
    viewModel: FunModeSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fun_mode_settings_title), color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = TextPrimary
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rule duration section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.fun_mode_settings_rule_duration_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fun_mode_settings_rule_duration_rounds_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.fun_mode_settings_rule_duration_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setIntervalRounds(uiState.intervalRounds - 1) },
                            enabled = uiState.intervalRounds > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = if (uiState.intervalRounds > 1) Accent else TextTertiary)
                        }
                        Text(
                            text = "${uiState.intervalRounds}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.widthIn(min = 24.dp),
                        )
                        IconButton(
                            onClick = { viewModel.setIntervalRounds(uiState.intervalRounds + 1) },
                            enabled = uiState.intervalRounds < 9
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = if (uiState.intervalRounds < 9) Accent else TextTertiary)
                        }
                    }
                }
            }

            // Rules section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.fun_mode_settings_rules_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                val physicalRules = FunRules.all.filter { it.category == FunRuleCategory.PHYSICAL }
                val scoringRules = FunRules.all.filter { it.category == FunRuleCategory.SCORING }

                Text(
                    text = stringResource(R.string.fun_mode_settings_category_physical),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(10.dp))
                ) {
                    physicalRules.forEachIndexed { index, rule ->
                        RuleToggleRow(
                            emoji = rule.emoji,
                            title = stringResource(rule.titleRes),
                            subtitle = stringResource(rule.descRes),
                            enabled = rule.id !in uiState.disabledRuleIds,
                            onToggle = { viewModel.toggleRule(rule.id) }
                        )
                        if (index < physicalRules.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = Surface2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.fun_mode_settings_category_scoring),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(10.dp))
                ) {
                    scoringRules.forEachIndexed { index, rule ->
                        RuleToggleRow(
                            emoji = rule.emoji,
                            title = stringResource(rule.titleRes),
                            subtitle = stringResource(rule.descRes),
                            enabled = rule.id !in uiState.disabledRuleIds,
                            onToggle = { viewModel.toggleRule(rule.id) }
                        )
                        if (index < scoringRules.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = Surface2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RuleToggleRow(
    emoji: String,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) TextPrimary else TextTertiary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = Accent,
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = Surface2
            )
        )
    }
}
