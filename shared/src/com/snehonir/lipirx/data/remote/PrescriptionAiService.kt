package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem

interface PrescriptionAiService {
    val provider: AiProvider

    suspend fun fetchAvailableModels(
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "com.snehonir.lipirx.App -> PrescriptionAiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>>

    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        mimeType: String = AppConstants.MIME_TYPE_JPEG,
        flow: String = "com.snehonir.lipirx.App -> PrescriptionAiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>>

    suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "com.snehonir.lipirx.App -> PrescriptionAiService.processTextPrompt"
    ): Result<String>

    suspend fun streamContent(
        promptText: String,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "com.snehonir.lipirx.App -> PrescriptionAiService.streamContent",
        onToken: (String) -> Unit = {}
    ): Result<String>
}
