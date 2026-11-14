package com.snehonir.lipirx.domain.eval

import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.MedicineItem
import kotlinx.serialization.Serializable

@Serializable
data class ExpectedMedicine(
    val brandName: String,
    val expectedStrength: String = "",
    val expectedGeneric: String = ""
)

data class RxEvalTestCase(
    val id: String,
    val description: String,
    val input: TranscriptionInput,
    val initialItems: List<MedicineItem> = emptyList(),
    val expectedItemsCount: Int,
    val expectedMedicines: List<ExpectedMedicine>,
    val maxAllowedTurns: Int = 6,
    val allowPharmacistSuspension: Boolean = true
)
