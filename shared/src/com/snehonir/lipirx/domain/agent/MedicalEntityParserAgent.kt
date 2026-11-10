package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class MedicalEntityParserAgent : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Medical Entity Parser Agent"
    override val description: String = "Parses and normalizes medicine forms, strengths, schedules, and calculated quantities"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Parsing ${input.size} medicine entities...", tag = TAG)

        val parsed = input.map { item ->
            val cleanName = item.medicineName
                .removePrefix("Tab.")
                .removePrefix("Cap.")
                .removePrefix("Syr.")
                .removePrefix("Inj.")
                .removePrefix("Supp.")
                .removePrefix("Eye Drop")
                .trim()

            val strengthNote = if (item.strengthValue.isBlank()) "not stated" else item.strengthNote
            val totalUnits = if (item.units <= 0) 1 else item.units

            item.copy(
                medicineName = cleanName,
                strengthNote = strengthNote,
                units = totalUnits
            )
        }

        Napier.d("[$agentName -> Result] Successfully parsed ${parsed.size} medical items", tag = TAG)
        return Result.success(parsed)
    }

    companion object {
        private const val TAG = "MedicalEntityParserAgent"
    }
}
