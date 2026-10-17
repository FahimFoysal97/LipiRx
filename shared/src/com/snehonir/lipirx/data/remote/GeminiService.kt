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
        flow: String = "App -> GeminiService.fetchAvailableModels"
    ): Result<List<ModelInfoDto>> = runCatching {
        val targetApiKey = apiKey.ifBlank { DEFAULT_API_KEY }
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
        Napier.e("[$flow -> Exception] Failed to fetch available models: ${ex.message}", throwable = ex, tag = TAG)
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String = "gemini-1.5-pro",
        mimeType: String = "image/jpeg",
        flow: String = "App -> GeminiService.processPrescriptionImage"
    ): Result<List<MedicineItem>> = runCatching {
        val cleanModel = selectedModel.removePrefix("models/").ifBlank { "gemini-1.5-pro" }
        Napier.d("[$flow] Starting prescription image processing with model '$cleanModel'. Input size: ${imageByteArray.size} bytes", tag = TAG)

        val base64Image = Base64.encode(imageByteArray)
        Napier.d("[$flow] Base64 encoding complete. Payload length: ${base64Image.length} chars", tag = TAG)

        val requestPayload = GeminiRequest(
            systemInstruction = SystemInstruction(
                parts = listOf(Part(text = SYSTEM_PROMPT))
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
                responseMimeType = "application/json"
            )
        )

        val targetApiKey = apiKey.ifBlank { DEFAULT_API_KEY }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$targetApiKey"

        val requestJson = json.encodeToString(requestPayload)
        Napier.d("[$flow] Serialized request JSON (${requestJson.length} bytes). Sending POST to $endpoint", tag = TAG)

        val responseText: String = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }.body()

        Napier.d("[$flow] Received raw response from Gemini REST API (${responseText.length} chars)", tag = TAG)

        val response = json.decodeFromString<GeminiResponse>(responseText)

        if (response.error != null) {
            val errorMsg = response.error.message ?: "Unknown error"
            Napier.e("[$flow -> Error] Gemini API returned error: $errorMsg", tag = TAG)
            throw IllegalStateException("Gemini API Error: $errorMsg")
        }

        val rawContent = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: throw IllegalStateException("No text response received from Gemini API.")

        Napier.d("[$flow] Gemini model extracted raw JSON content: $rawContent", tag = TAG)

        val cleanedJson = cleanJsonResponse(rawContent)
        val dtos = json.decodeFromString<List<GeminiMedicineDto>>(cleanedJson)
        Napier.d("[$flow] Successfully parsed ${dtos.size} DTO items from Gemini output", tag = TAG)

        val items = dtos.map { dto ->
            val qty = dto.units ?: dto.totalQuantity ?: dto.totalTablets ?: 1
            val brandsList = when {
                dto.similarBrands.isNotEmpty() -> dto.similarBrands
                dto.similarBrand.isNotBlank() -> dto.similarBrand.split(",").map { it.trim() }.filter { it.isNotBlank() }
                else -> emptyList()
            }

            MedicineItem(
                medicineName = dto.medicineName,
                genericName = dto.genericName,
                strengthValue = dto.strengthValue,
                strengthUnit = dto.strengthUnit,
                schedule = dto.schedule,
                unitType = dto.unitType?.ifBlank { "pcs" } ?: "pcs",
                units = qty,
                similarBrands = brandsList
            )
        }
        Napier.d("[$flow -> Result] Converted ${items.size} MedicineItem objects: $items", tag = TAG)
        items
    }.onFailure { ex ->
        val userFriendlyMessage = when {
            ex.message?.contains("timeout", ignoreCase = true) == true -> "Network request timed out. Please check your internet connection or select a faster model."
            ex.message?.contains("400") == true || ex.message?.contains("404") == true -> "Selected model '$selectedModel' returned HTTP error: ${ex.message}"
            else -> ex.message ?: "Failed to process prescription image."
        }
        Napier.e("[$flow -> Exception] $userFriendlyMessage", throwable = ex, tag = TAG)
    }

    private fun cleanJsonResponse(rawText: String): String {
        var trimmed = rawText.trim()
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        }
        return trimmed
    }

    companion object {
        private const val TAG = "GeminiService"
        private const val DEFAULT_API_KEY = "AQ.Ab8RN6LYU1DTPVLgNK5-nEgpGNVhQscxmYmQyBnzR1e54-NsVw"

        private const val SYSTEM_PROMPT = """
You are an expert Bangladeshi retail pharmacist specializing in reading handwritten doctor prescriptions and knowledge of pharmaceutical products in Bangladesh.
Analyze the provided prescription image carefully and extract all prescribed medicines with maximum accuracy.

For each medicine, extract:
1. "medicine_name": Brand name only (e.g., "Sergel", "Napa", "Kiton", "Seclo", "Ace").
2. "generic_name": Generic name and chemical composition (e.g., "Omeprazole", "Paracetamol", "Ketoconazole (2%)").
3. "strength_value": Numeric strength, weight, or volume (e.g., "20", "500", "50", "100"). Leave "" if none.
4. "strength_unit": Unit for strength/volume/weight (e.g., "mg", "ml", "g", "%"). Leave "" if none.
5. "schedule": Dosage schedule in local Bangladeshi shorthand (e.g., "1 + 0 + 1 (ac)", "1 + 1 + 1 (pc)", "0 + 0 + 1", "2 times daily", "Use during bath"). Use 'ac' for before meals and 'pc' for after meals.
6. "unit_type": Prescribed unit classification (e.g., "pcs", "tablets", "capsules", "tube", "bottle", "soap", "sachet", "vial"). Default to "pcs".
7. "units": Total calculated integer count of prescribed units (e.g., 30, 1, 20, 60). Default to 1 if count is 1 or unstated.
8. "similar_brands": A JSON array of EXACTLY 5 top-selling, widely available alternative Bangladeshi pharmaceuticals for that exact generic composition & strength, including brand name and manufacturer in parentheses (e.g., ["Seclo (Square)", "Finix (Opsonin)", "Xeldrin (Incepta)", "Ometid (Renata)", "Proceptin (Beximco)"]).

Output MUST be a raw JSON array of objects with keys: "medicine_name", "generic_name", "strength_value", "strength_unit", "schedule", "unit_type", "units", "similar_brands".
Do NOT wrap in markdown or add extra text.
"""
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
    val responseMimeType: String? = null
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
internal data class GeminiMedicineDto(
    @SerialName("medicine_name") val medicineName: String = "",
    @SerialName("generic_name") val genericName: String = "",
    @SerialName("strength_value") val strengthValue: String = "",
    @SerialName("strength_unit") val strengthUnit: String = "",
    @SerialName("schedule") val schedule: String = "",
    @SerialName("unit_type") val unitType: String? = null,
    @SerialName("units") val units: Int? = null,
    @SerialName("total_quantity") val totalQuantity: Int? = null,
    @SerialName("total_tablets") val totalTablets: Int? = null,
    @SerialName("similar_brands") val similarBrands: List<String> = emptyList(),
    @SerialName("similar_brand") val similarBrand: String = ""
)
