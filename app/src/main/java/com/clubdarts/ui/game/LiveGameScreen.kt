package com.clubdarts.ui.game

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

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
                .padding(padding),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Player strip
            PlayerStrip(
                players = uiState.players,
                currentPlayerIndex = uiState.currentPlayerIndex,
                scores = uiState.scores,
                legWins = uiState.legWins,
                currentDarts = uiState.currentDarts,
                modifier = Modifier.fillMaxWidth()
            )

            // Checkout hint bar
            if (uiState.checkoutHint != null) {
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
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Text(
                        text = uiState.checkoutHint!!,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = DmMono,
                        fontWeight = FontWeight.Medium,
                        color = Green,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // Numpad
            DartNumpad(
                pendingMultiplier = uiState.pendingMultiplier,
                onMultiplierChange = { viewModel.setMultiplier(it) },
                onDart = { viewModel.recordDart(it) },
                onMiss = { viewModel.recordMiss() },
                onUndo = { viewModel.undoLastDart() },
                modifier = Modifier.fillMaxWidth()
            )

            // Visit history
            VisitHistory(
                visits = uiState.visitHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }
    }
}
