package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class GeminiService(
    private val httpClient: HttpClient,
    private val apiKey: String = ""
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun fetchAvailableModels(
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        flow: String = "App -> GeminiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>> {
        return try {
            val targetApiKey = providedApiKey.ifBlank { apiKey }.trim()
            if (targetApiKey.isBlank() && provider.requiresApiKey) {
                throw IllegalStateException("API key is required for ${provider.displayName}. Please enter a valid API key.")
            }

            val modelsList = when (provider) {
                AiProvider.Groq -> fetchOpenAiCompatibleModels(
                    endpoint = provider.defaultEndpoint,
                    apiKey = targetApiKey,
                    provider = provider,
                    flow = flow
                )
                AiProvider.OpenAi -> fetchOpenAiCompatibleModels(
                    endpoint = provider.defaultEndpoint,
                    apiKey = targetApiKey,
                    provider = provider,
                    flow = flow
                )
                AiProvider.Custom -> {
                    val customModelsUrl = if (customUrl.contains("/chat/completions")) {
                        customUrl.replace("/chat/completions", "/models")
                    } else if (customUrl.endsWith("/")) {
                        "${customUrl}models"
                    } else {
                        "$customUrl/models"
                    }
                    runCatching {
                        fetchOpenAiCompatibleModels(
                            endpoint = customModelsUrl,
                            apiKey = targetApiKey,
                            provider = provider,
                            flow = flow
                        )
                    }.getOrElse {
                        val fallbackName = customUrl.substringAfterLast("/").ifBlank { provider.defaultModel }
                        listOf(ModelInfoDto(name = fallbackName, displayName = fallbackName))
                    }
                }
                AiProvider.Gemini -> {
                    val endpoint = "${provider.defaultEndpoint}?key=$targetApiKey"
                    Napier.d("[$flow] Dynamically fetching online models from Gemini server: $endpoint", tag = TAG)

                    val responseText: String = httpClient.get(endpoint).body()
                    Napier.d("[$flow -> Gemini Models RawResponse] Raw models response:\n$responseText", tag = TAG)

                    val modelsResponse = json.decodeFromString<GeminiModelsResponse>(responseText)
                    modelsResponse.models?.filter { model ->
                        val cleanName = model.name.lowercase()
                        val supportsGenerate = model.supportedGenerationMethods != null && model.supportedGenerationMethods.contains("generateContent")
                        val isNonVisionType = cleanName.contains("tts") || cleanName.contains("lyria") || cleanName.contains("embedding") || cleanName.contains("aqa")
                        supportsGenerate && !isNonVisionType
                    } ?: emptyList()
                }
            }

            Napier.d("[$flow -> Result] Found ${modelsList.size} dynamic online Vision models from ${provider.displayName} server: ${modelsList.map { it.name }}", tag = TAG)
            Result.success(modelsList)
        } catch (ex: Throwable) {
            val userFriendlyMessage = formatGeminiErrorMessage(ex)
            Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userFriendlyMessage, ex))
        }
    }

    private suspend fun fetchOpenAiCompatibleModels(
        endpoint: String,
        apiKey: String,
        provider: AiProvider,
        flow: String
    ): List<ModelInfoDto> {
        Napier.d("[$flow] Dynamically fetching online models from ${provider.displayName} server: $endpoint", tag = TAG)
        val responseText: String = httpClient.get(endpoint) {
            if (apiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
        }.body()

        Napier.d("[$flow -> ${provider.displayName} Models RawResponse] Raw models response:\n$responseText", tag = TAG)

        val response = json.decodeFromString<OpenAiModelsListResponse>(responseText)
        val allModels = response.data ?: emptyList()

        // Server-driven modalities & capability check:
        // 1. Filter out audio/speech/embedding/guard/codex/realtime/tts models.
        // 2. If server explicitly returns input_modalities: ["text", "image"], verify "image" is present.
        val visionModels = allModels.filter { dto ->
            val modalities = dto.inputModalities
            modalities?.contains("image") ?: isVisionCapableModel(provider, dto.id)
        }.map { dto ->
            ModelInfoDto(name = dto.id, displayName = dto.id)
        }

        Napier.d("[$flow] Filtered ${allModels.size} total models down to ${visionModels.size} Vision-capable models for ${provider.displayName}", tag = TAG)

        return if (visionModels.isNotEmpty()) {
            visionModels
        } else {
            allModels.map { ModelInfoDto(name = it.id, displayName = it.id) }
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

        if (isNonVisionUtility) return false

        return when (provider) {
            AiProvider.Gemini -> true
            AiProvider.Groq -> {
                cleanId.contains("vision") || cleanId.contains("llama-3.2-90b") || cleanId.contains("llama-3.2-11b") || cleanId.contains("qwen")
            }
            AiProvider.OpenAi -> {
                cleanId.startsWith("gpt-") || cleanId.startsWith("o1") || cleanId.startsWith("o3") || cleanId.startsWith("o4") || cleanId.startsWith("chatgpt-")
            }
            AiProvider.Custom -> true
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String = "gemini-1.5-pro",
        providedApiKey: String = "",
        provider: AiProvider = AiProvider.Gemini,
        customUrl: String = "",
        mimeType: String = "image/jpeg",
        flow: String = "App -> GeminiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>> {
        return try {
            val targetApiKey = providedApiKey.ifBlank { apiKey }.trim()
            if (provider.requiresApiKey && targetApiKey.isBlank()) {
                throw IllegalStateException("${provider.displayName} API key is missing. Please set your API key in Model Settings.")
            }
            val cleanModel = selectedModel.removePrefix("models/").ifBlank { provider.defaultModel }
            onProgress(0, "Encoding image & payload")
            Napier.d("[$flow] Starting processing: provider=${provider.id}, model=$cleanModel, size=${imageByteArray.size} bytes", tag = TAG)

            val base64Image = Base64.encode(imageByteArray)

            onProgress(1, "Reading handwriting")
            var rawResponseText = when (provider) {
                AiProvider.OpenAi, AiProvider.Groq, AiProvider.Custom -> {
                    val endpoint = when (provider) {
                        AiProvider.OpenAi -> "https://api.openai.com/v1/chat/completions"
                        AiProvider.Groq -> "https://api.groq.com/openai/v1/chat/completions"
                        AiProvider.Custom -> customUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
                        AiProvider.Gemini -> "https://generativelanguage.googleapis.com"
                    }
                    executeOpenAiPostCall(
                        endpoint = endpoint,
                        apiKey = targetApiKey,
                        modelName = cleanModel,
                        promptText = SINGLE_CALL_PROMPT,
                        base64Image = base64Image,
                        mimeType = mimeType,
                        flow = flow
                    )
                }
                AiProvider.Gemini -> {
                    val requestPayload = GeminiRequest(
                        systemInstruction = SystemInstruction(
                            parts = listOf(Part(text = SINGLE_CALL_PROMPT))
                        ),
                        contents = listOf(
                            Content(
                                role = "user",
                                parts = listOf(
                                    Part(inlineData = InlineData(mimeType = mimeType, data = base64Image)),
                                    Part(text = "Please analyze this prescription image and extract all medicine items.")
                                )
                            )
                        ),
                        generationConfig = GenerationConfig(
                            responseMimeType = "application/json",
                            temperature = 0.0,
                            maxOutputTokens = 16384
                        )
                    )

                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$targetApiKey"
                    executeGeminiPostCall(endpoint, requestPayload, flow)
                }
            }

            var parseResult = runCatching { parseTwoPassResponse(rawResponseText) }

            if (parseResult.isFailure) {
                Napier.w("[$flow -> Retry] Parsing failed on first attempt: ${parseResult.exceptionOrNull()?.message}. Retrying request once...", tag = TAG)
                onProgress(1, "Retrying Vision AI request...")
                rawResponseText = when (provider) {
                    AiProvider.OpenAi, AiProvider.Groq, AiProvider.Custom -> {
                        val endpoint = when (provider) {
                            AiProvider.OpenAi -> "https://api.openai.com/v1/chat/completions"
                            AiProvider.Groq -> "https://api.groq.com/openai/v1/chat/completions"
                            AiProvider.Custom -> customUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
                            AiProvider.Gemini -> "https://generativelanguage.googleapis.com"
                        }
                        executeOpenAiPostCall(
                            endpoint = endpoint,
                            apiKey = targetApiKey,
                            modelName = cleanModel,
                            promptText = SINGLE_CALL_PROMPT,
                            base64Image = base64Image,
                            mimeType = mimeType,
                            flow = flow
                        )
                    }
                    AiProvider.Gemini -> {
                        val requestPayload = GeminiRequest(
                            systemInstruction = SystemInstruction(
                                parts = listOf(Part(text = SINGLE_CALL_PROMPT))
                            ),
                            contents = listOf(
                                Content(
                                    role = "user",
                                    parts = listOf(
                                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image)),
                                        Part(text = "Please analyze this prescription image and extract all medicine items.")
                                    )
                                )
                            ),
                            generationConfig = GenerationConfig(
                                responseMimeType = "application/json",
                                temperature = 0.0,
                                maxOutputTokens = 16384
                            )
                        )

                        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$targetApiKey"
                        executeGeminiPostCall(endpoint, requestPayload, flow)
                    }
                }
                parseResult = runCatching { parseTwoPassResponse(rawResponseText) }
            }

            val twoPassResponse = parseResult.getOrThrow()
            onProgress(2, "Matching brand names")

            val items = merge(
                transcription = twoPassResponse.transcription ?: emptyList(),
                enrichment = twoPassResponse.enrichment ?: emptyList()
            )

            onProgress(3, "Finding similar brands")
            Napier.d("[$flow -> Result] Merged ${items.size} MedicineItem objects: $items", tag = TAG)
            Result.success(items)
        } catch (ex: Throwable) {
            val userFriendlyMessage = formatGeminiErrorMessage(ex)
            Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userFriendlyMessage, ex))
        }
    }

    private suspend fun executeGeminiPostCall(
        endpoint: String,
        requestPayload: GeminiRequest,
        flow: String
    ): String {
        val requestJson = json.encodeToString(requestPayload)
        Napier.d("[$flow] Serialized request JSON (${requestJson.length} bytes). Sending POST to $endpoint", tag = TAG)

        val responseText: String = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }.body()

        Napier.d("[$flow -> RawResponse] Gemini REST API Raw Response:\n$responseText", tag = TAG)
        return responseText
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

        val requestJson = json.encodeToString(payload)
        Napier.d("[$flow] Sending OpenAI-compatible POST request (${requestJson.length} bytes) to $endpoint", tag = TAG)

        val responseText: String = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            if (apiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(requestJson)
        }.body()

        Napier.d("[$flow -> RawResponse] OpenAI-compatible Raw Response:\n$responseText", tag = TAG)

        val openAiResponse = json.decodeFromString<OpenAiChatResponse>(responseText)
        if (openAiResponse.error != null) {
            throw IllegalStateException("API Error: ${openAiResponse.error.message ?: "Unknown error"}")
        }

        val content = openAiResponse.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("No text response received from AI model.")

        return cleanJsonResponse(content)
    }

    private fun parseTwoPassResponse(responseText: String): GeminiTwoPassResponse {
        val response = json.decodeFromString<GeminiResponse>(responseText)

        if (response.error != null) {
            val errorMsg = response.error.message ?: "Unknown error"
            Napier.e("[parseTwoPassResponse -> Error] Gemini API returned error: $errorMsg", tag = TAG)
            throw IllegalStateException("Gemini API Error: $errorMsg")
        }

        val candidate = response.candidates?.firstOrNull()
            ?: throw IllegalStateException("No candidate response received from Gemini API.")

        if (candidate.finishReason == "MAX_TOKENS") {
            Napier.w("[parseTwoPassResponse -> Warning] Response reached MAX_TOKENS limit", tag = TAG)
        }

        val rawContent = candidate.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("No text response received from Gemini API.")

        val cleanedJson = cleanJsonResponse(rawContent)
        return json.decodeFromString<GeminiTwoPassResponse>(cleanedJson)
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
            val genericName = if (e != null && e.genericName.isNotBlank()) e.genericName else t.genericNameWritten
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

    fun cleanJsonResponse(rawText: String): String {
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

    private fun formatGeminiErrorMessage(ex: Throwable): String {
        val msg = ex.message ?: ""
        return when {
            msg.contains("content must be a string", ignoreCase = true) -> {
                "The selected model does not support image analysis. Please select a vision-capable model."
            }
            msg.contains("UnknownHostException", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) || msg.contains("No address associated with hostname", ignoreCase = true) -> {
                "No internet connection or server unreachable. Please check your network connection and tap 'Analyze Prescription' to try again."
            }
            msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("quota", ignoreCase = true) -> {
                "Model is currently in high demand. Please tap 'Analyze Prescription' to try again or switch model."
            }
            msg.contains("503") || msg.contains("UNAVAILABLE", ignoreCase = true) || msg.contains("overloaded", ignoreCase = true) -> {
                "Model is currently overloaded due to high demand. Please tap 'Analyze Prescription' to try again."
            }
            msg.contains("500") || msg.contains("INTERNAL", ignoreCase = true) -> {
                "Server encountered a temporary issue. Please tap 'Analyze Prescription' to retry."
            }
            msg.contains("401") || msg.contains("UNAUTHENTICATED", ignoreCase = true) || msg.contains("API key not valid", ignoreCase = true) -> {
                "API key not valid. Please check your API key in Model Settings."
            }
            msg.contains("403") || msg.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                "Access forbidden. Please check your API key permissions."
            }
            msg.contains("400") || msg.contains("INVALID_ARGUMENT", ignoreCase = true) -> {
                "Bad request. Please check your API key or model parameters."
            }
            msg.contains("404") || msg.contains("NOT_FOUND", ignoreCase = true) -> {
                "Model not found. Please select an available model."
            }
            msg.contains("timeout", ignoreCase = true) -> {
                "Network request timed out. Please check your internet connection."
            }
            else -> msg.ifBlank { "Model is currently unavailable. Please tap 'Analyze Prescription' to try again." }
        }
    }

    companion object {
        private const val TAG = "GeminiService"
    }
}

