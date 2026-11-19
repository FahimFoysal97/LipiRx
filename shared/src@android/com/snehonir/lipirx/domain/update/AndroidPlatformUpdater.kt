package com.snehonir.lipirx.domain.update

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class AndroidPlatformUpdater(
    private val context: Context,
    private val httpClient: HttpClient
) : PlatformUpdater {

    companion object {
        private const val TAG = "AndroidPlatformUpdater"
    }

    init {
        clearUpdateCache()
    }

    override fun getAppVersionName(): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()?.ifBlank { "0.4.1" } ?: "0.4.1"
    }

    override suspend fun downloadApk(
        downloadUrl: String,
        targetFileName: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String> {
        return runCatching {
            clearUpdateCache()
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val targetFile = File(updatesDir, targetFileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            Napier.d("[$TAG] Downloading update APK from $downloadUrl to ${targetFile.absolutePath}", tag = TAG)

            httpClient.prepareGet(downloadUrl).execute { response ->
                val totalBytes = response.contentLength() ?: -1L
                val channel: ByteReadChannel = response.bodyAsChannel()
                val outputStream = FileOutputStream(targetFile)

                var bytesDownloaded = 0L
                val buffer = ByteArray(8192)

                outputStream.use { outputStream ->
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        outputStream.write(buffer, 0, read)
                        bytesDownloaded += read
                        onProgress(bytesDownloaded, totalBytes)
                    }
                    outputStream.flush()
                }
            }

            Napier.d("[$TAG -> Success] APK download completed: ${targetFile.length()} bytes", tag = TAG)
            targetFile.absolutePath
        }
    }

    override suspend fun calculateSha256(filePath: String): String? {
        return runCatching {
            val file = File(filePath)
            if (!file.exists()) return null
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }

    override fun canRequestPackageInstalls(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    override fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun installApk(filePath: String): Result<Unit> {
        return runCatching {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalStateException("APK file does not exist at $filePath")
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Napier.d("[$TAG] Launching Android package installer for $contentUri", tag = TAG)
            context.startActivity(intent)
        }
    }

    override fun clearUpdateCache() {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates")
            if (updatesDir.exists() && updatesDir.isDirectory) {
                updatesDir.listFiles()?.forEach { file ->
                    val deleted = file.delete()
                    Napier.d("[$TAG] Cleaned cached update file '${file.name}': deleted=$deleted", tag = TAG)
                }
            }
        }.onFailure { ex ->
            Napier.w("[$TAG] Failed to clear update cache: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    override fun getCachedApkPath(fileName: String): String? {
        val file = File(File(context.cacheDir, "updates"), fileName)
        return if (file.exists() && file.length() > 0L) file.absolutePath else null
    }
}
