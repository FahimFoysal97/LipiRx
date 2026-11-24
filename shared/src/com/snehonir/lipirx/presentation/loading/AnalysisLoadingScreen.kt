package com.snehonir.lipirx.presentation.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisLoadingScreen(
    viewModel: AnalysisLoadingViewModel = koinViewModel(),
    onCancel: () -> Unit,
    onViewResults: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LoadingView(
        imageByteArray = uiState.imageByteArray,
        progress = uiState.progress,
        isLoading = uiState.isLoading,
        onStopAnalysis = {
            viewModel.stopAnalysis()
            onCancel()
        },
        onViewResults = onViewResults
    )
}
