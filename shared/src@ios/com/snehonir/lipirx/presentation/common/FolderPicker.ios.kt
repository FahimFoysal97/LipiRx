package com.snehonir.lipirx.presentation.common

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFolderPickerLauncher(onFolderSelected: (folderPath: String) -> Unit): () -> Unit {
    return {
        // No-op on iOS
    }
}
