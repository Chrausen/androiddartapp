package com.clubdarts.ui.settings.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.onExportFileCreated(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImportFileSelected(it) }
    }

    var showImportConfirm by remember { mutableStateOf(false) }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            containerColor = Surface,
            title = {
                Text(
                    stringResource(R.string.backup_import_confirm_title),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    stringResource(R.string.backup_import_warning),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }) {
                    Text(stringResource(R.string.backup_import_confirm_btn), color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text(stringResource(R.string.btn_cancel), color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_backup_title),
                        color = TextPrimary
                    )
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            if (uiState is BackupUiState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Accent,
                    trackColor = Surface
                )
            }

            // Export section
            BackupCard(
                icon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp)) },
                title = stringResource(R.string.backup_export_title),
                subtitle = stringResource(R.string.backup_export_subtitle),
                buttonLabel = stringResource(R.string.backup_export_btn),
                enabled = uiState !is BackupUiState.Loading,
                onClick = {
                    val ts = System.currentTimeMillis()
                    exportLauncher.launch("clubdarts-backup-$ts.json")
                }
            )

            // Import section
            BackupCard(
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp)) },
                title = stringResource(R.string.backup_import_title),
                subtitle = stringResource(R.string.backup_import_subtitle),
                buttonLabel = stringResource(R.string.backup_import_btn),
                enabled = uiState !is BackupUiState.Loading,
                onClick = { showImportConfirm = true },
                warning = stringResource(R.string.backup_import_warning)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Result snackbar
    when (val state = uiState) {
        is BackupUiState.Success -> {
            val msg = when (state.message) {
                "export" -> stringResource(R.string.backup_export_success)
                "import" -> stringResource(R.string.backup_import_success)
                else -> state.message
            }
            ResultSnackbar(message = msg, isError = false, onDismiss = { viewModel.dismissResult() })
        }
        is BackupUiState.Error -> {
            val msg = when (state.message) {
                "export" -> stringResource(R.string.backup_error_write)
                "import" -> stringResource(R.string.backup_error_read)
                "parse" -> stringResource(R.string.backup_error_parse)
                else -> state.message
            }
            ResultSnackbar(message = msg, isError = true, onDismiss = { viewModel.dismissResult() })
        }
        else -> {}
    }
}

@Composable
private fun BackupCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    warning: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        if (warning != null) {
            Text(
                warning,
                style = MaterialTheme.typography.bodySmall,
                color = Red
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(buttonLabel, color = Background)
        }
    }
}

@Composable
private fun ResultSnackbar(message: String, isError: Boolean, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = if (isError) Red else Accent,
            contentColor = Background
        ) {
            Text(message)
        }
    }
}
