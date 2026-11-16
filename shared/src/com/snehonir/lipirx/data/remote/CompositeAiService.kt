package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import io.github.aakira.napier.Napier

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
        selectedModel: String = "gemini-2.5-flash",
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        mimeType: String = "image/jpeg",
        strategy: OnDeviceStrategy = OnDeviceStrategy.HYBRID_FALLBACK,
        flow: String = "App -> CompositeAiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>> {
        val primaryService = getService(provider)
        val onDeviceService = services[AiProvider.OnDevice]

        Napier.d("[$flow] Executing processPrescriptionImage with strategy '${strategy.id}' (Primary: ${provider.displayName})", tag = TAG)

        return when (strategy) {
            OnDeviceStrategy.ON_DEVICE_ONLY -> {
                val service = onDeviceService ?: primaryService
                service.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
            }
            OnDeviceStrategy.CLOUD_ONLY -> {
                primaryService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
            }
            OnDeviceStrategy.ON_DEVICE_FIRST -> {
                onProgress(0, "Executing On-Device SLM first...")
                val localResult = onDeviceService?.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                if (localResult != null && localResult.isSuccess && localResult.getOrNull()?.isNotEmpty() == true) {
                    localResult
                } else {
                    Napier.w("[$flow -> Escalation] On-Device SLM incomplete or unavailable. Escalating to ${provider.displayName} Cloud AI...", tag = TAG)
                    onProgress(1, "Escalating to ${provider.displayName} Cloud AI...")
                    primaryService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                }
            }
            OnDeviceStrategy.HYBRID_FALLBACK -> {
                val cloudResult = primaryService.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                if (cloudResult.isSuccess) {
                    cloudResult
                } else {
                    Napier.w("[$flow -> Fallback] ${provider.displayName} Cloud AI request failed (${cloudResult.exceptionOrNull()?.message}). Falling back to On-Device SLM...", tag = TAG)
                    onProgress(1, "Cloud AI unavailable. Falling back to On-Device SLM...")
                    onDeviceService?.processPrescriptionImage(imageByteArray, selectedModel, providedApiKey, customUrl, mimeType, flow, onProgress)
                        ?: cloudResult
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
        flow: String = "App -> CompositeAiService.processTextPrompt"
    ): Result<String> {
        val primaryService = getService(provider)
        val onDeviceService = services[AiProvider.OnDevice]

        return when (strategy) {
            OnDeviceStrategy.ON_DEVICE_ONLY -> {
                val service = onDeviceService ?: primaryService
                service.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
            }
            OnDeviceStrategy.CLOUD_ONLY -> {
                primaryService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
            }
            OnDeviceStrategy.ON_DEVICE_FIRST -> {
                val localResult = onDeviceService?.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                if (localResult != null && localResult.isSuccess && localResult.getOrNull()?.isNotBlank() == true) {
                    localResult
                } else {
                    Napier.w("[$flow -> Escalation] On-Device SLM text prompt unavailable. Escalating to ${provider.displayName} Cloud AI...", tag = TAG)
                    primaryService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                }
            }
            OnDeviceStrategy.HYBRID_FALLBACK -> {
                val cloudResult = primaryService.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                if (cloudResult.isSuccess) {
                    cloudResult
                } else {
                    Napier.w("[$flow -> Fallback] ${provider.displayName} Cloud AI text prompt failed. Falling back to On-Device SLM...", tag = TAG)
                    onDeviceService?.processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
                        ?: cloudResult
                }
            }
        }
    }

    companion object {
        private const val TAG = "CompositeAiService"
    }
}
