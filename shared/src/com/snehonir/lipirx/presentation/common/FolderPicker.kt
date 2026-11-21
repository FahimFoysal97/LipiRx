package com.snehonir.lipirx.presentation.common

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFolderPickerLauncher(onFolderSelected: (folderPath: String) -> Unit): () -> Unit
