package com.snehonir.lipirx.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelInfoDto(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)

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
