package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
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
    }

    suspend fun fetchAvailableModels(
        providedApiKey: String = "",
        flow: String = "App -> GeminiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>> = runCatching {
        val targetApiKey = providedApiKey.ifBlank { apiKey }.trim()
        if (targetApiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is required. Please enter your API key in Model Settings.")
        }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models?key=$targetApiKey"

        Napier.d("[$flow] Fetching all available models from API: $endpoint", tag = TAG)

        val responseText: String = httpClient.get(endpoint).body()
        Napier.d("[$flow] Received models response string (${responseText.length} chars)", tag = TAG)

        val modelsResponse = json.decodeFromString<GeminiModelsResponse>(responseText)
        val allContentModels = modelsResponse.models?.filter { model ->
            model.supportedGenerationMethods == null || model.supportedGenerationMethods.contains("generateContent")
        } ?: emptyList()

        Napier.d("[$flow -> Result] Found ${allContentModels.size} total models from API: ${allContentModels.map { it.name }}", tag = TAG)
        allContentModels
    }.onFailure { ex ->
        val userFriendlyMessage = formatGeminiErrorMessage(ex)
        Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
        throw IllegalStateException(userFriendlyMessage, ex)
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String = "gemini-1.5-pro",
        providedApiKey: String = "",
        mimeType: String = "image/jpeg",
        flow: String = "App -> GeminiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>> = runCatching {
        val targetApiKey = providedApiKey.ifBlank { apiKey }.trim()
        if (targetApiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is missing. Please set your API key in Model Settings.")
        }
        val cleanModel = selectedModel.removePrefix("models/").ifBlank { "gemini-1.5-pro" }
        onProgress(0, "Encoding image & payload")
        Napier.d("[$flow] Starting prescription image processing with model '$cleanModel'. Input size: ${imageByteArray.size} bytes", tag = TAG)

        val base64Image = Base64.encode(imageByteArray)
        Napier.d("[$flow] Base64 encoding complete. Payload length: ${base64Image.length} chars", tag = TAG)

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
                maxOutputTokens = 8192
            )
        )

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$targetApiKey"

        onProgress(1, "Reading handwriting")
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
        Napier.d("[$flow -> Result] Merged ${items.size} MedicineItem objects from two-pass call: $items", tag = TAG)
        items
    }.onFailure { ex ->
        val userFriendlyMessage = formatGeminiErrorMessage(ex)
        Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
        throw IllegalStateException(userFriendlyMessage, ex)
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

    private fun parseTwoPassResponse(responseText: String): GeminiTwoPassResponse {
        val response = json.decodeFromString<GeminiResponse>(responseText)

        if (response.error != null) {
            val errorMsg = response.error.message ?: "Unknown error"
            Napier.e("[parseTwoPassResponse -> Error] Gemini API returned error: $errorMsg", tag = TAG)
            throw IllegalStateException("Gemini API Error: $errorMsg")
        }

        val rawContent = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
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
            val genericName = e?.genericName?.ifBlank { t.genericNameWritten } ?: t.genericNameWritten
            val strengthValue = t.strengthValue
            val strengthUnit = t.strengthUnit
            val strengthNote = if (strengthValue.isBlank()) "not stated" else ""

            val brandRecognized = e?.brandRecognized ?: false
            val possibleStrengths = e?.possibleStrengths ?: emptyList()
            val assumedStrength = e?.assumedStrength ?: ""
            val strengthSource = e?.strengthSource ?: ""
            val prescribedBrandFull = e?.prescribedBrandFull ?: ""
            val similarBrands = e?.similarBrands ?: emptyList()
            val possibleMisreading = e?.possibleMisreading ?: ""
            val enrichmentConfidence = e?.enrichmentConfidence ?: "low"

            val unitsToDispense = t.units ?: 1
            val unitTypeString = t.unitType?.ifBlank { "pcs" } ?: "pcs"

            val needsReview = (
                t.confidence.equals("low", ignoreCase = true) ||
                medicineName == "UNREADABLE" ||
                !brandRecognized ||
                possibleMisreading.isNotBlank() ||
                strengthSource.equals("assumed", ignoreCase = true) ||
                (prescribedBrandFull.isBlank() && medicineName.isNotBlank() && medicineName != "UNREADABLE")
            )

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
                similarBrands = similarBrands,
                brandRecognized = brandRecognized,
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
        return trimmed
    }

    private fun formatGeminiErrorMessage(ex: Throwable): String {
        val msg = ex.message ?: ""
        return when {
            msg.contains("401") || msg.contains("UNAUTHENTICATED", ignoreCase = true) || msg.contains("API key not valid", ignoreCase = true) -> {
                "API key not valid. Please enter a valid Gemini API Key from Google AI Studio."
            }
            msg.contains("403") || msg.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                "Access forbidden. Please check your Gemini API key permissions."
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
            else -> msg.ifBlank { "An unexpected error occurred while communicating with Gemini API." }
        }
    }

    companion object {
        private const val TAG = "GeminiService"
    }
}

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
    val content: ResponseContent? = null
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
