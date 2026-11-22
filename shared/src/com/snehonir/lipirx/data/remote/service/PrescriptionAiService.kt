package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.data.remote.prompt.SINGLE_CALL_PROMPT

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
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
        mimeType: String = AppConstants.MIME_TYPE_JPEG,
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

    suspend fun streamContent(
        promptText: String,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "App -> PrescriptionAiService.streamContent",
        onToken: (String) -> Unit = {}
    ): Result<String>
}