@Serializable
internal data class OpenAiModelsListResponse(
    val data: List<OpenAiModelDto>? = null
)

@Serializable
internal data class OpenAiModelDto(
    val id: String,
    @SerialName("input_modalities") val inputModalities: List<String>? = null
)

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.0
)

@Serializable
internal data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>
)

@Serializable
internal data class OpenAiContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: OpenAiImageUrl? = null
)

@Serializable
internal data class OpenAiImageUrl(
    val url: String
)

@Serializable
internal data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiError? = null
)

@Serializable
internal data class OpenAiChoice(
    val message: OpenAiResponseMessage? = null
)

@Serializable
internal data class OpenAiResponseMessage(
    val content: String? = null
)

@Serializable
internal data class OpenAiError(
    val message: String? = null
)

@Serializable
internal data class GeminiModelsResponse(
    val models: List<ModelInfoDto>? = null
)

@Serializable
data class ModelInfoDto(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)

@Serializable
internal data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null,
    val generationConfig: GenerationConfig? = null
)

@Serializable
internal data class SystemInstruction(
    val parts: List<Part>
)

@Serializable
internal data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
internal data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
internal data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
internal data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
internal data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
internal data class Candidate(
    val content: ResponseContent? = null,
    val finishReason: String? = null
)

@Serializable
internal data class ResponseContent(
    val parts: List<ResponsePart>? = null
)

