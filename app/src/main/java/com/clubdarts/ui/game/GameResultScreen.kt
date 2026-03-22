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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*

@Composable
fun GameResultScreen(
    onNewGame: () -> Unit,
    onDone: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gameSaved = uiState.gameSaved

    // Back-press discards unsaved game and exits to setup
    BackHandler {
        viewModel.discardGame()
        onDone()
    }

    if (uiState.isTeamGame) {
        TeamResultScreen(
            uiState = uiState,
            gameSaved = gameSaved,
            onSave = { viewModel.saveGame() },
            onNewGame = {
                viewModel.discardGame()
                onNewGame()
            },
            onDone = {
                viewModel.discardGame()
                onDone()
            }
        )
    } else {
        SingleResultScreen(
            uiState = uiState,
            gameSaved = gameSaved,
            onSave = { viewModel.saveGame() },
            onNewGame = {
                viewModel.discardGame()
                onNewGame()
            },
            onDone = {
                viewModel.discardGame()
                onDone()
            }
        )
    }
}

@Composable
private fun SingleResultScreen(
    uiState: GameUiState,
    gameSaved: Boolean,
    onSave: () -> Unit,
    onNewGame: () -> Unit,
    onDone: () -> Unit
) {
    val winner = uiState.players.firstOrNull { it.id == uiState.winnerId }
    val isRanked = uiState.isRanked && uiState.eloResults != null

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
                    color = TextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Winner!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )
                    if (isRanked) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = Accent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Ranked",
                                style = MaterialTheme.typography.labelSmall,
                                color = Accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Final leg score
        item {
            val scoreStr = uiState.players.joinToString(" — ") { p ->
                (uiState.legWins[p.id] ?: 0).toString()
            }
            Text(
                text = scoreStr,
                fontSize = 28.sp,
                fontFamily = DmMono,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Per-player stat cards
        items(uiState.players) { player ->
            val isWinner = player.id == uiState.winnerId
            val eloChange = uiState.eloResults?.get(player.id)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWinner) AccentDim else Surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatar(name = player.name)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(player.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            if (isWinner) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = Green, shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        "Winner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Background,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Legs: ${uiState.legWins[player.id] ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    // Elo change chip for ranked games
                    if (eloChange != null) {
                        val isGain = eloChange >= 0
                        val chipColor = if (isGain) Green else Red
                        val sign = if (isGain) "+" else ""
                        Surface(
                            color = chipColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$sign${"%.0f".format(eloChange)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = chipColor
                                )
                                Text(
                                    text = "Elo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = chipColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { ResultActions(gameSaved, uiState.isRanked, onSave, onNewGame, onDone) }
    }
}

@Composable
private fun TeamResultScreen(
    uiState: GameUiState,
    gameSaved: Boolean,
    onSave: () -> Unit,
    onNewGame: () -> Unit,
    onDone: () -> Unit
) {
    val winTeamIdx = uiState.winningTeamIndex
    val winTeamColor = if (winTeamIdx == 0) Red else Blue
    val winTeamName = if (winTeamIdx == 0) "Team A" else "Team B"
    val teamAPlayers = uiState.players.filter { uiState.teamAssignments[it.id] == 0 }
    val teamBPlayers = uiState.players.filter { uiState.teamAssignments[it.id] == 1 }
    val winTeamPlayers = if (winTeamIdx == 0) teamAPlayers else teamBPlayers

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
            // Winning team badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(winTeamColor.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
                    .border(2.dp, winTeamColor, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (winTeamIdx == 0) "A" else "B",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = winTeamColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = winTeamName,
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )
            Text(
                text = "Wins!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = winTeamColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            // List winning team member names
            winTeamPlayers.forEach { player ->
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
        }

        // Team leg score
        item {
            Text(
                text = "Team A: ${uiState.teamLegWins[0] ?: 0}  —  Team B: ${uiState.teamLegWins[1] ?: 0}",
                fontSize = 24.sp,
                fontFamily = DmMono,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Team A player cards
        item {
            TeamPlayerCards(
                teamLabel = "Team A",
                teamColor = Red,
                players = teamAPlayers,
                legWins = uiState.teamLegWins[0] ?: 0,
                isWinningTeam = winTeamIdx == 0
            )
        }

        // Team B player cards
        item {
            TeamPlayerCards(
                teamLabel = "Team B",
                teamColor = Blue,
                players = teamBPlayers,
                legWins = uiState.teamLegWins[1] ?: 0,
                isWinningTeam = winTeamIdx == 1
            )
        }

        item { ResultActions(gameSaved, false, onSave, onNewGame, onDone) }
    }
}

@Composable
private fun TeamPlayerCards(
    teamLabel: String,
    teamColor: androidx.compose.ui.graphics.Color,
    players: List<com.clubdarts.data.model.Player>,
    legWins: Int,
    isWinningTeam: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinningTeam) teamColor.copy(alpha = 0.1f) else Surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(teamColor, RoundedCornerShape(5.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = teamLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = teamColor
                    )
                    if (isWinningTeam) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Green, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "Winners",
                                style = MaterialTheme.typography.labelSmall,
                                color = Background,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Legs: $legWins",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            players.forEach { player ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerAvatar(name = player.name, size = 28.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun ResultActions(
    gameSaved: Boolean,
    isRanked: Boolean,
    onSave: () -> Unit,
    onNewGame: () -> Unit,
    onDone: () -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))

    if (!gameSaved) {
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Save to history", fontWeight = FontWeight.Bold, color = Background)
        }
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        Text(
            text = "Saved to history",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Button(
        onClick = onNewGame,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface2),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text("New game", fontWeight = FontWeight.Bold, color = TextPrimary)
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text("Done", color = TextSecondary)
    }
}
