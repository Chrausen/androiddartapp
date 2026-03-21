package com.clubdarts.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
    val winner = uiState.players.firstOrNull { it.id == uiState.winnerId }
    val gameSaved = uiState.gameSaved

    // Back-press discards unsaved game and exits to setup
    BackHandler {
        viewModel.discardGame()
        onDone()
    }

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
                Text(
                    text = "Winner!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent
                )
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
                }
            }
        }

        // Action buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Save to history (shown until saved)
            if (!gameSaved) {
                Button(
                    onClick = { viewModel.saveGame() },
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
                onClick = {
                    viewModel.discardGame()
                    onNewGame()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface2),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("New game", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.discardGame()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = TextSecondary)
            }
        }
    }
}
