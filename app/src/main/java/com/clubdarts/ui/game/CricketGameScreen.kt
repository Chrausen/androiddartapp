package com.clubdarts.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.ui.theme.*

private val CRICKET_NUMBERS = listOf(20, 19, 18, 17, 16, 15, 25)

@Composable
fun CricketGameScreen(
    onGameFinished: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAbortDialog by remember { mutableStateOf(false) }

    BackHandler { showAbortDialog = true }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.screen) {
        if (uiState.screen == GameScreen.CRICKET_RESULT) onGameFinished()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            val isWide = maxWidth > 600.dp

            if (isWide) {
                // Tablet: side-by-side layout
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                    ) {
                        CricketStatusBar(
                            uiState = uiState,
                            onAbort = { showAbortDialog = true }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CricketScoreboard(
                            uiState = uiState,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CricketCurrentPlayerBar(uiState = uiState)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 8.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { viewModel.undoCricketDart() },
                                enabled = uiState.currentDarts.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Undo,
                                    contentDescription = "Undo",
                                    tint = if (uiState.currentDarts.isNotEmpty()) Amber else TextTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        CricketNumpad(
                            pendingMultiplier = uiState.pendingMultiplier,
                            onSetMultiplier = { viewModel.setMultiplier(it) },
                            onNumber = { viewModel.recordCricketDart(it) },
                            onMiss = { viewModel.recordCricketMiss() }
                        )
                    }
                }
            } else {
                // Phone: stacked layout
                Column(modifier = Modifier.fillMaxSize()) {
                    CricketStatusBar(
                        uiState = uiState,
                        onAbort = { showAbortDialog = true },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    CricketScoreboard(
                        uiState = uiState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    CricketCurrentPlayerBar(
                        uiState = uiState,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { viewModel.undoCricketDart() },
                            enabled = uiState.currentDarts.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (uiState.currentDarts.isNotEmpty()) Amber else TextTertiary
                            )
                        }
                    }
                    CricketNumpad(
                        pendingMultiplier = uiState.pendingMultiplier,
                        onSetMultiplier = { viewModel.setMultiplier(it) },
                        onNumber = { viewModel.recordCricketDart(it) },
                        onMiss = { viewModel.recordCricketMiss() },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAbortDialog) {
        AlertDialog(
            onDismissRequest = { showAbortDialog = false },
            title = { Text(stringResource(R.string.dialog_abort_game_title), color = TextPrimary) },
            text = { Text(stringResource(R.string.dialog_abort_game_message), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showAbortDialog = false
                    viewModel.abortCricketGame()
                }) {
                    Text(stringResource(R.string.btn_abort), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbortDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = Surface2
        )
    }
}

@Composable
private fun CricketStatusBar(
    uiState: GameUiState,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = uiState.config
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cricket",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Accent,
            modifier = Modifier.weight(1f)
        )
        if (config != null && config.legsToWin > 1) {
            Text(
                text = "Leg ${uiState.currentLegNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                fontFamily = DmMono
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        TextButton(
            onClick = onAbort,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(stringResource(R.string.btn_abort), color = TextTertiary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CricketCurrentPlayerBar(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    val currentPlayer = uiState.players.getOrNull(uiState.currentPlayerIndex) ?: return
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currentPlayer.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Dart slots
        repeat(3) { i ->
            val dart = uiState.currentDarts.getOrNull(i)
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 36.dp)
                    .background(Surface2, RoundedCornerShape(6.dp))
                    .border(1.dp, if (i == uiState.currentDarts.size) Accent else Border, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dart?.label() ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DmMono,
                    color = if (dart != null && dart.score in CRICKET_NUMBERS) TextPrimary else TextTertiary
                )
            }
            if (i < 2) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

// ---- Scoreboard ----

@Composable
private fun CricketScoreboard(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    val isTeams = uiState.isTeamGame

    if (isTeams) {
        CricketTeamsScoreboard(uiState = uiState, modifier = modifier)
    } else {
        CricketSingleScoreboard(uiState = uiState, modifier = modifier)
    }
}

@Composable
private fun CricketSingleScoreboard(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    val players = uiState.players
    val n = players.size
    // Split: left players = first floor(n/2), right players = remaining
    val leftPlayers = players.take(n / 2)
    val rightPlayers = players.drop(n / 2)
    val currentPlayer = players.getOrNull(uiState.currentPlayerIndex)

    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
        ) {
            // Left player columns
            leftPlayers.forEach { player ->
                PlayerColumn(
                    player = player,
                    isActive = player.id == currentPlayer?.id,
                    marks = uiState.cricketMarks[player.id] ?: emptyMap(),
                    score = uiState.cricketScores[player.id] ?: 0,
                    legWins = uiState.legWins[player.id] ?: 0,
                    isLeftSide = true,
                    modifier = Modifier.widthIn(min = 72.dp)
                )
            }

            // Center numbers column
            NumbersColumn()

            // Right player columns
            rightPlayers.forEach { player ->
                PlayerColumn(
                    player = player,
                    isActive = player.id == currentPlayer?.id,
                    marks = uiState.cricketMarks[player.id] ?: emptyMap(),
                    score = uiState.cricketScores[player.id] ?: 0,
                    legWins = uiState.legWins[player.id] ?: 0,
                    isLeftSide = false,
                    modifier = Modifier.widthIn(min = 72.dp)
                )
            }
        }
    }
}

@Composable
private fun CricketTeamsScoreboard(
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    val teamAPlayers = uiState.players.filter { uiState.teamAssignments[it.id] == 0 }
    val teamBPlayers = uiState.players.filter { uiState.teamAssignments[it.id] == 1 }
    val currentPlayer = uiState.players.getOrNull(uiState.currentPlayerIndex)
    val currentTeamIdx = currentPlayer?.let { uiState.teamAssignments[it.id] }

    Row(modifier = modifier.fillMaxWidth()) {
        // Team A column (left)
        TeamColumn(
            teamPlayers = teamAPlayers,
            teamColor = Red,
            isActive = currentTeamIdx == 0,
            marks = uiState.cricketMarks[0L] ?: emptyMap(),
            score = uiState.cricketScores[0L] ?: 0,
            legWins = uiState.teamLegWins[0] ?: 0,
            isLeftSide = true,
            modifier = Modifier.weight(1f)
        )

        // Center numbers column
        NumbersColumn()

        // Team B column (right)
        TeamColumn(
            teamPlayers = teamBPlayers,
            teamColor = Blue,
            isActive = currentTeamIdx == 1,
            marks = uiState.cricketMarks[1L] ?: emptyMap(),
            score = uiState.cricketScores[1L] ?: 0,
            legWins = uiState.teamLegWins[1] ?: 0,
            isLeftSide = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NumbersColumn() {
    Column {
        // Header cell
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(40.dp)
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text("#", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        HorizontalDivider(color = Border)

        // Number rows
        CRICKET_NUMBERS.forEach { number ->
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (number == 25) "Bull" else number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
            HorizontalDivider(color = Border)
        }

        // Points label row
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(40.dp)
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.cricket_points),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun PlayerColumn(
    player: Player,
    isActive: Boolean,
    marks: Map<Int, Int>,
    score: Int,
    legWins: Int,
    isLeftSide: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) Accent else Color.Transparent

    Column(
        modifier = modifier
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            )
    ) {
        // Player name header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (isActive) Accent.copy(alpha = 0.12f) else Surface2),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Accent else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (legWins > 0) {
                    Text(
                        text = "$legWins ★",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        fontSize = 10.sp
                    )
                }
            }
        }
        HorizontalDivider(color = Border)

        // Mark cells
        CRICKET_NUMBERS.forEach { number ->
            MarkCell(
                marks = marks[number] ?: 0,
                isLeftSide = isLeftSide,
                isActive = isActive
            )
            HorizontalDivider(color = Border)
        }

        // Score cell
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (isActive) Accent.copy(alpha = 0.08f) else Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DmMono,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Accent else TextPrimary
            )
        }
    }
}

@Composable
private fun TeamColumn(
    teamPlayers: List<Player>,
    teamColor: Color,
    isActive: Boolean,
    marks: Map<Int, Int>,
    score: Int,
    legWins: Int,
    isLeftSide: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) teamColor else Color.Transparent

    Column(
        modifier = modifier
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            )
    ) {
        // Team header with player names
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (isActive) teamColor.copy(alpha = 0.15f) else Surface2),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                teamPlayers.forEach { player ->
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) teamColor else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (legWins > 0) {
                    Text(
                        text = "$legWins ★",
                        style = MaterialTheme.typography.labelSmall,
                        color = teamColor,
                        fontSize = 10.sp
                    )
                }
            }
        }
        HorizontalDivider(color = Border)

        // Mark cells
        CRICKET_NUMBERS.forEach { number ->
            MarkCell(
                marks = marks[number] ?: 0,
                isLeftSide = isLeftSide,
                isActive = isActive,
                activeColor = teamColor
            )
            HorizontalDivider(color = Border)
        }

        // Score cell
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (isActive) teamColor.copy(alpha = 0.08f) else Surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = DmMono,
                fontWeight = FontWeight.Bold,
                color = if (isActive) teamColor else TextPrimary
            )
        }
    }
}

