package com.snehonir.lipirx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
