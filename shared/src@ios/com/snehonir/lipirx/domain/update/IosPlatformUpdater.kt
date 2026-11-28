package com.snehonir.lipirx.domain.update

import io.github.aakira.napier.Napier

class IosPlatformUpdater : PlatformUpdater {

    override fun getAppVersionName(): String = "0.6.5"

    override suspend fun downloadApk(
        downloadUrl: String,
        targetFileName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String> {
        return Result.failure(UnsupportedOperationException("APK updates are not supported on iOS."))
    }

    override suspend fun calculateSha256(filePath: String): String? = null

    override fun canRequestPackageInstalls(): Boolean = false

    override fun openInstallPermissionSettings() {
        Napier.w("Install permission settings not available on iOS.", tag = "IosPlatformUpdater")
    }

    override fun installApk(filePath: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("APK installation is not supported on iOS."))
    }

    override fun clearUpdateCache() {
        // No-op on iOS
    }

    override fun getCachedApkPath(fileName: String): String? = null
}
