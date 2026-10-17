package com.snehonir.lipirx.presentation.scan

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.aakira.napier.Napier
import java.io.ByteArrayOutputStream

private const val TAG = "ImagePickerAndroid"

@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray) -> Unit
): ImagePickerState {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Napier.d("[CameraLauncher] Native camera bitmap captured: ${bitmap.width}x${bitmap.height}", tag = TAG)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val bytes = stream.toByteArray()
            Napier.d("[CameraLauncher] Compressed bitmap to JPEG: ${bytes.size} bytes. Invoking callback...", tag = TAG)
            onImagePicked(bytes)
        } else {
            Napier.w("[CameraLauncher -> Warning] Camera capture returned null bitmap", tag = TAG)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Napier.d("[GalleryLauncher] Selected image URI: $uri", tag = TAG)
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            }
            if (bytes != null) {
                Napier.d("[GalleryLauncher] Read ${bytes.size} bytes from gallery URI. Invoking callback...", tag = TAG)
                onImagePicked(bytes)
            } else {
                Napier.e("[GalleryLauncher -> Error] Failed to read bytes from URI input stream", tag = TAG)
            }
        } else {
            Napier.w("[GalleryLauncher -> Warning] Gallery selection returned null URI", tag = TAG)
        }
    }

    return remember(cameraLauncher, galleryLauncher) {
        object : ImagePickerState {
            override fun launchCamera() {
                Napier.d("[launchCamera] Launching native Android camera preview...", tag = TAG)
                cameraLauncher.launch()
            }

            override fun launchGallery() {
                Napier.d("[launchGallery] Launching native Android photo picker for image/*...", tag = TAG)
                galleryLauncher.launch("image/*")
            }
        }
    }
}
