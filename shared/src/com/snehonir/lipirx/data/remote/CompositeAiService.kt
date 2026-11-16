package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_cloud_failed_max_attempts
import com.snehonir.lipirx.resources.error_unknown
import com.snehonir.lipirx.resources.progress_attempt_failed
import com.snehonir.lipirx.resources.progress_escalating_to_cloud
import com.snehonir.lipirx.resources.progress_executing_on_device_first
import com.snehonir.lipirx.resources.progress_falling_back
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.milliseconds

class CompositeAiService(
    private val services: Map<AiProvider, PrescriptionAiService>
) {
    fun getService(provider: AiProvider): PrescriptionAiService {
        return services[provider] ?: services.values.first()
    }

    suspend fun fetchAvailableModels(
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        flow: String = "App -> CompositeAiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>> {
        val activeService = getService(provider)
        Napier.d("[$flow] Routing fetchAvailableModels to ${activeService::class.simpleName} for ${provider.displayName}", tag = TAG)
        return activeService.fetchAvailableModels(providedApiKey, customUrl, flow)
    }

    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String = AppConstants.GEMINI_DEFAULT_MODEL,
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        mimeType: String = AppConstants.MIME_TYPE_JPEG,
        strategy: OnDeviceStrategy = OnDeviceStrategy.HYBRID_FALLBACK,
        fallbackProvider: AiProvider? = null,
        fallbackModel: String = "",
        flow: String = "App -> CompositeAiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>> {
        val primaryService = getService(provider)
        val targetFallbackProvider = fallbackProvider ?: if (provider != AiProvider.OnDevice) AiProvider.OnDevice else AiProvider.Gemini
        val fallbackService = getService(targetFallbackProvider)

        Napier.d("[$flow] Executing processPrescriptionImage with strategy '${strategy.id}' (Primary: ${provider.displayName}, Fallback: ${targetFallbackProvider.displayName})", tag = TAG)

        return when (strategy) {
            OnDeviceStrategy.ON_DEVICE_ONLY -> {
                val service = services[AiProvider.OnDevice] ?: primaryService
                service.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
            }
            OnDeviceStrategy.CLOUD_ONLY -> {
                executeWithCloudRetries(
                    maxAttempts = AppConstants.MAX_CLOUD_RETRIES,
                    providerName = provider.displayName,
                    flow = flow,
                    onRetryProgress = { _, msg -> onProgress(1, msg) }
                ) {
                    primaryService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                }
            }
            OnDeviceStrategy.ON_DEVICE_FIRST -> {
                val executingMsg = getString(Res.string.progress_executing_on_device_first)
                onProgress(0, executingMsg)

                val localService = services[AiProvider.OnDevice] ?: fallbackService
                val localResult = localService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                if (localResult.isSuccess && localResult.getOrNull()?.isNotEmpty() == true) {
                    localResult
                } else {
                    val escalatingMsg = getString(Res.string.progress_escalating_to_cloud, provider.displayName)
                    Napier.w("[$flow -> Escalation] On-Device SLM incomplete or unavailable. Escalating to ${provider.displayName} Cloud AI...", tag = TAG)
                    onProgress(1, escalatingMsg)
                    executeWithCloudRetries(
                        maxAttempts = AppConstants.MAX_CLOUD_RETRIES,
                        providerName = provider.displayName,
                        flow = flow,
                        onRetryProgress = { _, msg -> onProgress(1, msg) }
                    ) {
                        primaryService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                    }
                }
            }
            OnDeviceStrategy.HYBRID_FALLBACK -> {
                val cloudResult = executeWithCloudRetries(
                    maxAttempts = AppConstants.MAX_CLOUD_RETRIES,
                    providerName = provider.displayName,
                    flow = flow,
                    onRetryProgress = { _, msg -> onProgress(1, msg) }
                ) {
                    primaryService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                }

                if (cloudResult.isSuccess) {
                    cloudResult
                } else {
                    val targetModelName = fallbackModel.ifBlank { selectedModel }
                    val fallingBackMsg = getString(Res.string.progress_falling_back, provider.displayName, AppConstants.MAX_CLOUD_RETRIES, targetFallbackProvider.displayName)
                    Napier.w("[$flow -> Fallback Triggered] ${provider.displayName} Cloud AI failed after ${AppConstants.MAX_CLOUD_RETRIES} attempts (${cloudResult.exceptionOrNull()?.message}). Falling back to ${targetFallbackProvider.displayName} ($targetModelName)...", tag = TAG)
                    onProgress(1, fallingBackMsg)
                    fallbackService.processPrescriptionImage(imageByteArray, targetModelName, providedApiKey, customUrl, mimeType, flow, onProgress)
                }
            }
        }
    }

    suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String = "",
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        strategy: OnDeviceStrategy = OnDeviceStrategy.HYBRID_FALLBACK,
        fallbackProvider: AiProvider? = null,
        fallbackModel: String = "",
        flow: String = "App -> CompositeAiService.processTextPrompt"
    ): Result<String> {
        val primaryService = getService(provider)
        val targetFallbackProvider = fallbackProvider ?: if (provider != AiProvider.OnDevice) AiProvider.OnDevice else AiProvider.Gemini
        val fallbackService = getService(targetFallbackProvider)

        return when (strategy) {
            OnDeviceStrategy.ON_DEVICE_ONLY -> {
                val service = services[AiProvider.OnDevice] ?: primaryService
                service.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
            }
            OnDeviceStrategy.CLOUD_ONLY -> {
                executeWithCloudRetries(
                    maxAttempts = AppConstants.MAX_CLOUD_RETRIES,
                    providerName = provider.displayName,
                    flow = flow
                ) {
                    primaryService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                }
            }
            OnDeviceStrategy.ON_DEVICE_FIRST -> {
                val localService = services[AiProvider.OnDevice] ?: fallbackService
                val localResult = localService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                if (localResult.isSuccess && localResult.getOrNull()?.isNotBlank() == true) {
                    localResult
                } else {
                    Napier.w("[$flow -> Escalation] On-Device SLM text prompt unavailable. Escalating to ${provider.displayName} Cloud AI...", tag = TAG)
                    executeWithCloudRetries(
                        maxAttempts = AppConstants.MAX_CLOUD_RETRIES,
                        providerName = provider.displayName,
                        flow = flow
                    ) {
                        primaryService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                    }
                }
            }
            OnDeviceStrategy.HYBRID_FALLBACK -> {
                val cloudResult = executeWithCloudRetries(
                    maxAttempts = AppConstants.MAX_CLOUD_RETRIES,
                    providerName = provider.displayName,
                    flow = flow
                ) {
                    primaryService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                }

                if (cloudResult.isSuccess) {
                    cloudResult
                } else {
                    val targetModelName = fallbackModel.ifBlank { selectedModel }
                    Napier.w("[$flow -> Fallback Triggered] ${provider.displayName} Cloud AI text prompt failed after ${AppConstants.MAX_CLOUD_RETRIES} attempts. Falling back to ${targetFallbackProvider.displayName} ($targetModelName)...", tag = TAG)
                    fallbackService.processTextPrompt(promptText, targetModelName, providedApiKey, customUrl, flow)
                }
            }
        }
    }

    private suspend fun <T> executeWithCloudRetries(
        maxAttempts: Int = AppConstants.MAX_CLOUD_RETRIES,
        providerName: String,
        flow: String,
        onRetryProgress: ((attempt: Int, errorMsg: String) -> Unit)? = null,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastResult: Result<T>? = null
        val unknownErr = getString(Res.string.error_unknown)
        val maxFailedErr = getString(Res.string.error_cloud_failed_max_attempts, providerName, maxAttempts)

        for (attempt in 1..maxAttempts) {
            val result = block()
            if (result.isSuccess) {
                if (attempt > 1) {
                    Napier.i("[$flow -> Cloud Retry Success] $providerName Cloud AI succeeded on attempt $attempt/$maxAttempts.", tag = TAG)
                }
                return result
            }
            lastResult = result
            val errMsg = result.exceptionOrNull()?.message ?: unknownErr
            val attemptFailedMsg = getString(Res.string.progress_attempt_failed, providerName, attempt, maxAttempts, errMsg)
            Napier.w("[$flow -> Attempt $attempt/$maxAttempts Failed] $attemptFailedMsg", tag = TAG)
            onRetryProgress?.invoke(attempt, attemptFailedMsg)
            if (attempt < maxAttempts) {
                delay(AppConstants.CLOUD_RETRY_DELAY_MILLIS.milliseconds)
            }
        }
        return lastResult ?: Result.failure(IllegalStateException(maxFailedErr))
    }

    companion object {
        private const val TAG = "CompositeAiService"
    }
}
