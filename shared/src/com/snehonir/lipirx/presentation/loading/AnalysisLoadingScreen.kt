package com.snehonir.lipirx.presentation.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.snehonir.lipirx.domain.agent.PipelineProgress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AnalysisLoadingScreen(
    viewModel: AnalysisLoadingViewModel = koinViewModel(),
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LoadingView(
        imageByteArray = uiState.imageByteArray,
        progress = uiState.progress,
        onStopAnalysis = {
            viewModel.stopAnalysis()
            onCancel()
        }
    )
}
