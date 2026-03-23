package com.clubdarts.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.clubdarts.R
import com.clubdarts.ui.settings.GeneralSettingsViewModel
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

    val context = LocalContext.current
    val animationsEnabled = remember {
        context.getSharedPreferences(GeneralSettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(GeneralSettingsViewModel.KEY_ANIMATIONS, true)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showAbortDialog by remember { mutableStateOf(false) }

    BackHandler { showAbortDialog = true }

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
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Status bar — game mode info + controls + abort button
            uiState.config?.let { config ->
                GameStatusBar(
                    config = config,
                    currentLegNumber = uiState.currentLegNumber,
                    isTtsMuted = uiState.isTtsMuted,
                    onToggleMute = { viewModel.toggleTtsMute() },
                    showHistory = uiState.showHistory,
                    onToggleHistory = { viewModel.toggleHistory() },
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
                modifier = Modifier.fillMaxWidth().weight(1f),
                isTeamGame = uiState.isTeamGame,
                teamAssignments = uiState.teamAssignments,
                teamScores = uiState.teamScores,
                teamLegWins = uiState.teamLegWins,
                animationsEnabled = animationsEnabled
            )

            // Checkout hint bar — always visible so the numpad never moves.
            // Shows the suggested finish path when in checkout range, "—" otherwise.
            // Undo button lives here to keep the numpad clean.
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
                IconButton(
                    onClick = { viewModel.undoLastDart() },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = stringResource(R.string.live_abort),
                        tint = Amber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Numpad — fixed position, never moves
            DartNumpad(
                pendingMultiplier = uiState.pendingMultiplier,
                onMultiplierChange = { viewModel.setMultiplier(it) },
                onDart = { viewModel.recordDart(it) },
                onMiss = { viewModel.recordMiss() },
                modifier = Modifier.fillMaxWidth()
            )

            // Visit history — only shown when history is enabled
            if (uiState.showHistory) {
                VisitHistory(
                    visits = uiState.visitHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }
    }

    // Mid-game abort confirmation
    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            containerColor = Surface2,
            title = {
                Text(stringResource(R.string.dialog_abort_game_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            },
            text = {
                Text(
                    stringResource(R.string.dialog_abort_game_message),
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
                    Text(stringResource(R.string.btn_abort), color = Red, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
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
    currentLegNumber: Int,
    isTtsMuted: Boolean,
    onToggleMute: () -> Unit,
    showHistory: Boolean,
    onToggleHistory: () -> Unit,
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
            Text("·", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(
                text = "Leg $currentLegNumber",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SlashedIconButton(
                icon = Icons.Default.VolumeUp,
                slashed = isTtsMuted,
                contentDescription = if (isTtsMuted) stringResource(R.string.live_unmute_tts) else stringResource(R.string.live_mute_tts),
                onClick = onToggleMute
            )
            SlashedIconButton(
                icon = Icons.Default.History,
                slashed = !showHistory,
                contentDescription = if (showHistory) stringResource(R.string.live_hide_history) else stringResource(R.string.live_show_history),
                onClick = onToggleHistory
            )
            Surface(
                onClick = onAbort,
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.live_abort),
                    style = MaterialTheme.typography.labelMedium,
                    color = Red,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SlashedIconButton(
    icon: ImageVector,
    slashed: Boolean,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val tint = if (slashed) TextTertiary else TextSecondary
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
            if (slashed) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawLine(
                        color = tint,
                        start = Offset(size.width * 0.2f, size.height * 0.8f),
                        end = Offset(size.width * 0.8f, size.height * 0.2f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun CheckoutRule.displayName() = when (this) {
    CheckoutRule.STRAIGHT -> "Straight out"
    CheckoutRule.DOUBLE   -> "Double out"
    CheckoutRule.TRIPLE   -> "Triple out"
}
