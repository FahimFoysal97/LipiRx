package com.snehonir.lipirx.domain.update

interface PlatformUpdater {
    fun getAppVersionName(): String
    suspend fun downloadApk(
        downloadUrl: String,
        targetFileName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String>
    suspend fun calculateSha256(filePath: String): String?
    fun canRequestPackageInstalls(): Boolean
    fun openInstallPermissionSettings()
    fun installApk(filePath: String): Result<Unit>
    fun clearUpdateCache()
    fun getCachedApkPath(fileName: String): String?
}
