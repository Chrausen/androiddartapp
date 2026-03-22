package com.clubdarts.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingSettingsScreen(
    onBack: () -> Unit,
    viewModel: RankingSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.resetSuccess) {
        LaunchedEffect(Unit) { viewModel.clearResetSuccess() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ranking System", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Enable toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Enable Ranking System",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        "Track player Elo ratings and show leaderboard",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = uiState.rankingEnabled,
                    onCheckedChange = { viewModel.setRankingEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Background,
                        checkedTrackColor = Accent
                    )
                )
            }

            // Settings only visible when ranking is enabled
            AnimatedVisibility(
                visible = uiState.rankingEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // K-Factor
                    Column {
                        RankingSectionLabel("Rating Aggressiveness (K-Factor)")
                        Text(
                            text = if (uiState.kFactor == 32)
                                "Standard (32) — slower, more stable ratings"
                            else
                                "Aggressive (64) — faster, more volatile ratings",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        RankingSegmentedRow(
                            options = listOf(32, 64),
                            selected = uiState.kFactor,
                            onSelect = { viewModel.setKFactor(it) },
                            label = { it.toString() }
                        )
                    }

                    // Game Mode
                    Column {
                        RankingSectionLabel("Game Mode for Ranked Matches")

                        Spacer(modifier = Modifier.height(8.dp))
                        RankingSectionLabel("Starting score")
                        RankingSegmentedRow(
                            options = listOf(201, 301, 401, 501, 701),
                            selected = uiState.startScore,
                            onSelect = { viewModel.setStartScore(it) },
                            label = { it.toString() }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        RankingSectionLabel("Checkout rule")
                        RankingSegmentedRow(
                            options = CheckoutRule.values().toList(),
                            selected = uiState.checkoutRule,
                            onSelect = { viewModel.setCheckoutRule(it) },
                            label = { it.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        RankingSectionLabel("Legs to win")
                        RankingSegmentedRow(
                            options = listOf(1, 3, 5, 7, 9),
                            selected = uiState.legsToWin,
                            onSelect = { viewModel.setLegsToWin(it) },
                            label = { it.toString() }
                        )
                    }

                    // Reset data
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(10.dp))
                            .clickable { viewModel.requestReset() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
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
                                "Reset all rating data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Red
                            )
                            Text(
                                "Reset all players to 1000 Elo and delete match history",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (uiState.showResetConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReset() },
            title = { Text("Reset all rating data?", color = TextPrimary) },
            text = {
                Text(
                    "This will reset all players to 1000 Elo and delete all ranked match history. This cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmReset() }) {
                    Text("Reset", color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissReset() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Surface2
        )
    }
}

@Composable
private fun RankingSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun <T> RankingSegmentedRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .background(
                        color = if (option == selected) Accent else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (option == selected) Background else TextSecondary
                )
            }
        }
    }
}