@Composable
private fun MarkCell(
    marks: Int,
    isLeftSide: Boolean,
    isActive: Boolean,
    activeColor: Color = Accent
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center
    ) {
        val symbol = when {
            marks <= 0 -> ""
            marks == 1 -> "/"
            marks == 2 -> "X"
            else -> "●"  // closed
        }
        val color = when {
            marks >= 3 -> activeColor
            marks > 0 -> TextSecondary
            else -> Color.Transparent
        }
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

// ---- Cricket Numpad ----

@Composable
private fun CricketNumpad(
    pendingMultiplier: Int,
    onSetMultiplier: (Int) -> Unit,
    onNumber: (Int) -> Unit,
    onMiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Row 1: 20, 19, 18, 17, 16
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(20, 19, 18, 17, 16).forEach { n ->
                CricketNumButton(
                    label = n.toString(),
                    onClick = { onNumber(n) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: 15, Bull, [empty], Miss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CricketNumButton(
                label = "15",
                onClick = { onNumber(15) },
                modifier = Modifier.weight(1f)
            )
            CricketNumButton(
                label = "Bull",
                onClick = { onNumber(25) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(2f))
            CricketNumButton(
                label = "Miss",
                onClick = onMiss,
                modifier = Modifier.weight(1f),
                isSpecial = true
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Row 3: D / T multiplier toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            MultiplierToggle(
                label = "D",
                isSelected = pendingMultiplier == 2,
                onClick = { onSetMultiplier(if (pendingMultiplier == 2) 1 else 2) },
                modifier = Modifier.weight(1f)
            )
            MultiplierToggle(
                label = "T",
                isSelected = pendingMultiplier == 3,
                onClick = { onSetMultiplier(if (pendingMultiplier == 3) 1 else 3) },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(2f))
        }
    }
}

@Composable
private fun CricketNumButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                color = if (isSpecial) Surface3 else Surface2,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSpecial) TextTertiary else TextPrimary
        )
    }
}

@Composable
private fun MultiplierToggle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                color = if (isSelected) Accent else Surface2,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Background else TextSecondary
        )
    }
}
