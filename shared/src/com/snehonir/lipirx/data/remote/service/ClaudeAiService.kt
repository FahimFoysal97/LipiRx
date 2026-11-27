package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_api_key_missing
import com.snehonir.lipirx.resources.error_invalid_response
import com.snehonir.lipirx.resources.error_no_models_found
import com.snehonir.lipirx.resources.error_service_unavailable_fallback
import com.snehonir.lipirx.resources.status_agent_active
import com.snehonir.lipirx.resources.status_encoding_payload
import com.snehonir.lipirx.resources.status_matching_brands
import com.snehonir.lipirx.resources.status_reading_handwriting
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import kotlin.io.encoding.Base64

@Serializable
private data class ClaudeRequestPayload(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val messages: List<ClaudeRequestMessage>
)

@Serializable
private data class ClaudeRequestMessage(
    val role: String = "user",
    val content: List<ClaudeContentBlock>
)

@Serializable
private sealed class ClaudeContentBlock {
    @Serializable
    @SerialName("text")
    data class TextBlock(val text: String) : ClaudeContentBlock()

    @Serializable
    @SerialName("image")
    data class ImageBlock(val source: ClaudeImageSource) : ClaudeContentBlock()
}

@Serializable
private data class ClaudeImageSource(
    val type: String = "base64",
    @SerialName("media_type") val mediaType: String = AppConstants.MIME_TYPE_JPEG,
    val data: String
)

@Serializable
private data class ClaudeResponsePayload(
    val id: String? = null,
    val content: List<ClaudeResponseBlock>? = null,
    val error: ClaudeErrorPayload? = null
)

@Serializable
private data class ClaudeResponseBlock(
    val type: String? = null,
    val text: String? = null
)

@Serializable
private data class ClaudeErrorPayload(
    val type: String? = null,
    val message: String? = null
)

@Serializable
private data class ClaudeModelsResponse(
    val data: List<ClaudeModelItem>? = null
)

@Serializable
private data class ClaudeModelItem(
    val id: String,
    @SerialName("display_name") val displayName: String? = null
)

