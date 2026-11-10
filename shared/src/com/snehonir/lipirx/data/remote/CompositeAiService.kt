package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
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
        selectedModel: String = "gemini-1.5-pro",
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        mimeType: String = "image/jpeg",
        flow: String = "App -> CompositeAiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>> {
        val activeService = getService(provider)
        Napier.d("[$flow] Routing processPrescriptionImage to ${activeService::class.simpleName} for ${provider.displayName}", tag = TAG)
        return activeService.processPrescriptionImage(
            imageByteArray = imageByteArray,
            selectedModel = selectedModel,
            providedApiKey = providedApiKey,
            customUrl = customUrl,
            mimeType = mimeType,
            flow = flow,
            onProgress = onProgress
        )
    }

    suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String = "",
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        flow: String = "App -> CompositeAiService.processTextPrompt"
    ): Result<String> {
        val activeService = getService(provider)
        Napier.d("[$flow] Routing processTextPrompt to ${activeService::class.simpleName} for ${provider.displayName}", tag = TAG)
        return activeService.processTextPrompt(
            promptText = promptText,
            selectedModel = selectedModel,
            providedApiKey = providedApiKey,
            customUrl = customUrl,
            flow = flow
        )
    }

    companion object {
        private const val TAG = "CompositeAiService"
    }
}
