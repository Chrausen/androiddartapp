package com.clubdarts.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*

private val CRICKET_RESULT_NUMBERS = listOf(20, 19, 18, 17, 16, 15, 25)

@Composable
fun CricketResultScreen(
    onNewGame: () -> Unit,
    onDone: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gameSaved = uiState.gameSaved

    BackHandler {
        viewModel.discardCricketGame()
        onDone()
    }

    if (uiState.isTeamGame) {
        CricketTeamResultScreen(
            uiState = uiState,
            gameSaved = gameSaved,
            onSave = { viewModel.saveCricketGame() },
            onNewGame = { viewModel.repeatGame(); onNewGame() },
            onDone = { viewModel.discardCricketGame(); onDone() }
        )
    } else {
        CricketSingleResultScreen(
            uiState = uiState,
            gameSaved = gameSaved,
            onSave = { viewModel.saveCricketGame() },
            onNewGame = { viewModel.repeatGame(); onNewGame() },
            onDone = { viewModel.discardCricketGame(); onDone() }
        )
    }
}

@Composable
private fun CricketSingleResultScreen(
    uiState: GameUiState,
    gameSaved: Boolean,
    onSave: () -> Unit,
    onNewGame: () -> Unit,
    onDone: () -> Unit
) {
    val winner = uiState.players.firstOrNull { it.id == uiState.winnerId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            if (winner != null) {
                PlayerAvatar(name = winner.name, size = 72.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = winner.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.result_winner_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Leg score summary
        if (uiState.legWins.values.any { it > 0 }) {
            item {
                val legScoreText = uiState.players.joinToString(" – ") { p ->
                    "${uiState.legWins[p.id] ?: 0}"
                }
                Text(
                    text = legScoreText,
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = DmMono,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Cricket mode badge
        item {
            Surface(color = Accent.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "Cricket",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Per-player result cards
        items(uiState.players) { player ->
            val isWinner = player.id == uiState.winnerId
            val playerMarks = uiState.cricketMarks[player.id] ?: emptyMap()
            val closedCount = CRICKET_RESULT_NUMBERS.count { (playerMarks[it] ?: 0) >= 3 }
            val points = uiState.cricketScores[player.id] ?: 0

            CricketPlayerResultCard(
                player = player,
                isWinner = isWinner,
                closedNumbers = closedCount,
                points = points,
                legWins = uiState.legWins[player.id] ?: 0
            )
        }

        // Action buttons
        item {
            CricketResultActions(
                gameSaved = gameSaved,
                onSave = onSave,
                onNewGame = onNewGame,
                onDone = onDone
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CricketTeamResultScreen(
    uiState: GameUiState,
    gameSaved: Boolean,
    onSave: () -> Unit,
    onNewGame: () -> Unit,
    onDone: () -> Unit
) {
    val winTeamIdx = uiState.winningTeamIndex
    val teamAPlayers = uiState.players.filter { uiState.teamAssignments[it.id] == 0 }
    val teamBPlayers = uiState.players.filter { uiState.teamAssignments[it.id] == 1 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            val winColor = if (winTeamIdx == 0) Red else Blue
            val winName = if (winTeamIdx == 0) stringResource(R.string.result_team_a) else stringResource(R.string.result_team_b)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(winColor.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
                    .border(2.dp, winColor, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(winName.first().toString(), style = MaterialTheme.typography.headlineLarge, color = winColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = winName,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.result_winner_label),
                style = MaterialTheme.typography.bodyMedium,
                color = winColor,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Leg score
        item {
            val legScoreText = "${uiState.teamLegWins[0] ?: 0} – ${uiState.teamLegWins[1] ?: 0}"
            Text(
                text = legScoreText,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = DmMono,
                color = TextPrimary
            )
        }

        // Cricket badge
        item {
            Surface(color = Accent.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "Cricket",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Team A card
        item {
            val teamMarks = uiState.cricketMarks[0L] ?: emptyMap()
            val closedCount = CRICKET_RESULT_NUMBERS.count { (teamMarks[it] ?: 0) >= 3 }
            val points = uiState.cricketScores[0L] ?: 0
            CricketTeamResultCard(
                label = stringResource(R.string.result_team_a),
                players = teamAPlayers,
                teamColor = Red,
                isWinner = winTeamIdx == 0,
                closedNumbers = closedCount,
                points = points,
                legWins = uiState.teamLegWins[0] ?: 0
            )
        }

        // Team B card
        item {
            val teamMarks = uiState.cricketMarks[1L] ?: emptyMap()
            val closedCount = CRICKET_RESULT_NUMBERS.count { (teamMarks[it] ?: 0) >= 3 }
            val points = uiState.cricketScores[1L] ?: 0
            CricketTeamResultCard(
                label = stringResource(R.string.result_team_b),
                players = teamBPlayers,
                teamColor = Blue,
                isWinner = winTeamIdx == 1,
                closedNumbers = closedCount,
                points = points,
                legWins = uiState.teamLegWins[1] ?: 0
            )
        }

        item {
            CricketResultActions(
                gameSaved = gameSaved,
                onSave = onSave,
                onNewGame = onNewGame,
                onDone = onDone
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CricketPlayerResultCard(
    player: Player,
    isWinner: Boolean,
    closedNumbers: Int,
    points: Int,
    legWins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) AccentDim else Surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        val borderModifier = if (isWinner) Modifier.border(1.dp, Accent, RoundedCornerShape(12.dp)) else Modifier
        Row(
            modifier = borderModifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(name = player.name, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Closed: $closedNumbers/7",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$points pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) Accent else TextPrimary
                )
                if (legWins > 0) {
                    Text("$legWins ★", style = MaterialTheme.typography.labelSmall, color = Accent, fontSize = 10.sp)
                }
            }
            if (isWinner) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("W", style = MaterialTheme.typography.labelSmall, color = Green, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CricketTeamResultCard(
    label: String,
    players: List<Player>,
    teamColor: Color,
    isWinner: Boolean,
    closedNumbers: Int,
    points: Int,
    legWins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = teamColor.copy(alpha = if (isWinner) 0.15f else 0.06f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        val borderModifier = if (isWinner) Modifier.border(1.dp, teamColor, RoundedCornerShape(12.dp)) else Modifier
        Column(
            modifier = borderModifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(teamColor, RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, color = teamColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$points pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = DmMono,
                    fontWeight = FontWeight.Bold,
                    color = if (isWinner) teamColor else TextPrimary
                )
                if (legWins > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$legWins ★", style = MaterialTheme.typography.labelSmall, color = teamColor, fontSize = 10.sp)
                }
                if (isWinner) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("W", style = MaterialTheme.typography.labelSmall, color = teamColor, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "Closed: $closedNumbers/7",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.padding(start = 18.dp, top = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            players.forEach { player ->
                Row(modifier = Modifier.padding(start = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(name = player.name, size = 24.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(player.name, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CricketResultActions(
    gameSaved: Boolean,
    onSave: () -> Unit,
    onNewGame: () -> Unit,
    onDone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!gameSaved) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Background
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.result_save), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNewGame,
                modifier = Modifier.weight(1f).height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border2),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.result_new_game), color = TextSecondary, style = MaterialTheme.typography.titleSmall)
            }
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.weight(1f).height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border2),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.btn_done), color = TextSecondary, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
