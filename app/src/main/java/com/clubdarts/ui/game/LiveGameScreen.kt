package com.clubdarts.ui.game

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.ui.settings.GeneralSettingsViewModel
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.ui.game.components.DartBoardInput
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
    var showBoardInput  by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.toggleVoiceInput()
    }

    fun onMicClick() {
        if (uiState.isVoiceListening) {
            viewModel.stopVoiceInput()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) viewModel.toggleVoiceInput()
            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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
                    showBoardInput = showBoardInput,
                    onToggleBoardInput = { showBoardInput = !showBoardInput },
                    isVoiceListening = uiState.isVoiceListening,
                    onMicClick = { onMicClick() },
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
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.live_abort),
                        tint = Amber,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Input area — numpad (default) or board input (when toggled)
            if (showBoardInput) {
                // BoxWithConstraints gives us the actual available px so we can make
                // the board a perfect square that fills as much of the width as possible
                // without overflowing the screen height.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val boardSize = minOf(maxWidth, maxHeight)
                    DartBoardInput(
                        currentDarts = uiState.currentDarts,
                        onDartConfirmed = { score, mult, bx, by ->
                            viewModel.recordBoardDart(score, mult, bx, by)
                        },
                        modifier = Modifier.size(boardSize)
                    )
                }
            } else {
                DartNumpad(
                    pendingMultiplier = uiState.pendingMultiplier,
                    onMultiplierChange = { viewModel.setMultiplier(it) },
                    onDart = { viewModel.recordDart(it) },
                    onMiss = { viewModel.recordMiss() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Visit history — only shown when history is enabled and in numpad mode
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
    showBoardInput: Boolean,
    onToggleBoardInput: () -> Unit,
    isVoiceListening: Boolean,
    onMicClick: () -> Unit,
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
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                slashed = isTtsMuted,
                contentDescription = if (isTtsMuted) stringResource(R.string.live_unmute_tts) else stringResource(R.string.live_mute_tts),
                onClick = onToggleMute
            )
            SlashedIconButton(
                icon = Icons.Default.Adjust,
                slashed = !showBoardInput,
                contentDescription = if (showBoardInput) stringResource(R.string.live_hide_board_input) else stringResource(R.string.live_show_board_input),
                onClick = onToggleBoardInput
            )
            SlashedIconButton(
                icon = Icons.Default.History,
                slashed = !showHistory,
                contentDescription = if (showHistory) stringResource(R.string.live_hide_history) else stringResource(R.string.live_show_history),
                onClick = onToggleHistory
            )
            VoiceMicButton(
                isListening = isVoiceListening,
                onClick = onMicClick
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
private fun VoiceMicButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_alpha"
    )

    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isListening)
                stringResource(R.string.live_voice_stop)
            else
                stringResource(R.string.live_voice_start),
            tint = if (isListening) Red else TextTertiary,
            modifier = if (isListening) Modifier.alpha(pulseAlpha) else Modifier
        )
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
