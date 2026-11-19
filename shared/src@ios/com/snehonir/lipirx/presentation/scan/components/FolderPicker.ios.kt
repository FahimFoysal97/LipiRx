package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFolderPickerLauncher(onFolderSelected: (folderPath: String) -> Unit): () -> Unit {
    return {
        // No-op on iOS
    }
}
