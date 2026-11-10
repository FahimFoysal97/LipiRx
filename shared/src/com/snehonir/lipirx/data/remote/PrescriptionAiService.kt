package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem

interface PrescriptionAiService {
    val provider: AiProvider

    suspend fun fetchAvailableModels(
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "App -> PrescriptionAiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>>

    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        mimeType: String = "image/jpeg",
        flow: String = "App -> PrescriptionAiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>>

    suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "App -> PrescriptionAiService.processTextPrompt"
    ): Result<String>
}
