package com.snehonir.lipirx.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.snehonir.lipirx.presentation.common.rememberImagePicker
import com.snehonir.lipirx.presentation.home.EmptyStateView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onOpenSettings: () -> Unit,
    onHistoryClick: () -> Unit,
    onImageCaptured: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePicker = rememberImagePicker { imageBytes ->
        viewModel.onImageSelected(imageBytes)
        onImageCaptured()
    }

    EmptyStateView(
        onOpenSettings = onOpenSettings,
        onTakePhoto = { imagePicker.launchCamera() },
        onChooseGallery = { imagePicker.launchGallery() },
        onHistoryClick = {
            viewModel.loadHistoryCount()
            onHistoryClick()
        }
    )
}
