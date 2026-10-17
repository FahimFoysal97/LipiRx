package com.snehonir.lipirx.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class MedicineItem(
    val medicineName: String,                    // Brand name as written, e.g. "Sergel", "Fimoxyl"
    val prescribedBrandFull: String = "",        // e.g. "Sergel (Healthcare) - 20 mg"
    val genericName: String = "",                // Generic composition e.g. "Amoxicillin"
    val dosageForm: String = "",                 // e.g. "drops", "tablet", "capsule", "soap", "syrup"
    val strengthValue: String = "",              // e.g. "20", "500/125"
    val strengthUnit: String = "",               // e.g. "mg", "mg/5ml", "%"
    val strengthNote: String = "",               // "not stated" or ""
    val doseAmount: String = "",                 // e.g. "1", "2.5"
    val doseUnit: String = "",                   // e.g. "ml", "tablet", "drop"
    val frequencyPerDay: Int? = null,            // e.g. 3, 2, 1
    val schedule: String = "",                   // e.g. "1 + 0 + 1 (ac)", "1 ml × 3 times daily"
    val instructions: String = "",               // e.g. "if needed", "use during bath"
    val duration: String = "",                   // e.g. "5 days", "1 month"
    val units: Int = 1,                          // Total pieces/bottles count e.g. 30, 1
    val unitType: String = "pcs",                // e.g. "pcs", "tablets", "capsules", "bottle", "tube"
    val totalVolumeNeededMl: Double? = null,     // e.g. 15.0
    val patientAgeGroup: String = "",            // e.g. "infant", "child", "adult"
    val confidence: String = "high",             // "high", "medium", "low"
    val similarBrands: List<String> = emptyList()// Top 5 substitute brands e.g. ["Seclo (Square)", ...]
) {
    // Backwards compatibility getters
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
