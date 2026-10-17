package com.snehonir.lipirx.presentation.scan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerState {
    return remember {
        object : ImagePickerState {
            override fun launchCamera() {
                // iOS Camera integration placeholder
            }

            override fun launchGallery() {
                // iOS Gallery integration placeholder
            }
        }
    }
}
