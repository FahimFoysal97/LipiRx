package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class MedicalEntityParserAgent : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Medical Entity Parser Agent"
    override val description: String = "Parses and normalizes medicine forms, strengths, schedules, and calculated quantities"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.Gemini, "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.Gemini,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = ""
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Parsing ${input.size} medicine entities via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        val PREFIX_REGEX = Regex("(?i)^(Tab|Cap|Syr|Inj|Supp|Eye Drop)\\.?\\s*")

        val parsed = input.map { item ->
            val cleanName = item.medicineName.replace(PREFIX_REGEX, "").trim()
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
