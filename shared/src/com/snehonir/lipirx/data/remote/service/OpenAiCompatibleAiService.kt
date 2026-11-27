package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.data.remote.prompt.SINGLE_CALL_PROMPT

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.*
import com.snehonir.lipirx.domain.agent.AiOutputFormatException
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

abstract class OpenAiCompatibleAiService(
    protected val httpClient: HttpClient,
    private val defaultApiKey: String = "",
    private val jsonParser: Json
) : LipiRxAiService {

    abstract override val provider: AiProvider
    abstract val modelsEndpoint: String
    abstract val completionsEndpoint: String

    override suspend fun fetchAvailableModels(
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<List<ModelInfoDto>> = safeApiCall(provider.displayName) {
        val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
        if (targetApiKey.isBlank() && provider.requiresApiKey) {
            throw IllegalStateException("API key is required for ${provider.displayName}. Please enter a valid API key.")
        }

        val endpoint = if (provider == AiProvider.Custom) {
            resolveCustomModelsEndpoint(customUrl)
        } else {
            modelsEndpoint
        }

        Napier.d("[$flow] Dynamically fetching online models from ${provider.displayName} server: $endpoint", tag = TAG)

        val responseText: String = httpClient.get(endpoint) {
            if (targetApiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $targetApiKey")
            }
        }.body()

        Napier.d("[$flow -> ${provider.displayName} Models RawResponse] Raw models response:\n$responseText", tag = TAG)

        val response = jsonParser.decodeFromString<OpenAiModelsListResponse>(responseText)
        val allModels = response.data ?: emptyList()

        // Server-driven modalities & capability check:
        val visionModels = allModels.filter { dto ->
            val modalities = dto.inputModalities
            modalities?.contains("image") ?: isVisionCapableModel(provider, dto.id)
        }.map { dto ->
            ModelInfoDto(name = dto.id, displayName = dto.id)
        }

        Napier.d("[$flow] Filtered ${allModels.size} total models down to ${visionModels.size} Vision-capable models for ${provider.displayName}", tag = TAG)

        visionModels.ifEmpty {
            allModels.map { ModelInfoDto(name = it.id, displayName = it.id) }
        }
    }

    override suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        mimeType: String,
        flow: String,
        onProgress: (step: Int, statusText: String) -> Unit
    ): Result<List<MedicineItem>> {
        return try {
            val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
            if (provider.requiresApiKey && targetApiKey.isBlank()) {
                throw IllegalStateException("${provider.displayName} API key is missing. Please set your API key in Model Settings.")
            }
            val cleanModel = selectedModel.removePrefix(AppConstants.MODEL_PREFIX_GEMINI).ifBlank { provider.defaultModel }
            onProgress(0, "Encoding image & payload")
            Napier.d("[$flow] Starting processing: provider=${provider.id}, model=$cleanModel, size=${imageByteArray.size} bytes", tag = TAG)

            val base64Image = Base64.encode(imageByteArray)

            onProgress(1, "Reading handwriting")
            val endpoint = if (provider == AiProvider.Custom) {
                resolveCustomCompletionsEndpoint(customUrl)
            } else {
                completionsEndpoint
            }

            var rawResponseText = executeOpenAiPostCall(
                endpoint = endpoint,
                apiKey = targetApiKey,
                modelName = cleanModel,
                promptText = SINGLE_CALL_PROMPT,
                base64Image = base64Image,
                mimeType = mimeType,
                flow = flow
            )

            var parseResult = runCatching { parseTwoPassResponse(rawResponseText) }

            if (parseResult.isFailure) {
                Napier.w("[$flow -> Retry] Parsing failed on first attempt: ${parseResult.exceptionOrNull()?.message}. Retrying request once...", tag = TAG)
                onProgress(1, "Retrying Vision AI request...")
                rawResponseText = executeOpenAiPostCall(
                    endpoint = endpoint,
                    apiKey = targetApiKey,
                    modelName = cleanModel,
                    promptText = SINGLE_CALL_PROMPT,
                    base64Image = base64Image,
                    mimeType = mimeType,
                    flow = flow
                )
                parseResult = runCatching { parseTwoPassResponse(rawResponseText) }
            }

            val twoPassResponse = parseResult.getOrElse { ex ->
                throw AiOutputFormatException(rawOutput = rawResponseText, cause = ex)
            }
            onProgress(2, "Matching brand names")

            val items = merge(
                transcription = twoPassResponse.transcription ?: emptyList(),
                enrichment = twoPassResponse.enrichment ?: emptyList()
            )

            onProgress(3, "Finding similar brands")
            Napier.d("[$flow -> Result] Merged ${items.size} MedicineItem objects: $items", tag = TAG)
            Result.success(items)
        } catch (ex: Throwable) {
            val userFriendlyMessage = formatAiErrorMessage(ex, provider)
            Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userFriendlyMessage, ex))
        }
    }

    override suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<String> {
        return try {
            val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
            if (provider.requiresApiKey && targetApiKey.isBlank()) {
                throw IllegalStateException("${provider.displayName} API key is missing. Please set your API key in Model Settings.")
            }
            val cleanModel = selectedModel.removePrefix(AppConstants.MODEL_PREFIX_GEMINI).ifBlank { provider.defaultModel }
            val endpoint = if (provider == AiProvider.Custom) {
                resolveCustomCompletionsEndpoint(customUrl)
            } else {
                completionsEndpoint
            }

            val requestPayload = OpenAiChatRequest(
                model = cleanModel,
                messages = listOf(
                    OpenAiMessage(
                        role = "user",
                        content = listOf(
                            OpenAiContentPart(type = "text", text = promptText)
                        )
                    )
                ),
                temperature = 0.1
            )

            val requestJson = jsonParser.encodeToString(requestPayload)
            val responseText: String = try {
                httpClient.post(endpoint) {
                    if (targetApiKey.isNotBlank()) {
                        header(HttpHeaders.Authorization, "Bearer $targetApiKey")
                    }
                    header("HTTP-Referer", "https://github.com/FahimFoysal97/LipiRx")
                    header("X-Title", "LipiRx")
                    contentType(ContentType.Application.Json)
                    setBody(requestJson)
                }.body()
            } catch (postEx: Throwable) {
                val errorBody = if (postEx is ResponseException) {
                    runCatching { postEx.response.bodyAsText() }.getOrNull() ?: ""
                } else {
                    ""
                }
                val apiErrorMsg = if (errorBody.isNotBlank()) parseOpenAiErrorResponse(errorBody) else null
                val finalMsg = apiErrorMsg ?: formatAiErrorMessage(postEx, provider)
                throw IllegalStateException(finalMsg, postEx)
            }

            val response = jsonParser.decodeFromString<OpenAiChatResponse>(responseText)
            if (response.error != null) {
                throw IllegalStateException("API Error: ${response.error.message ?: "Unknown error"}")
            }
            val content = response.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("No content received from ${provider.displayName}.")
            Result.success(content)
        } catch (ex: Throwable) {
            Result.failure(ex)
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
        val res = processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
        res.onSuccess { onToken(it) }
        return res
    }

    private suspend fun executeOpenAiPostCall(
        endpoint: String,
        apiKey: String,
        modelName: String,
        promptText: String,
        base64Image: String,
        mimeType: String,
        flow: String
    ): String {
        val payload = OpenAiChatRequest(
            model = modelName,
            messages = listOf(
                OpenAiMessage(
                    role = "user",
                    content = listOf(
                        OpenAiContentPart(type = "text", text = promptText),
                        OpenAiContentPart(
                            type = "image_url",
                            imageUrl = OpenAiImageUrl(url = "data:$mimeType;base64,$base64Image")
                        )
                    )
                )
            ),
            temperature = 0.0
        )

        val requestJson = jsonParser.encodeToString(payload)
        Napier.d("[$flow] Sending OpenAI-compatible POST request (${requestJson.length} bytes) to $endpoint", tag = TAG)

        val responseText: String = try {
            httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                if (apiKey.isNotBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                header("HTTP-Referer", "https://github.com/FahimFoysal97/LipiRx")
                header("X-Title", "LipiRx")
                setBody(requestJson)
            }.body()
        } catch (postEx: Throwable) {
            val errorBody = if (postEx is ResponseException) {
                runCatching { postEx.response.bodyAsText() }.getOrNull() ?: ""
            } else {
                ""
            }
            val apiErrorMsg = if (errorBody.isNotBlank()) parseOpenAiErrorResponse(errorBody) else null
            val finalMsg = apiErrorMsg ?: formatAiErrorMessage(postEx, provider)
            throw IllegalStateException(finalMsg, postEx)
        }

        Napier.d("[$flow -> RawResponse] OpenAI-compatible Raw Response:\n$responseText", tag = TAG)

        val openAiResponse = jsonParser.decodeFromString<OpenAiChatResponse>(responseText)
        if (openAiResponse.error != null) {
            throw IllegalStateException("API Error: ${openAiResponse.error.message ?: "Unknown error"}")
        }

        val content = openAiResponse.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No text response received from AI model.")

        return cleanJsonResponse(content)
    }

    private fun parseOpenAiErrorResponse(rawJson: String): String? {
        return runCatching {
            val openAiResponse = jsonParser.decodeFromString<OpenAiChatResponse>(rawJson)
            openAiResponse.error?.message?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun parseTwoPassResponse(responseText: String): GeminiTwoPassResponse {
        return jsonParser.decodeFromString<GeminiTwoPassResponse>(responseText)
    }

    internal fun merge(
        transcription: List<GeminiTranscriptionDto>,
        enrichment: List<GeminiEnrichmentDto>
    ): List<MedicineItem> {
        if (transcription.isEmpty()) return emptyList()

        val enrichmentMap = enrichment.associateBy { it.id }

        return transcription.map { t ->
            val e = enrichmentMap[t.id]

            val medicineName = t.medicineNameWritten
            val strengthValue = t.strengthValue
            val strengthUnit = t.strengthUnit
            val strengthNote = if (strengthValue.isBlank()) "not stated" else ""

            val recognized = e?.brandRecognized ?: false
            val genericName = e?.genericName?.takeIf { it.isNotBlank() } ?: t.genericNameWritten
            val similar = e?.similarBrands ?: emptyList()

            val possibleMisreading = e?.possibleMisreading ?: ""
            val needsReview = !recognized || possibleMisreading.isNotBlank() || t.confidence.equals("low", ignoreCase = true)

            val possibleStrengths = e?.possibleStrengths ?: emptyList()
            val assumedStrength = e?.assumedStrength ?: ""
            val strengthSource = e?.strengthSource ?: ""
            val prescribedBrandFull = e?.prescribedBrandFull ?: ""
            val enrichmentConfidence = e?.enrichmentConfidence ?: "low"

            val unitsToDispense = t.units ?: 1
            val unitTypeString = t.unitType?.ifBlank { "pcs" } ?: "pcs"

            MedicineItem(
                medicineName = medicineName,
                prescribedBrandFull = prescribedBrandFull,
                genericName = genericName,
                dosageForm = t.dosageForm,
                strengthValue = strengthValue,
                strengthUnit = strengthUnit,
                strengthNote = strengthNote,
                doseAmount = t.doseAmount,
                doseUnit = t.doseUnit,
                frequencyPerDay = t.frequencyPerDay,
                schedule = t.schedule,
                instructions = t.instructions,
                duration = t.duration,
                units = unitsToDispense,
                unitType = unitTypeString,
                totalVolumeNeededMl = t.totalVolumeNeededMl,
                patientAgeGroup = t.patientAgeGroup,
                confidence = t.confidence.ifBlank { "high" },
                similarBrands = similar,
                brandRecognized = recognized,
                possibleStrengths = possibleStrengths,
                assumedStrength = assumedStrength,
                strengthSource = strengthSource,
                possibleMisreading = possibleMisreading,
                enrichmentConfidence = enrichmentConfidence,
                needsReview = needsReview
            )
        }
    }

    private fun isVisionCapableModel(provider: AiProvider, modelId: String): Boolean {
        val cleanId = modelId.lowercase().trim()

        val isNonVisionUtility = (
            cleanId.contains("tts") ||
            cleanId.contains("whisper") ||
            cleanId.contains("embed") ||
            cleanId.contains("babbage") ||
            cleanId.contains("davinci") ||
            cleanId.contains("sora") ||
            cleanId.contains("moderation") ||
            cleanId.contains("realtime") ||
            cleanId.contains("audio") ||
            cleanId.contains("transcribe") ||
            cleanId.contains("codex") ||
            cleanId.contains("search-api") ||
            cleanId.contains("prompt-guard") ||
            cleanId.contains("orpheus") ||
            cleanId.contains("lyria") ||
            cleanId.contains("aqa")
        )

        return !isNonVisionUtility && when (provider) {
            AiProvider.Gemini -> true
            AiProvider.Groq -> {
                cleanId.contains("vision") || cleanId.contains("llama-3.2-90b") || cleanId.contains("llama-3.2-11b") || cleanId.contains("qwen")
            }

            AiProvider.OpenAi -> {
                cleanId.startsWith("gpt-") || cleanId.startsWith("o1") || cleanId.startsWith("o3") || cleanId.startsWith("o4") || cleanId.startsWith("chatgpt-")
            }

            AiProvider.Claude -> true
            AiProvider.Custom -> true
            AiProvider.OnDevice -> true
        }
    }

    private fun cleanJsonResponse(rawText: String): String {
        var trimmed = rawText.trim()
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        }
        return sanitizeJsonString(trimmed)
    }

    private fun sanitizeJsonString(rawJson: String): String {
        val sb = StringBuilder()
        var inString = false
        var isEscaped = false

        for (i in rawJson.indices) {
            val ch = rawJson[i]
            if (inString) {
                if (isEscaped) {
                    sb.append(ch)
                    isEscaped = false
                } else if (ch == '\\') {
                    sb.append(ch)
                    isEscaped = true
                } else if (ch == '"') {
                    val nextNonSpace = rawJson.substring(i + 1).trimStart().firstOrNull()
                    if (nextNonSpace != null && nextNonSpace !in listOf(':', '}', ',', ']', '\n', '\r')) {
                        sb.append("\\\"")
                    } else {
                        sb.append(ch)
                        inString = false
                    }
                } else if (ch == '\n' || ch == '\r') {
                    sb.append("\\n")
                } else {
                    sb.append(ch)
                }
            } else {
                if (ch == '"') {
                    inString = true
                }
                sb.append(ch)
            }
        }
        val result = sb.toString()
        return if (inString) "$result\"" else result
    }

    private fun resolveCustomCompletionsEndpoint(customUrl: String): String {
        val normalized = normalizeCustomUrl(customUrl)
        if (normalized.isBlank()) return completionsEndpoint
        val clean = normalized.removeSuffix("/")
        return when {
            clean.endsWith("/chat/completions") -> clean
            clean.endsWith("/v1") -> "$clean/chat/completions"
            else -> "$clean/v1/chat/completions"
        }
    }

    private fun resolveCustomModelsEndpoint(customUrl: String): String {
        val normalized = normalizeCustomUrl(customUrl)
        if (normalized.isBlank()) return modelsEndpoint
        val clean = normalized.removeSuffix("/")
        return when {
            clean.endsWith("/models") -> clean
            clean.endsWith("/v1") -> "$clean/models"
            else -> "$clean/v1/models"
        }
    }

    private fun normalizeCustomUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    companion object {
        private const val TAG = "OpenAiCompatibleAiService"
    }
}
