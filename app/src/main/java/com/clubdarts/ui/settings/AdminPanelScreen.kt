package com.clubdarts.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.BuildConfig
import com.clubdarts.R
import com.clubdarts.data.model.Player
import com.clubdarts.ui.theme.*

private const val ADMIN_PASSWORD = "supersecretpassword12"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    onDataDeleted: () -> Unit = {},
    viewModel: AdminPanelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var unlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showPasswordError by remember { mutableStateOf(false) }

    if (uiState.deleteSuccess) {
        LaunchedEffect(Unit) {
            onDataDeleted()
            viewModel.clearDeleteSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { padding ->
        if (!unlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Admin Access",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Enter the admin password to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            showPasswordError = false
                        },
                        label = { Text("Password", color = TextTertiary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = showPasswordError,
                        supportingText = if (showPasswordError) {
                            { Text("Incorrect password", color = Red) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border2,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            errorBorderColor = Red
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (passwordInput == ADMIN_PASSWORD) {
                                unlocked = true
                                passwordInput = ""
                                showPasswordError = false
                            } else {
                                showPasswordError = true
                                passwordInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("Unlock", color = Background, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "Player Rankings",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(uiState.players, key = { it.id }) { player ->
                    PlayerRankingRow(
                        player = player,
                        onSave = { elo, wins, losses ->
                            viewModel.savePlayer(player.id, elo, wins, losses)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.labelMedium,
                        color = Red,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(10.dp))
                            .clickable { viewModel.requestDeleteAll() }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Red,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_delete_all),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Red
                            )
                            Text(
                                stringResource(R.string.settings_delete_all_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (BuildConfig.DEBUG) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface, RoundedCornerShape(10.dp))
                                .clickable(
                                    enabled = !uiState.isGeneratingDebugData,
                                    onClick = { viewModel.generateDebugData() }
                                )
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isGeneratingDebugData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = Accent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Generate Debug Data",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (uiState.isGeneratingDebugData) TextTertiary else TextPrimary
                                )
                                Text(
                                    if (uiState.isGeneratingDebugData) "Creating 20 players and 500 games…"
                                    else "20 players · 500 games",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text(stringResource(R.string.dialog_delete_all_title), color = TextPrimary) },
            text = {
                Text(
                    stringResource(R.string.dialog_delete_all_message),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteAll() }) {
                    Text(stringResource(R.string.btn_delete), color = Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            },
            containerColor = Surface2
        )
    }
}

@Composable
private fun PlayerRankingRow(
    player: Player,
    onSave: (elo: Double, wins: Int, losses: Int) -> Unit
) {
    var eloText by remember(player.id) { mutableStateOf("%.0f".format(player.elo)) }
    var winsText by remember(player.id) { mutableStateOf(player.wins.toString()) }
    var lossesText by remember(player.id) { mutableStateOf(player.losses.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = player.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdminTextField(
                value = eloText,
                onValueChange = { eloText = it },
                label = "Elo",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            AdminTextField(
                value = winsText,
                onValueChange = { winsText = it },
                label = "Wins",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            AdminTextField(
                value = lossesText,
                onValueChange = { lossesText = it },
                label = "Losses",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val elo = eloText.toDoubleOrNull() ?: return@TextButton
                    val wins = winsText.toIntOrNull() ?: return@TextButton
                    val losses = lossesText.toIntOrNull() ?: return@TextButton
                    onSave(elo, wins, losses)
                }
            ) {
                Text("Save", color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary, style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Border2,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
        shape = RoundedCornerShape(8.dp)
    )
}
