package com.snehonir.lipirx.domain.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val sha256DownloadUrl: String? = null,
    val apkFileName: String = "LipiRx-v$versionName.apk"
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val updateInfo: UpdateInfo) : UpdateState
    data class Downloading(
        val updateInfo: UpdateInfo,
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState
    data class NeedsInstallPermission(val updateInfo: UpdateInfo, val apkPath: String) : UpdateState
    data class ReadyToInstall(val updateInfo: UpdateInfo, val apkPath: String) : UpdateState
    data class Error(val message: String) : UpdateState
}
