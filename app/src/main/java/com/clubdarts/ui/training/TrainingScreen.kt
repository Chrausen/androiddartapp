package com.clubdarts.ui.training

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TrainingScreen(
    onOpenHeatmap: () -> Unit,
    viewModel: TrainingViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    when (uiState.screen) {
        TrainingScreenState.SETUP -> TrainingSetupScreen(
            uiState           = uiState,
            onSelectPlayer    = viewModel::selectPlayer,
            onSelectMode      = viewModel::selectMode,
            onSelectDifficulty = viewModel::selectDifficulty,
            onStart           = viewModel::startSession,
            onOpenHeatmap     = onOpenHeatmap
        )
        TrainingScreenState.LIVE -> TrainingLiveScreen(
            uiState           = uiState,
            onRecordDart      = viewModel::recordDart,
            onRecordScoringDart = viewModel::recordScoringDart,
            onUndo            = viewModel::undoLastDart,
            onAbort           = viewModel::backToSetup
        )
        TrainingScreenState.DONE -> TrainingDoneScreen(
            uiState   = uiState,
            onRepeat  = viewModel::repeatSession,
            onBack    = viewModel::backToSetup
        )
    }
}
