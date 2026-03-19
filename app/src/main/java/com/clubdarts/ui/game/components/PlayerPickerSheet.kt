package com.clubdarts.ui.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clubdarts.data.model.Player
import com.clubdarts.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerPickerSheet(
    allPlayers: List<Player>,
    recentPlayers: List<Player>,
    selectedPlayerIds: Set<Long>,
    onPlayerSelected: (Player) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add player",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search players…", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border2,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            val filteredAll = if (searchQuery.isBlank()) allPlayers
            else allPlayers.filter { it.name.contains(searchQuery, ignoreCase = true) }

            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                if (searchQuery.isBlank() && recentPlayers.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(recentPlayers) { player ->
                        PlayerPickerRow(
                            player = player,
                            isInGame = player.id in selectedPlayerIds,
                            onAdd = { onPlayerSelected(player) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All players",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                items(filteredAll) { player ->
                    PlayerPickerRow(
                        player = player,
                        isInGame = player.id in selectedPlayerIds,
                        onAdd = { onPlayerSelected(player) }
                    )
                }

                if (filteredAll.isEmpty()) {
                    item {
                        Text(
                            text = "No players found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PlayerPickerRow(
    player: Player,
    isInGame: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(
                name = player.name,
                size = 36.dp,
                alpha = if (isInGame) 0.4f else 1f
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isInGame) TextTertiary else TextPrimary
                )
                if (isInGame) {
                    Text(
                        text = "In game",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }

        if (isInGame) {
            Text(
                text = "Added",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        } else {
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Accent
                )
            }
        }
    }
}

@Composable
fun PlayerAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    alpha: Float = 1f
) {
    val colors = listOf(
        Blue, Green, Amber, Accent, Red,
        androidx.compose.ui.graphics.Color(0xFF9C6FFF),
        androidx.compose.ui.graphics.Color(0xFF00D4AA)
    )
    val colorIndex = name.hashCode().let { if (it < 0) -it else it } % colors.size
    val color = colors[colorIndex].copy(alpha = alpha)
    val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Background
        )
    }
}
