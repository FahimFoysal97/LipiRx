package com.snehonir.lipirx.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.snehonir.lipirx.presentation.common.rememberImagePicker
import com.snehonir.lipirx.presentation.preview.ImagePreviewAndProcessView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImagePreviewScreen(
    viewModel: ImagePreviewViewModel = koinViewModel(),
    onOpenSettings: () -> Unit,
    onZoom: () -> Unit,
    onStartAnalysis: () -> Unit,
    onClear: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePicker = rememberImagePicker { imageBytes ->
        viewModel.onImageSelected(imageBytes)
    }

    val bytes = uiState.imageByteArray
    if (bytes != null) {
        ImagePreviewAndProcessView(
            imageByteArray = bytes,
            errorMessage = uiState.errorMessage,
            agentConfigs = uiState.agentConfigs,
            onOpenSettings = onOpenSettings,
            onZoom = onZoom,
            onAnalyze = {
                if (viewModel.processSelectedImage()) {
                    onStartAnalysis()
                }
            },
            onRetake = { imagePicker.launchCamera() },
            onChange = { imagePicker.launchGallery() },
            onClear = {
                viewModel.clearSelectedImage()
                onClear()
            }
        )
    }
}
