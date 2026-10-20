package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

class GeminiAiService(
    private val httpClient: HttpClient,
    private val defaultApiKey: String = "",
    private val jsonParser: Json
) : PrescriptionAiService {

    override val provider: AiProvider = AiProvider.Gemini

    override suspend fun fetchAvailableModels(
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<List<ModelInfoDto>> {
        return try {
            val targetApiKey = providedApiKey.ifBlank { defaultApiKey }.trim()
            if (targetApiKey.isBlank()) {
                throw IllegalStateException("API key is required for ${provider.displayName}. Please enter your API key in Model Settings.")
            }

            val endpoint = "${provider.defaultEndpoint}?key=$targetApiKey"
            Napier.d("[$flow] Dynamically fetching online models from Gemini server: $endpoint", tag = TAG)

            val responseText: String = httpClient.get(endpoint).body()
            Napier.d("[$flow -> Gemini Models RawResponse] Raw models response:\n$responseText", tag = TAG)

            val modelsResponse = jsonParser.decodeFromString<GeminiModelsResponse>(responseText)
            val modelsList = modelsResponse.models?.filter { model ->
                val cleanName = model.name.lowercase()
                val supportsGenerate = model.supportedGenerationMethods != null && model.supportedGenerationMethods.contains("generateContent")
                val isNonVisionType = cleanName.contains("tts") || cleanName.contains("lyria") || cleanName.contains("embedding") || cleanName.contains("aqa")
                supportsGenerate && !isNonVisionType
            } ?: emptyList()

            Napier.d("[$flow -> Result] Found ${modelsList.size} dynamic online Vision models from Gemini server: ${modelsList.map { it.name }}", tag = TAG)
            Result.success(modelsList)
        } catch (ex: Throwable) {
            val userFriendlyMessage = formatErrorMessage(ex)
            Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userFriendlyMessage, ex))
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
            if (targetApiKey.isBlank()) {
                throw IllegalStateException("Gemini API key is missing. Please set your API key in Model Settings.")
            }
            val cleanModel = selectedModel.removePrefix("models/").ifBlank { provider.defaultModel }
            onProgress(0, "Encoding image & payload")
            Napier.d("[$flow] Starting processing: provider=${provider.id}, model=$cleanModel, size=${imageByteArray.size} bytes", tag = TAG)

            val base64Image = Base64.encode(imageByteArray)

            onProgress(1, "Reading handwriting")
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
            var rawResponseText = executeGeminiPostCall(endpoint, requestPayload, flow)
            var parseResult = runCatching { parseTwoPassResponse(rawResponseText) }

            if (parseResult.isFailure) {
                Napier.w("[$flow -> Retry] Parsing failed on first attempt: ${parseResult.exceptionOrNull()?.message}. Retrying request once...", tag = TAG)
                onProgress(1, "Retrying Vision AI request...")
                rawResponseText = executeGeminiPostCall(endpoint, requestPayload, flow)
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
            val userFriendlyMessage = formatErrorMessage(ex)
            Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
            Result.failure(IllegalStateException(userFriendlyMessage, ex))
        }
    }

    private suspend fun executeGeminiPostCall(
        endpoint: String,
        requestPayload: GeminiRequest,
        flow: String
    ): String {
        val requestJson = jsonParser.encodeToString(requestPayload)
        Napier.d("[$flow] Serialized request JSON (${requestJson.length} bytes). Sending POST to $endpoint", tag = TAG)

        val responseText: String = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }.body()

        Napier.d("[$flow -> RawResponse] Gemini REST API Raw Response:\n$responseText", tag = TAG)
        return responseText
    }

    private fun parseTwoPassResponse(responseText: String): GeminiTwoPassResponse {
        val response = jsonParser.decodeFromString<GeminiResponse>(responseText)

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
        return jsonParser.decodeFromString<GeminiTwoPassResponse>(cleanedJson)
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

    private fun formatErrorMessage(ex: Throwable): String {
        val msg = ex.message ?: ""
        return when {
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
        private const val TAG = "GeminiAiService"
    }
}
