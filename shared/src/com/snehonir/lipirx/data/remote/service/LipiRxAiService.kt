package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

interface LipiRxAiService {
    val provider: AiProvider

    suspend fun fetchAvailableModels(
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "App -> LipiRxAiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>>

    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        mimeType: String = AppConstants.MIME_TYPE_JPEG,
        flow: String = "App -> LipiRxAiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>>

    suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "App -> LipiRxAiService.processTextPrompt"
    ): Result<String>

    suspend fun streamContent(
        promptText: String,
        selectedModel: String,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = "App -> LipiRxAiService.streamContent",
        onToken: (String) -> Unit
    ): Result<String>
}

suspend inline fun <T> safeApiCall(
    providerName: String,
    crossinline block: suspend () -> T
): Result<T> = runCatching {
    block()
}.onFailure { ex ->
    if (ex is CancellationException) throw ex
    Napier.e("[$providerName API Failure] ${ex.message}", throwable = ex)
}
