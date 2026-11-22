package com.snehonir.lipirx.data.local

import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import java.io.File

actual fun getPlatformEpochMillis(): Long = System.currentTimeMillis()

actual fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a", java.util.Locale.US)
    return formatter.format(java.util.Date(epochMillis))
}

actual fun scanInstalledOnDeviceModels(customDirectoryPath: String?): List<ModelInfoDto> {
    val models = mutableListOf<ModelInfoDto>()
    val pathsToSearch = mutableListOf<File>()

    if (!customDirectoryPath.isNullOrBlank()) {
        val customDir = File(customDirectoryPath)
        if (customDir.exists() && customDir.isDirectory) {
            pathsToSearch.add(customDir)
        }
    }

    val externalDir = File("/sdcard/Download")
    if (externalDir.exists() && externalDir.isDirectory) {
        pathsToSearch.add(externalDir)
    }

    val extensions = listOf(".litertlm", ".tflite", ".bin", ".gguf")

    pathsToSearch.forEach { dir ->
        dir.listFiles()?.forEach { file ->
            if (file.isFile && extensions.any { file.name.endsWith(it, ignoreCase = true) }) {
                models.add(
                    ModelInfoDto(
                        name = file.name,
                        displayName = file.name
                    )
                )
            }
        }
    }

    return models.distinctBy { it.name }
}
