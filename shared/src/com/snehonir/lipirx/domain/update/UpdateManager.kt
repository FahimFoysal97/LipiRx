package com.snehonir.lipirx.domain.update

import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.update.dto.GitHubReleaseDto
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class UpdateManager(
    private val httpClient: HttpClient,
    private val platformUpdater: PlatformUpdater,
    private val repository: PrescriptionRepository,
    private val jsonParser: Json
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _isDialogDismissed = MutableStateFlow(false)
    val isDialogDismissed: StateFlow<Boolean> = _isDialogDismissed.asStateFlow()

    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val currentVersionName: String
        get() = platformUpdater.getAppVersionName()

    fun checkForUpdates(silent: Boolean = false, fromSettings: Boolean = false) {
        val current = _updateState.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) {
            return
        }

        if (fromSettings) {
            _isDialogDismissed.value = true
        }

        if (!silent) {
            _updateState.value = UpdateState.Checking
        }

        scope.launch {
            val result = fetchLatestReleaseFromGitHub()
            result.fold(
                onSuccess = { updateInfo ->
                    if (updateInfo != null) {
                        val dismissedVersion = repository.getDismissedUpdateVersion()
                        if (fromSettings) {
                            _isDialogDismissed.value = true
                        } else if (silent && updateInfo.versionName == dismissedVersion) {
                            _isDialogDismissed.value = true
                        } else {
                            _isDialogDismissed.value = false
                        }
                        _updateState.value = UpdateState.Available(updateInfo)
                    } else {
                        if (!silent) {
                            _updateState.value = UpdateState.UpToDate
                        }
                    }
                },
                onFailure = { ex ->
                    Napier.w("[$TAG -> Error] Check for updates failed: ${ex.message}", throwable = ex, tag = TAG)
                    if (!silent) {
                        _updateState.value = UpdateState.Error(
                            ex.message?.ifBlank { "Failed to check for updates." } ?: "Failed to check for updates."
                        )
                    }
                }
            )
        }
    }

    private suspend fun fetchLatestReleaseFromGitHub(): Result<UpdateInfo?> {
        return runCatching {
            Napier.d("[$TAG] Requesting latest release from GitHub API...", tag = TAG)
            val responseText: String = httpClient.get("https://api.github.com/repos/FahimFoysal97/LipiRx/releases/latest") {
                header("User-Agent", "LipiRx-Android-App")
                header("Accept", "application/vnd.github.v3+json")
            }.body()

            val releaseDto = jsonParser.decodeFromString<GitHubReleaseDto>(responseText)
            val tagName = releaseDto.tagName ?: throw IllegalStateException("Release tag_name is missing.")
            val versionName = tagName.removePrefix("v").removePrefix("V").trim()
            val currentVersionName = platformUpdater.getAppVersionName()

            val latestSemVer = SemVer.parse(versionName)
            val currentSemVer = SemVer.parse(currentVersionName)

            Napier.d("[$TAG] GitHub Latest: $versionName, Installed: $currentVersionName", tag = TAG)

            if (latestSemVer > currentSemVer) {
                val apkAsset = releaseDto.assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }
                    ?: throw IllegalStateException("No APK asset found in release $tagName.")

                val sha256Asset =
                    releaseDto.assets.firstOrNull { it.name?.endsWith(".sha256", ignoreCase = true) == true }

                UpdateInfo(
                    versionName = versionName,
                    releaseNotes = releaseDto.body?.ifBlank { "New version v$versionName is available." } ?: "New version v$versionName is available.",
                    apkDownloadUrl = apkAsset.browserDownloadUrl ?: throw IllegalStateException("APK download URL is missing."),
                    sha256DownloadUrl = sha256Asset?.browserDownloadUrl,
                    apkFileName = apkAsset.name ?: "LipiRx-v$versionName.apk"
                )
            } else {
                null
            }
        }
    }

    fun startDownload(updateInfo: UpdateInfo) {
        if (_updateState.value is UpdateState.Downloading) return

        _updateState.value = UpdateState.Downloading(
            updateInfo = updateInfo,
            progress = 0f,
            bytesDownloaded = 0L,
            totalBytes = 0L
        )

        downloadJob?.cancel()
        downloadJob = scope.launch {
            val downloadResult = platformUpdater.downloadApk(
                downloadUrl = updateInfo.apkDownloadUrl,
                targetFileName = updateInfo.apkFileName
            ) { bytes, total ->
                val progressVal = if (total > 0) (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                _updateState.value = UpdateState.Downloading(
                    updateInfo = updateInfo,
                    progress = progressVal,
                    bytesDownloaded = bytes,
                    totalBytes = total
                )
            }

            downloadResult.fold(
                onSuccess = { apkPath ->
                    // Optional Checksum Verification
                    if (!updateInfo.sha256DownloadUrl.isNullOrBlank()) {
                        val checksumVerified = verifySha256Checksum(updateInfo.sha256DownloadUrl, apkPath)
                        if (!checksumVerified) {
                            _updateState.value = UpdateState.Error("APK checksum verification failed. The downloaded file may be corrupted.")
                            return@launch
                        }
                    }

                    // Check installation permission
                    if (platformUpdater.canRequestPackageInstalls()) {
                        platformUpdater.installApk(apkPath)
                        _updateState.value = UpdateState.ReadyToInstall(updateInfo, apkPath)
                    } else {
                        _updateState.value = UpdateState.NeedsInstallPermission(updateInfo, apkPath)
                    }
                },
                onFailure = { ex ->
                    Napier.e("[$TAG -> Failure] Download APK failed: ${ex.message}", throwable = ex, tag = TAG)
                    _updateState.value = UpdateState.Error("Download failed: ${ex.message ?: "Network error"}")
                }
            )
        }
    }

    private suspend fun verifySha256Checksum(sha256Url: String, apkPath: String): Boolean {
        return runCatching {
            Napier.d("[$TAG] Downloading SHA256 checksum from $sha256Url...", tag = TAG)
            val expectedText: String = httpClient.get(sha256Url).body()
            val expectedHash = expectedText.trim().substringBefore(" ").trim().lowercase()
            val actualHash = platformUpdater.calculateSha256(apkPath)?.lowercase() ?: ""
            Napier.d("[$TAG] SHA256 Expected: $expectedHash, Actual: $actualHash", tag = TAG)
            expectedHash.isBlank() || actualHash == expectedHash
        }.getOrDefault(true)
    }

    fun openInstallSettings() {
        platformUpdater.openInstallPermissionSettings()
    }

    fun onPermissionResultCheck() {
        val currentState = _updateState.value
        val (updateInfo, apkPath) = when (currentState) {
            is UpdateState.NeedsInstallPermission -> currentState.updateInfo to currentState.apkPath
            is UpdateState.ReadyToInstall -> currentState.updateInfo to currentState.apkPath
            else -> return
        }

        if (platformUpdater.canRequestPackageInstalls()) {
            platformUpdater.installApk(apkPath)
            _updateState.value = UpdateState.ReadyToInstall(updateInfo, apkPath)
        }
    }

    fun installNow(apkPath: String) {
        platformUpdater.installApk(apkPath)
    }

    fun dismissDialog() {
        val versionName = when (val current = _updateState.value) {
            is UpdateState.Available -> current.updateInfo.versionName
            is UpdateState.NeedsInstallPermission -> current.updateInfo.versionName
            is UpdateState.ReadyToInstall -> current.updateInfo.versionName
            else -> null
        }
        if (!versionName.isNullOrBlank()) {
            repository.saveDismissedUpdateVersion(versionName)
        }
        _isDialogDismissed.value = true
    }

    fun resetState() {
        _isDialogDismissed.value = false
        _updateState.value = UpdateState.Idle
    }

    companion object {
        private const val TAG = "UpdateManager"
    }
}
