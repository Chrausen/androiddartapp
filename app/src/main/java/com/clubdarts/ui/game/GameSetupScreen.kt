package com.clubdarts.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.Player
import com.clubdarts.data.repository.GameConfig
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.game.components.PlayerPickerSheet
import com.clubdarts.ui.players.PlayersViewModel
import com.clubdarts.ui.theme.*

@Composable
fun GameSetupScreen(
    onStartGame: () -> Unit,
    gameViewModel: GameViewModel = hiltViewModel(),
    playersViewModel: PlayersViewModel = hiltViewModel()
) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val playersState by playersViewModel.uiState.collectAsStateWithLifecycle()

    var startScore by remember(uiState.setupDefaults.startScore) {
        mutableIntStateOf(uiState.setupDefaults.startScore)
    }
    var checkoutRule by remember(uiState.setupDefaults.checkoutRule) {
        mutableStateOf(uiState.setupDefaults.checkoutRule)
    }
    var legsToWin by remember(uiState.setupDefaults.legsToWin) {
        mutableIntStateOf(uiState.setupDefaults.legsToWin)
    }
    var randomOrder by remember(uiState.setupDefaults.randomOrder) {
        mutableStateOf(uiState.setupDefaults.randomOrder)
    }
    var selectedPlayers by remember { mutableStateOf<List<Player>>(emptyList()) }
    var showPlayerPicker by remember { mutableStateOf(false) }

    // Load recent players into picker
    val recentPlayers = remember(uiState.setupDefaults.recentPlayerIds, playersState.players) {
        val ids = uiState.setupDefaults.recentPlayerIds
        val map = playersState.players.associateBy { it.id }
        ids.mapNotNull { map[it] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "New game",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            // Starting score
            item {
                SectionLabel("Starting score")
                SegmentedRow(
                    options = listOf(201, 301, 401, 501, 701),
                    selected = startScore,
                    onSelect = { startScore = it },
                    label = { it.toString() }
                )
            }

            // Checkout rule
            item {
                SectionLabel("Checkout rule")
                SegmentedRow(
                    options = CheckoutRule.values().toList(),
                    selected = checkoutRule,
                    onSelect = { checkoutRule = it },
                    label = { it.name.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }
                )
            }

            // Legs
            item {
                SectionLabel("Legs to win")
                SegmentedRow(
                    options = listOf(1, 3, 5, 7, 9),
                    selected = legsToWin,
                    onSelect = { legsToWin = it },
                    label = { it.toString() }
                )
            }

            // Players section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Players ", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        Text(
                            "${selectedPlayers.size} selected",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Random order", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Checkbox(
                            checked = randomOrder,
                            onCheckedChange = { randomOrder = it },
                            colors = CheckboxDefaults.colors(checkedColor = Accent)
                        )
                    }
                }
            }

            // Selected players list
            itemsIndexed(selectedPlayers) { index, player ->
                SelectedPlayerRow(
                    player = player,
                    index = index,
                    isFirst = index == 0,
                    randomOrder = randomOrder,
                    onRemove = { selectedPlayers = selectedPlayers.filter { it.id != player.id } }
                )
            }

            // Add player button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Border2, RoundedCornerShape(10.dp))
                        .clickable { showPlayerPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add player", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        // Start button — sticky at bottom, always visible
        Button(
            onClick = {
                val orderedPlayers = if (randomOrder) selectedPlayers.shuffled() else selectedPlayers
                val config = GameConfig(
                    startScore = startScore,
                    checkoutRule = checkoutRule,
                    legsToWin = legsToWin,
                    isSolo = orderedPlayers.size == 1,
                    playerIds = orderedPlayers.map { it.id }
                )
                gameViewModel.startGame(config)
                onStartGame()
            },
            enabled = selectedPlayers.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                disabledContainerColor = Surface3,
                contentColor = Background,
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Start game",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showPlayerPicker) {
        PlayerPickerSheet(
            allPlayers = playersState.players,
            recentPlayers = recentPlayers,
            selectedPlayerIds = selectedPlayers.map { it.id }.toSet(),
            onPlayerSelected = { player ->
                if (player.id !in selectedPlayers.map { it.id }) {
                    selectedPlayers = selectedPlayers + player
                }
            },
            onDismiss = { showPlayerPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun <T> SegmentedRow(
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
        options.forEachIndexed { i, option ->
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

@Composable
private fun SelectedPlayerRow(
    player: Player,
    index: Int,
    isFirst: Boolean,
    randomOrder: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isFirst) AccentDim else Surface2,
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (isFirst) Modifier.border(1.dp, Accent, RoundedCornerShape(10.dp)) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!randomOrder) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        PlayerAvatar(name = player.name, size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(
                text = if (randomOrder) "Order randomised on start" else "Throws ${index + 1}${ordinalSuffix(index + 1)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun ordinalSuffix(n: Int): String = when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}
