package com.snehonir.lipirx.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class MedicineItem(
    val medicineName: String,                    // Brand name e.g. "Sergel", "Napa", "Kiton"
    val genericName: String = "",                // Generic composition e.g. "Omeprazole", "Paracetamol", "Ketoconazole (2%)"
    val strengthValue: String = "",              // Numeric value e.g. "20", "500", "50"
    val strengthUnit: String = "",               // Unit e.g. "mg", "ml", "g", "%"
    val schedule: String = "",                   // Schedule e.g. "1 + 0 + 1 (ac)"
    val unitType: String = "pcs",                // Unit type e.g. "pcs", "tablets", "capsules", "tube", "soap"
    val units: Int = 1,                          // Total quantity count e.g. 30, 1, 60
    val similarBrands: List<String> = emptyList() // List of top ~5 substitute brands e.g. ["Seclo (Square)", "Finix (Opsonin)", ...]
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
        get() = buildString {
            append(medicineName)
            if (formattedStrength.isNotBlank()) {
                append(" $formattedStrength")
            }
        }

    val formattedQuantity: String
        get() = "$units $unitType"

    val formattedSimilarBrands: String
        get() = similarBrands.joinToString(", ")
}
