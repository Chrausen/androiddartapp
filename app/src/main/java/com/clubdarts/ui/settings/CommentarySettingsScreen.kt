package com.clubdarts.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.clubdarts.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentarySettingsScreen(
    onBack: () -> Unit,
    viewModel: CommentarySettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.commentary_title), color = TextPrimary) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item {
                Text(
                    text = stringResource(R.string.commentary_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            item {
                TierSection(
                    title = stringResource(R.string.commentary_tier_bad),
                    phrases = uiState.bad,
                    accentColor = Red,
                    onAdd = { viewModel.showAddDialog(CommentaryTier.BAD) },
                    onDelete = { i -> viewModel.deletePhrase(CommentaryTier.BAD, i) }
                )
            }
            item {
                TierSection(
                    title = stringResource(R.string.commentary_tier_normal),
                    phrases = uiState.normal,
                    accentColor = Accent,
                    onAdd = { viewModel.showAddDialog(CommentaryTier.NORMAL) },
                    onDelete = { i -> viewModel.deletePhrase(CommentaryTier.NORMAL, i) }
                )
            }
            item {
                TierSection(
                    title = stringResource(R.string.commentary_tier_good),
                    phrases = uiState.good,
                    accentColor = Green,
                    onAdd = { viewModel.showAddDialog(CommentaryTier.GOOD) },
                    onDelete = { i -> viewModel.deletePhrase(CommentaryTier.GOOD, i) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (uiState.addDialogTier != null) {
        AddPhraseDialog(
            text = uiState.addDialogText,
            onTextChange = { viewModel.updateDialogText(it) },
            onConfirm = { viewModel.confirmAdd() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}

@Composable
private fun TierSection(
    title: String,
    phrases: List<String>,
    accentColor: androidx.compose.ui.graphics.Color,
    onAdd: () -> Unit,
    onDelete: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.commentary_add_phrase), tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (phrases.isEmpty()) {
            Text(
                text = stringResource(R.string.commentary_empty),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(10.dp))
            ) {
                phrases.forEachIndexed { index, phrase ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = phrase,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(index) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.btn_delete),
                                tint = Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < phrases.lastIndex) {
                        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddPhraseDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.commentary_add_phrase), color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text(stringResource(R.string.commentary_phrase_hint), color = TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border2,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
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
