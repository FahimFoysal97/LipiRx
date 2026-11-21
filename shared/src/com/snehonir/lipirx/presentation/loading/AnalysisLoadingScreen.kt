package com.snehonir.lipirx.presentation.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.snehonir.lipirx.presentation.loading.LoadingView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisLoadingScreen(
    viewModel: AnalysisLoadingViewModel = koinViewModel(),
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LoadingView(
        imageByteArray = uiState.imageByteArray,
        activeStep = uiState.loadingStep,
        statusText = uiState.statusText,
        agentProgressPercent = uiState.progressPercent,
        onStopAnalysis = {
            viewModel.stopAnalysis()
            onCancel()
        }
    )
}
