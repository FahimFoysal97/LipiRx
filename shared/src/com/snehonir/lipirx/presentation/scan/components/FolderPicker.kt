package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFolderPickerLauncher(onFolderSelected: (folderPath: String) -> Unit): () -> Unit
