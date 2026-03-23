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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.ui.game.components.PlayerAvatar
import com.clubdarts.ui.theme.*

@Composable
fun PlayersScreen(viewModel: PlayersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.players_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.players.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.players_empty), style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
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
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.players_add_title))
        }
    }

    // Dialogs
    when (val dialog = uiState.dialogState) {
        is PlayerDialogState.Add -> {
            PlayerDialog(
                title = stringResource(R.string.players_add_title),
                name = dialog.name,
                error = dialog.error,
                onNameChange = { viewModel.updateDialogName(it) },
                onConfirm = { viewModel.savePlayer() },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is PlayerDialogState.Edit -> {
            PlayerDialog(
                title = stringResource(R.string.players_edit_title),
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
                title = { Text(stringResource(R.string.players_delete_title), color = TextPrimary) },
                text = { Text(stringResource(R.string.players_delete_message, dialog.player.name), color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text(stringResource(R.string.btn_delete), color = Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(R.string.btn_cancel), color = TextSecondary)
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
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.players_edit_title), tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.btn_delete), tint = Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
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
                    label = { Text(stringResource(R.string.player_name_label), color = TextTertiary) },
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
                Text(stringResource(R.string.btn_save), color = Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = TextSecondary)
            }
        },
        containerColor = Surface2
    )
}