class ClaudeAiService(
    private val httpClient: HttpClient,
    private val defaultApiKey: String = "",
    private val jsonParser: Json
) : LipiRxAiService {

    override val provider: AiProvider = AiProvider.Claude
    private val endpoint: String = AppConstants.CLAUDE_DEFAULT_ENDPOINT

    override suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        mimeType: String,
        flow: String,
        onProgress: (step: Int, statusText: String) -> Unit
    ): Result<List<MedicineItem>> {
        val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
        if (targetApiKey.isBlank()) {
            return Result.failure(IllegalStateException(getString(Res.string.error_api_key_missing, provider.displayName)))
        }

        val model = selectedModel.ifBlank { provider.defaultModel }.trim()
        val targetUrl = customUrl.ifBlank { endpoint }.trim()

        onProgress(0, getString(Res.string.status_encoding_payload))
        Napier.d("[$flow] Encoding ${imageByteArray.size} bytes as Base64 for Claude model '$model'...", tag = TAG)

        val base64Data = Base64.encode(imageByteArray)
        val promptText = "Analyze this handwritten doctor prescription and return ONLY a raw JSON array of prescribed medicines matching standard schema."

        onProgress(1, getString(Res.string.status_agent_active))

        val requestPayload = ClaudeRequestPayload(
            model = model,
            messages = listOf(
                ClaudeRequestMessage(
                    role = "user",
                    content = listOf(
                        ClaudeContentBlock.ImageBlock(
                            source = ClaudeImageSource(
                                type = "base64",
                                mediaType = mimeType,
                                data = base64Data
                            )
                        ),
                        ClaudeContentBlock.TextBlock(text = promptText)
                    )
                )
            )
        )

        return try {
            val response = httpClient.post(targetUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    append(AppConstants.CLAUDE_HEADER_API_KEY, targetApiKey)
                    append(AppConstants.CLAUDE_HEADER_VERSION, AppConstants.CLAUDE_API_VERSION)
                }
                setBody(requestPayload)
            }

            val rawBody = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Napier.e("[$flow -> Error] Claude request failed (${response.status}): $rawBody", tag = TAG)
                val errorMsg = runCatching { jsonParser.decodeFromString<ClaudeResponsePayload>(rawBody).error?.message }.getOrNull()
                throw IllegalStateException(errorMsg ?: getString(Res.string.error_service_unavailable_fallback, provider.displayName))
            }

            onProgress(2, getString(Res.string.status_matching_brands))
            val parsedResponse = jsonParser.decodeFromString<ClaudeResponsePayload>(rawBody)
            val responseText = parsedResponse.content?.firstOrNull { it.type == "text" }?.text
                ?: throw IllegalStateException(getString(Res.string.error_invalid_response))

            onProgress(3, getString(Res.string.status_reading_handwriting))
            val items = parseMedicinesFromJson(responseText)
            Napier.d("[$flow -> Success] Claude extracted ${items.size} medicine items", tag = TAG)
            Result.success(items)
        } catch (ex: Throwable) {
            val userMsg = formatAiErrorMessage(ex, provider)
            Napier.e("[$flow -> Exception] $userMsg", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userMsg, ex))
        }
    }

    override suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<String> {
        val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
        if (targetApiKey.isBlank()) {
            return Result.failure(IllegalStateException(getString(Res.string.error_api_key_missing, provider.displayName)))
        }

        val model = selectedModel.ifBlank { provider.defaultModel }.trim()
        val targetUrl = customUrl.ifBlank { endpoint }.trim()

        val requestPayload = ClaudeRequestPayload(
            model = model,
            messages = listOf(
                ClaudeRequestMessage(
                    role = "user",
                    content = listOf(ClaudeContentBlock.TextBlock(text = promptText))
                )
            )
        )

        return try {
            val response = httpClient.post(targetUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    append(AppConstants.CLAUDE_HEADER_API_KEY, targetApiKey)
                    append(AppConstants.CLAUDE_HEADER_VERSION, AppConstants.CLAUDE_API_VERSION)
                }
                setBody(requestPayload)
            }

            val rawBody = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val errorMsg = runCatching { jsonParser.decodeFromString<ClaudeResponsePayload>(rawBody).error?.message }.getOrNull()
                throw IllegalStateException(errorMsg ?: getString(Res.string.error_service_unavailable_fallback, provider.displayName))
            }

            val parsedResponse = jsonParser.decodeFromString<ClaudeResponsePayload>(rawBody)
            val responseText = parsedResponse.content?.firstOrNull { it.type == "text" }?.text
                ?: throw IllegalStateException(getString(Res.string.error_invalid_response))
            Result.success(responseText)
        } catch (ex: Throwable) {
            val userMsg = formatAiErrorMessage(ex, provider)
            Napier.e("[$flow -> Exception] $userMsg", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userMsg, ex))
        }
    }

    override suspend fun streamContent(
        promptText: String,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        flow: String,
        onToken: (String) -> Unit
    ): Result<String> {
        return processTextPrompt(
            promptText = promptText,
            selectedModel = selectedModel,
            providedApiKey = providedApiKey,
            customUrl = customUrl,
            flow = flow
        ).onSuccess { text ->
            onToken(text)
        }
    }

    override suspend fun fetchAvailableModels(
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<List<ModelInfoDto>> = safeApiCall("Claude") {
        val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
        if (targetApiKey.isBlank()) {
            throw IllegalStateException(getString(Res.string.error_api_key_missing, provider.displayName))
        }

        val targetUrl = customUrl.ifBlank { AppConstants.CLAUDE_MODELS_ENDPOINT }.trim()

        val response = httpClient.post(targetUrl) {
            headers {
                append(AppConstants.CLAUDE_HEADER_API_KEY, targetApiKey)
                append(AppConstants.CLAUDE_HEADER_VERSION, AppConstants.CLAUDE_API_VERSION)
            }
        }

        val rawBody = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val errorMsg = runCatching { jsonParser.decodeFromString<ClaudeResponsePayload>(rawBody).error?.message }.getOrNull()
            throw IllegalStateException(errorMsg ?: getString(Res.string.error_no_models_found))
        }

        val parsedResponse = jsonParser.decodeFromString<ClaudeModelsResponse>(rawBody)
        val modelItems = parsedResponse.data?.map { item ->
            ModelInfoDto(name = item.id, displayName = item.displayName ?: item.id)
        } ?: emptyList()

        if (modelItems.isEmpty()) {
            throw IllegalStateException(getString(Res.string.error_no_models_found))
        }

        modelItems
    }

    private fun parseMedicinesFromJson(jsonText: String): List<MedicineItem> {
        val cleaned = jsonText.substringAfter("[").substringBeforeLast("]")
        if (cleaned.isBlank()) return emptyList()
        val wrappedJson = "[$cleaned]"
        return runCatching {
            jsonParser.decodeFromString<List<MedicineItem>>(wrappedJson)
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val TAG = "ClaudeAiService"
    }
}
