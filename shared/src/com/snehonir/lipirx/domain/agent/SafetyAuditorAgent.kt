package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class SafetyAuditorAgent : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Safety Auditor Agent"
    override val description: String = "Audits dosage safety, confidence levels, and sets review flags for pharmacist verification"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.OpenAi, "", "", "")
    }

    fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.OpenAi,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = ""
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Auditing ${input.size} medicine items for safety via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        val audited = input.map { item ->
            val isLowConfidence = item.confidenceEnum.isLow || item.enrichmentConfidenceEnum.isLow
            val hasMisreading = item.possibleMisreading.isNotBlank()
            val isUnrecognized = !item.brandRecognized
            val isHighVolume = item.units > 200

            val needsReview = isLowConfidence || hasMisreading || isUnrecognized || isHighVolume

            item.copy(needsReview = needsReview)
        }

        val flaggedCount = audited.count { it.needsReview }
        Napier.d("[$agentName -> Result] Safety audit completed. Flagged $flaggedCount items requiring pharmacist review.", tag = TAG)
        return Result.success(audited)
    }

    companion object {
        private const val TAG = "SafetyAuditorAgent"
    }
}
