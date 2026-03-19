package com.clubdarts.ui.players

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*

@Composable
fun PlayersScreen(viewModel: PlayersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Players", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No players yet. Add one!", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.players) { player ->
                        PlayerRow(
                            player = player,
                            onEdit = { viewModel.showEditDialog(player) },
                            onDelete = { viewModel.showDeleteConfirmDialog(player) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showAddDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Accent,
            contentColor = Background
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add player")
        }
    }

    // Dialogs
    when (val dialog = uiState.dialogState) {
        is PlayerDialogState.Add -> {
            PlayerDialog(
                title = "Add player",
                name = dialog.name,
                error = dialog.error,
                onNameChange = { viewModel.updateDialogName(it) },
                onConfirm = { viewModel.savePlayer() },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is PlayerDialogState.Edit -> {
            PlayerDialog(
                title = "Edit player",
                name = dialog.name,
                error = dialog.error,
                onNameChange = { viewModel.updateDialogName(it) },
                onConfirm = { viewModel.savePlayer() },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is PlayerDialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete player", color = TextPrimary) },
                text = { Text("Delete ${dialog.player.name}? Their game history will be preserved.", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("Delete", color = Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = Surface2
            )
        }
        null -> {}
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAvatar(name = player.name)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(player.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlayerDialog(
    title: String,
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Player name", color = TextTertiary) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = Red) } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border2,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Save", color = Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = Surface2
    )
}
