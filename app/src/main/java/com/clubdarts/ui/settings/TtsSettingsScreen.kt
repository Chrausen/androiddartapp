package com.clubdarts.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clubdarts.R
import com.clubdarts.data.model.TtsPhrase
import com.clubdarts.data.model.TtsScoreSetting
import com.clubdarts.ui.theme.*

@Composable
fun TtsSettingsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: TtsSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    stringResource(R.string.tts_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.tts_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.scoreSettings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.tts_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(uiState.scoreSettings) { _, setting ->
                        ScoreCard(
                            setting = setting,
                            onEdit = { viewModel.showEditDialog(setting) },
                            onDelete = { viewModel.deleteSetting(setting.score) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.showAddDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Accent,
            contentColor = Background
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.tts_add_score))
        }
    }

    when (val dialog = uiState.dialogState) {
        is TtsDialogState.Add -> ScoreDialog(
            title = stringResource(R.string.tts_add_title),
            score = dialog.score,
            phrases = dialog.phrases,
            scoreError = dialog.scoreError,
            onScoreChange = { viewModel.updateDialogScore(it) },
            onPhraseChange = { index, before, after -> viewModel.updatePhrase(index, before, after) },
            onAddPhrase = { viewModel.addPhrase() },
            onRemovePhrase = { viewModel.removePhrase(it) },
            onConfirm = { viewModel.saveSetting() },
            onDismiss = { viewModel.dismissDialog() }
        )
        is TtsDialogState.Edit -> ScoreDialog(
            title = stringResource(R.string.tts_edit_title),
            score = dialog.score,
            phrases = dialog.phrases,
            scoreError = dialog.scoreError,
            onScoreChange = { viewModel.updateDialogScore(it) },
            onPhraseChange = { index, before, after -> viewModel.updatePhrase(index, before, after) },
            onAddPhrase = { viewModel.addPhrase() },
            onRemovePhrase = { viewModel.removePhrase(it) },
            onConfirm = { viewModel.saveSetting() },
            onDismiss = { viewModel.dismissDialog() }
        )
        null -> {}
    }
}

@Composable
private fun ScoreCard(
    setting: TtsScoreSetting,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.score.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = Accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            setting.phrases.forEach { phrase ->
                PhrasePreviewRow(phrase = phrase, score = setting.score)
            }
            if (setting.phrases.isEmpty()) {
                Text(
                    stringResource(R.string.tts_no_phrases),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.players_edit_title),
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.btn_delete),
                tint = Red.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PhrasePreviewRow(phrase: TtsPhrase, score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("· ", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        if (phrase.before.isNotBlank()) {
            Text(
                phrase.before.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(" · ", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Text(
            score.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = Accent,
            fontWeight = FontWeight.Medium
        )
        if (phrase.after.isNotBlank()) {
            Text(" · ", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            Text(
                phrase.after.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ScoreDialog(
    title: String,
    score: String,
    phrases: List<TtsPhrase>,
    scoreError: String?,
    onScoreChange: (String) -> Unit,
    onPhraseChange: (Int, String, String) -> Unit,
    onAddPhrase: () -> Unit,
    onRemovePhrase: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = score,
                    onValueChange = onScoreChange,
                    label = { Text(stringResource(R.string.tts_score_label), color = TextTertiary) },
                    isError = scoreError != null,
                    supportingText = scoreError?.let { { Text(it, color = Red) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border2,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.tts_phrases_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                phrases.forEachIndexed { index, phrase ->
                    PhraseInputRow(
                        phrase = phrase,
                        scoreLabel = score.ifBlank { "?" },
                        canRemove = phrases.size > 1,
                        onPhraseChange = { before, after -> onPhraseChange(index, before, after) },
                        onRemove = { onRemovePhrase(index) }
                    )
                    if (index < phrases.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onAddPhrase,
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.tts_add_phrase), color = Accent, style = MaterialTheme.typography.labelMedium)
                }
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

@Composable
private fun PhraseInputRow(
    phrase: TtsPhrase,
    scoreLabel: String,
    canRemove: Boolean,
    onPhraseChange: (before: String, after: String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = phrase.before,
            onValueChange = { onPhraseChange(it, phrase.after) },
            placeholder = { Text(stringResource(R.string.tts_before_placeholder), color = TextTertiary, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border2,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            textStyle = MaterialTheme.typography.bodySmall
        )
        Text(
            " $scoreLabel ",
            style = MaterialTheme.typography.labelMedium,
            color = Accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        OutlinedTextField(
            value = phrase.after,
            onValueChange = { onPhraseChange(phrase.before, it) },
            placeholder = { Text(stringResource(R.string.tts_after_placeholder), color = TextTertiary, style = MaterialTheme.typography.bodySmall) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border2,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            textStyle = MaterialTheme.typography.bodySmall
        )
        if (canRemove) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.tts_remove_phrase),
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(36.dp))
        }
    }
}