@Serializable
internal data class ResponsePart(
    val text: String? = null
)

@Serializable
internal data class GeminiTwoPassResponse(
    val transcription: List<GeminiTranscriptionDto>? = emptyList(),
    val enrichment: List<GeminiEnrichmentDto>? = emptyList()
)

@Serializable
internal data class GeminiTranscriptionDto(
    val id: Int = 1,
    @SerialName("medicine_name_written") val medicineNameWritten: String = "",
    @SerialName("generic_name_written") val genericNameWritten: String = "",
    @SerialName("dosage_form") val dosageForm: String = "",
    @SerialName("strength_value") val strengthValue: String = "",
    @SerialName("strength_unit") val strengthUnit: String = "",
    @SerialName("dose_amount") val doseAmount: String = "",
    @SerialName("dose_unit") val doseUnit: String = "",
    @SerialName("frequency_per_day") val frequencyPerDay: Int? = null,
    val schedule: String = "",
    val instructions: String = "",
    val duration: String = "",
    val units: Int? = null,
    @SerialName("unit_type") val unitType: String? = null,
    @SerialName("total_volume_needed_ml") val totalVolumeNeededMl: Double? = null,
    @SerialName("patient_age_group") val patientAgeGroup: String = "",
    val confidence: String = "high"
)

@Serializable
internal data class GeminiEnrichmentDto(
    val id: Int = 1,
    @SerialName("brand_recognized") val brandRecognized: Boolean = false,
    @SerialName("generic_name") val genericName: String = "",
    @SerialName("possible_strengths") val possibleStrengths: List<String> = emptyList(),
    @SerialName("assumed_strength") val assumedStrength: String = "",
    @SerialName("strength_source") val strengthSource: String = "",
    @SerialName("prescribed_brand_full") val prescribedBrandFull: String = "",
    @SerialName("similar_brands") val similarBrands: List<String>? = emptyList(),
    @SerialName("possible_misreading") val possibleMisreading: String = "",
    @SerialName("enrichment_confidence") val enrichmentConfidence: String = "low"
)
