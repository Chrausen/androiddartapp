package com.clubdarts.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.ui.game.components.DartNumpad
import com.clubdarts.ui.game.components.PlayerStrip
import com.clubdarts.ui.game.components.VisitHistory
import com.clubdarts.ui.theme.*

@Composable
fun LiveGameScreen(
    onGameFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAbortDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
        ) {
            // Status bar — game mode info + abort button
            uiState.config?.let { config ->
                GameStatusBar(
                    config = config,
                    onAbort = { showAbortDialog = true }
                )
            }

            // Player strip — fills all space above the bottom section, scrollable when needed
            PlayerStrip(
                players = uiState.players,
                currentPlayerIndex = uiState.currentPlayerIndex,
                scores = uiState.scores,
                legWins = uiState.legWins,
                currentDarts = uiState.currentDarts,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            // Checkout hint bar — always visible so the numpad never moves.
            // Shows the suggested finish path when in checkout range, "—" otherwise.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Surface2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Checkout",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Text(
                    text = uiState.checkoutHint ?: "—",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Medium,
                    color = if (uiState.checkoutHint != null) Green else TextTertiary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // Numpad — fixed position, never moves
            DartNumpad(
                pendingMultiplier = uiState.pendingMultiplier,
                onMultiplierChange = { viewModel.setMultiplier(it) },
                onDart = { viewModel.recordDart(it) },
                onMiss = { viewModel.recordMiss() },
                onUndo = { viewModel.undoLastDart() },
                modifier = Modifier.fillMaxWidth()
            )

            // Visit history — fixed 3-row height, never shifts the numpad
            VisitHistory(
                visits = uiState.visitHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }
    }

    // Mid-game abort confirmation
    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            containerColor = Surface2,
            title = {
                Text("Abort game?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            },
            text = {
                Text(
                    "The current game will be abandoned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAbortDialog = false
                    viewModel.abortGame()
                    onBack()
                }) {
                    Text("Abort", color = Red, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Navigate to result screen automatically when the game is won
    LaunchedEffect(uiState.screen) {
        if (uiState.screen == GameScreen.RESULT) {
            onGameFinished()
        }
    }
}

@Composable
private fun GameStatusBar(
    config: GameConfig,
    onAbort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface2)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${config.startScore}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DmMono,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text("·", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(
                text = config.checkoutRule.displayName(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text("·", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(
                text = "${config.legsToWin} leg${if (config.legsToWin > 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        Surface(
            onClick = onAbort,
            color = androidx.compose.ui.graphics.Color.Transparent,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "Abort",
                style = MaterialTheme.typography.labelMedium,
                color = Red,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

private fun CheckoutRule.displayName() = when (this) {
    CheckoutRule.STRAIGHT -> "Straight out"
    CheckoutRule.DOUBLE   -> "Double out"
    CheckoutRule.TRIPLE   -> "Triple out"
}
