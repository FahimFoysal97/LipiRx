package com.snehonir.lipirx.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class MedicineItem(
    val medicineName: String,                    // Mapped from transcription medicine_name_written
    val prescribedBrandFull: String = "",        // Mapped from enrichment prescribed_brand_full
    val genericName: String = "",                // Mapped from enrichment generic_name or transcription generic_name_written
    val dosageForm: String = "",                 // From transcription dosage_form
    val strengthValue: String = "",              // From transcription strength_value
    val strengthUnit: String = "",               // From transcription strength_unit
    val strengthNote: String = "",               // "not stated" if strength_value is empty, else ""
    val doseAmount: String = "",                 // From transcription dose_amount
    val doseUnit: String = "",                   // From transcription dose_unit
    val frequencyPerDay: Int? = null,            // From transcription frequency_per_day
    val schedule: String = "",                   // From transcription schedule
    val instructions: String = "",               // From transcription instructions
    val duration: String = "",                   // From transcription duration
    val units: Int = 1,                          // From transcription units
    val unitType: String = "pcs",                // From transcription unit_type
    val totalVolumeNeededMl: Double? = null,     // From transcription total_volume_needed_ml
    val patientAgeGroup: String = "",            // From transcription patient_age_group
    val confidence: String = "high",             // From transcription confidence ("high", "medium", "low")
    val similarBrands: List<String> = emptyList(),// From enrichment similar_brands
    val brandRecognized: Boolean = false,        // From enrichment brand_recognized
    val possibleStrengths: List<String> = emptyList(), // From enrichment possible_strengths
    val assumedStrength: String = "",            // From enrichment assumed_strength
    val strengthSource: String = "",             // From enrichment strength_source ("written", "assumed", "")
    val possibleMisreading: String = "",         // From enrichment possible_misreading
    val enrichmentConfidence: String = "low",     // From enrichment enrichment_confidence ("high", "medium", "low")
    val needsReview: Boolean = false             // Computed review flag
) {
    // Backwards compatibility getters
    val totalTablets: Int get() = units
    val similarBrand: String get() = formattedSimilarBrands

    val formattedStrength: String
        get() = buildString {
            if (strengthValue.isNotBlank()) append(strengthValue)
            if (strengthUnit.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(strengthUnit)
            }
        }

    val formattedFullName: String
        get() = prescribedBrandFull.ifBlank {
            buildString {
                append(medicineName)
                if (formattedStrength.isNotBlank()) {
                    append(" $formattedStrength")
                }
            }
        }

    val formattedQuantity: String
        get() = "$units $unitType"

    val formattedSimilarBrands: String
        get() = similarBrands.joinToString(", ")
}
