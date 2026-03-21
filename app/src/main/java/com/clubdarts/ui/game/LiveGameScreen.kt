package com.clubdarts.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(uiState.screen) {
        if (uiState.screen == GameScreen.RESULT) {
            onGameFinished()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
            // Player strip pinned at top
            PlayerStrip(
                players = uiState.players,
                currentPlayerIndex = uiState.currentPlayerIndex,
                scores = uiState.scores,
                legWins = uiState.legWins,
                currentDarts = uiState.currentDarts,
                modifier = Modifier.fillMaxWidth()
            )

            // Flexible gap — pushes bottom section down
            Spacer(modifier = Modifier.weight(1f))

            // Checkout hint bar — always occupies space so numpad never moves
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(Surface2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Checkout hint",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.checkoutHint != null) TextTertiary else androidx.compose.ui.graphics.Color.Transparent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
                Text(
                    text = uiState.checkoutHint ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Medium,
                    color = Green,
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
}
