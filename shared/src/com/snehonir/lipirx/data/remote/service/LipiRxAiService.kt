package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.agent.AiAuthenticationException
import com.snehonir.lipirx.domain.agent.AiModelOverloadedException
import com.snehonir.lipirx.domain.agent.AiRateLimitException
import com.snehonir.lipirx.domain.agent.NetworkUnavailableException
import com.snehonir.lipirx.domain.agent.isCancellation
import com.snehonir.lipirx.domain.agent.toTypedAiException
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_analysis_stopped
import com.snehonir.lipirx.resources.error_bad_request
import com.snehonir.lipirx.resources.error_api_key_invalid_details
import com.snehonir.lipirx.resources.error_model_not_found
import com.snehonir.lipirx.resources.error_model_not_vision
import com.snehonir.lipirx.resources.error_model_overloaded
import com.snehonir.lipirx.resources.error_no_internet
import com.snehonir.lipirx.resources.error_no_local_slm_model
import com.snehonir.lipirx.resources.error_out_of_memory
import com.snehonir.lipirx.resources.error_payload_too_large
import com.snehonir.lipirx.resources.error_quota_rate_limit_exceeded
import com.snehonir.lipirx.resources.error_service_unavailable_fallback
import com.snehonir.lipirx.resources.error_unprocessable_entity
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.getString

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
    if (toTypedAiException(ex).isCancellation()) throw ex
    Napier.e("[$providerName API Failure] ${ex.message}", throwable = ex)
}

suspend fun formatAiErrorMessage(ex: Throwable, provider: AiProvider): String {
    val typedEx = toTypedAiException(ex)
    if (typedEx.isCancellation()) {
        return getString(Res.string.error_analysis_stopped)
    }
    val providerName = provider.displayName

    return when (typedEx) {
        is NetworkUnavailableException -> getString(Res.string.error_no_internet)
        is AiRateLimitException -> getString(Res.string.error_quota_rate_limit_exceeded, providerName)
        is AiModelOverloadedException -> getString(Res.string.error_model_overloaded, providerName)
        is AiAuthenticationException -> getString(Res.string.error_api_key_invalid_details, providerName)
        else -> {
            val msg = typedEx.message ?: ""
            when {
                msg.contains("413") || msg.contains("Payload Too Large", ignoreCase = true) || msg.contains("request_too_large", ignoreCase = true) ->
                    getString(Res.string.error_payload_too_large)
                msg.contains("content must be a string", ignoreCase = true) ->
                    getString(Res.string.error_model_not_vision)
                msg.contains("400") || msg.contains("INVALID_ARGUMENT", ignoreCase = true) || msg.contains("invalid_request", ignoreCase = true) ->
                    getString(Res.string.error_bad_request)
                msg.contains("404") || msg.contains("NOT_FOUND", ignoreCase = true) ->
                    getString(Res.string.error_model_not_found, providerName)
                msg.contains("422") || msg.contains("Unprocessable", ignoreCase = true) ->
                    getString(Res.string.error_unprocessable_entity, providerName)
                msg.contains("OutOfMemory", ignoreCase = true) ->
                    getString(Res.string.error_out_of_memory)
                msg.contains(".litertlm", ignoreCase = true) || msg.contains(".tflite", ignoreCase = true) || msg.contains("No local", ignoreCase = true) || msg.contains("model file", ignoreCase = true) ->
                    getString(Res.string.error_no_local_slm_model)
                else -> msg.ifBlank { getString(Res.string.error_service_unavailable_fallback, providerName) }
            }
        }
    }
}
