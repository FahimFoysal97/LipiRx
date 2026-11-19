package com.snehonir.lipirx.presentation.scan.components

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aakira.napier.Napier

@Composable
actual fun rememberFolderPickerLauncher(onFolderSelected: (folderPath: String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolvedPath = parseTreeUriToPath(uri)
            Napier.d("Selected folder Uri: $uri -> Resolved path: '$resolvedPath'", tag = "FolderPicker")
            if (resolvedPath.isNotBlank()) {
                onFolderSelected(resolvedPath)
            }
        }
    }

    return remember(launcher) {
        { launcher.launch(null) }
    }
}

private fun parseTreeUriToPath(uri: Uri): String {
    val docId = try {
        DocumentsContract.getTreeDocumentId(uri)
    } catch (e: Exception) {
        uri.path ?: ""
    }

    return when {
        docId.startsWith("primary:") -> {
            val relativePath = docId.substringAfter("primary:")
            @Suppress("DEPRECATION")
            val storageBase = Environment.getExternalStorageDirectory().absolutePath
            if (relativePath.isNotBlank()) "$storageBase/$relativePath" else storageBase
        }
        docId.contains(":") -> {
            val parts = docId.split(":")
            val volume = parts[0]
            val relativePath = parts.getOrNull(1) ?: ""
            if (volume.isNotBlank()) "/storage/$volume/$relativePath".removeSuffix("/") else docId
        }
        else -> docId
    }
}
