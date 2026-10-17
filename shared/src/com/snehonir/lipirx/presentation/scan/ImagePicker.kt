package com.snehonir.lipirx.presentation.scan

import androidx.compose.runtime.Composable

interface ImagePickerState {
    fun launchCamera()
    fun launchGallery()
}

@Composable
expect fun rememberImagePicker(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerState
