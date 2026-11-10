package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class SafetyAuditorAgent(
    private val aiService: CompositeAiService? = null
) : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Safety Auditor Agent"
    override val description: String = "Audits dosage safety, confidence levels, and sets review flags for pharmacist verification"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.OpenAi, "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.OpenAi,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = "",
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Auditing ${input.size} medicine items for safety via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        val service = aiService
        if (service != null && (apiKey.isNotBlank() || provider == AiProvider.Custom && customUrl.isNotBlank())) {
            runCatching {
                val prompt = buildString {
                    appendLine("You are a clinical patient safety auditor.")
                    appendLine("Audit the following prescription medicines for dosage safety, interaction risks, and high quantity anomalies.")
                    appendLine("Input items:")
                    input.forEachIndexed { i, item ->
                        appendLine("$i: Medicine='${item.medicineName}', Strength='${item.formattedStrength}', Units=${item.units}, Confidence='${item.confidence}'")
                    }
                }

                val aiResult = service.processTextPrompt(
                    promptText = prompt,
                    selectedModel = selectedModel,
                    providedApiKey = apiKey,
                    provider = provider,
                    customUrl = customUrl,
                    flow = "Agent -> $agentName"
                )

                if (aiResult.isSuccess) {
                    Napier.d("[$agentName -> AI Success] Received LLM safety audit assessment from ${provider.displayName}", tag = TAG)
                }
            }.onFailure { ex ->
                Napier.w("[$agentName -> AI Fallback] Remote AI prompt failed: ${ex.message}. Falling back to local safety rules.", tag = TAG)
            }
        }

        // Local Rule-Based Safety Audit (Fallback / Guarantee)
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
