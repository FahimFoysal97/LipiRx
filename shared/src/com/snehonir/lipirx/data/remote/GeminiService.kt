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
        flow: String = "App -> GeminiService.processPrescriptionImage",
        onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
    ): Result<List<MedicineItem>> = runCatching {
        val cleanModel = selectedModel.removePrefix("models/").ifBlank { "gemini-1.5-pro" }
        onProgress(0, "Encoding image & payload")
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
        onProgress(1, "Reading handwriting")
        Napier.d("[$flow] Serialized request JSON (${requestJson.length} bytes). Sending POST to $endpoint", tag = TAG)

        val responseText: String = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }.body()

        onProgress(2, "Matching brand names")
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

        onProgress(3, "Finding similar brands")
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
                prescribedBrandFull = dto.prescribedBrandFull,
                genericName = dto.genericName,
                dosageForm = dto.dosageForm,
                strengthValue = dto.strengthValue,
                strengthUnit = dto.strengthUnit,
                strengthNote = dto.strengthNote,
                doseAmount = dto.doseAmount,
                doseUnit = dto.doseUnit,
                frequencyPerDay = dto.frequencyPerDay,
                schedule = dto.schedule,
                instructions = dto.instructions,
                duration = dto.duration,
                units = qty,
                unitType = dto.unitType?.ifBlank { "pcs" } ?: "pcs",
                totalVolumeNeededMl = dto.totalVolumeNeededMl,
                patientAgeGroup = dto.patientAgeGroup,
                confidence = dto.confidence.ifBlank { "high" },
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
You are an expert Bangladeshi retail pharmacist skilled at reading handwritten doctor prescriptions and familiar with pharmaceutical products sold in Bangladesh.

TASK
Extract every prescribed medicine from the prescription image. Transcribe only what is written. Never invent, add, or substitute medicines. Ignore patient name, doctor details, and diagnosis. Do not give medical advice.

If a name is hard to read, give your best reading and set "confidence" to "low". If it cannot be read at all, set "medicine_name" to "UNREADABLE". Do not guess strengths, doses, or durations that are not written.

FIELDS (return all keys for every medicine)
- "medicine_name": string. Brand name exactly as written (e.g., "Sergel", "Fimoxyl"). Use "" if only a generic name is written.
- "prescribed_brand_full": string. The prescribed product in the same format as similar_brands: "Brand (Manufacturer) - strength". Use the brand name exactly as written on the prescription, the manufacturer of that brand in Bangladesh, and strength = the written strength, or assumed_strength if none is written. If you are not sure of the manufacturer, write the brand name and strength only. If only a generic name is written, use "" here.
- "generic_name": string. Generic name(s). For combinations, join with " + " (e.g., "Amoxicillin + Clavulanic Acid").
- "dosage_form": string. One of: "tablet", "capsule", "syrup", "suspension", "drops", "cream", "ointment", "gel", "lotion", "soap", "injection", "sachet", "inhaler", "other".
- "strength_value": string. Strength ONLY if explicitly written on the prescription (e.g., "20", "500/125", "125/5"). Use "" if not written.
- "strength_unit": string. Unit of the strength (e.g., "mg", "mg/5ml", "mg/ml", "%"). Use "" if not written.
- "strength_note": string. Write "not stated" when strength is not written. Otherwise "".
- "dose_amount": string. Amount per single intake as written (e.g., "1", "2.5", "0.5"). Use "" if not stated.
- "dose_unit": string. One of: "tablet", "capsule", "ml", "drop", "tsp", "puff", "sachet", "application", "". 
- "frequency_per_day": integer or null. Times per day (e.g., 3). null if "as needed" or not stated.
- "schedule": string. Human-readable schedule. For tablets/capsules use Bangladeshi shorthand like "1 + 0 + 1 (ac)" or "0 + 0 + 1 (pc)" (ac = before meal, pc = after meal). For liquids/drops use the written style, e.g., "1 ml × 3 times daily".
- "instructions": string. Any extra written directions (e.g., "if needed", "use during bath", "apply on affected area", "shake well"). Use "" if none.
- "duration": string. As written (e.g., "5 days", "1 month", "continue"). Use "" if not stated.
- "units": integer or null. Total pieces to dispense.
  - Tablets/capsules/sachets: doses per day × number of days (e.g., 1 + 0 + 1 for 15 days = 30). 
  - Syrup, suspension, drops, cream, ointment, soap, tube, bottle, vial: the count written, otherwise 1.
  - null if a tablet/capsule count cannot be calculated because duration or frequency is missing.
- "unit_type": string. One of: "pcs", "tablets", "capsules", "tube", "bottle", "sachet", "vial", "soap".
- "total_volume_needed_ml": number or null. Only for liquids dosed in ml: dose_amount × frequency_per_day × days. null if not a liquid, or if duration is not stated. Do not use for drops dosed in "drop" units.
- "patient_age_group": string. "infant", "child", "adult", or "" based only on what is written on the prescription (age, weight, or "baby/pediatric" notes).
- "confidence": "high", "medium", or "low".
- "similar_brands": array of 3 to 5 strings formatted "Brand (Manufacturer) - strength". Brands sold in Bangladesh with the same generic name and dosage form as the item, matching assumed_strength where possible. Always give your best suggestions when the generic is recognized; do not return an empty array just because the strength was not written or because the product is pediatric. If you can only recall 2 or 3, return those. Return [] only if the generic name is not recognized. Never invent a manufacturer; if unsure of the manufacturer, write the brand name alone.

DOSING RULES
- 1 ml, 5 ml, 2.5 ml, etc. written next to a syrup, drops, or suspension is the DOSE per intake, not the strength. Put it in "dose_amount" and "dose_unit", never in the strength fields.
- Example: "Fimoxyl - 1ml 3 times a day" means dose_amount "1", dose_unit "ml", frequency_per_day 3, schedule "1 ml × 3 times daily". Strength is not written, so strength_value "" and strength_note "not stated".
- If dose is written in "drops", keep dose_unit as "drop"; do not convert to ml.
- Do not guess the strength of pediatric products. Brand strengths vary and a wrong guess can cause dosing errors.

OUTPUT
Return a raw JSON array of objects only. No markdown fences, no comments, no extra text. If the image is not a prescription or contains no medicines, return [].
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
    @SerialName("prescribed_brand_full") val prescribedBrandFull: String = "",
    @SerialName("generic_name") val genericName: String = "",
    @SerialName("dosage_form") val dosageForm: String = "",
    @SerialName("strength_value") val strengthValue: String = "",
    @SerialName("strength_unit") val strengthUnit: String = "",
    @SerialName("strength_note") val strengthNote: String = "",
    @SerialName("dose_amount") val doseAmount: String = "",
    @SerialName("dose_unit") val doseUnit: String = "",
    @SerialName("frequency_per_day") val frequencyPerDay: Int? = null,
    @SerialName("schedule") val schedule: String = "",
    @SerialName("instructions") val instructions: String = "",
    @SerialName("duration") val duration: String = "",
    @SerialName("units") val units: Int? = null,
    @SerialName("total_quantity") val totalQuantity: Int? = null,
    @SerialName("total_tablets") val totalTablets: Int? = null,
    @SerialName("unit_type") val unitType: String? = null,
    @SerialName("total_volume_needed_ml") val totalVolumeNeededMl: Double? = null,
    @SerialName("patient_age_group") val patientAgeGroup: String = "",
    @SerialName("confidence") val confidence: String = "high",
    @SerialName("similar_brands") val similarBrands: List<String> = emptyList(),
    @SerialName("similar_brand") val similarBrand: String = ""
)
