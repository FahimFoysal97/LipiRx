package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class MedicalEntityParserAgent(
    private val aiService: CompositeAiService? = null
) : RxAgent<List<MedicineItem>, List<MedicineItem>> {

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
        customUrl: String = "",
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Parsing ${input.size} medicine entities via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        val service = aiService
        if (service != null && (apiKey.isNotBlank() || provider == AiProvider.Custom && customUrl.isNotBlank())) {
            runCatching {
                val prompt = buildString {
                    appendLine("You are a medical entity parser. Given the following medicine items extracted from a prescription, clean up and normalize their dosage forms, strengths, and schedule notes.")
                    appendLine("Return a JSON array of objects with fields: medicineName, strengthNote, units.")
                    appendLine("Input items:")
                    input.forEachIndexed { i, item ->
                        appendLine("$i: Name='${item.medicineName}', Strength='${item.formattedStrength}', Units=${item.units}")
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
                    Napier.d("[$agentName -> AI Success] Received LLM entity parsing enrichment from ${provider.displayName}", tag = TAG)
                }
            }.onFailure { ex ->
                Napier.w("[$agentName -> AI Fallback] Remote AI prompt failed: ${ex.message}. Falling back to local rules.", tag = TAG)
            }
        }

        // Local Rule-Based Normalization (Fallback / Guarantee)
        val parsed = input.map { item ->
            val cleanName = item.medicineName.replace(PrefixRegex, "").trim()
            val strengthNote = if (item.strengthValue.isBlank()) "not stated" else item.strengthNote
            val totalUnits = if (item.units <= 0) 1 else item.units

            item.copy(
                medicineName = cleanName,
                strengthNote = strengthNote,
                units = totalUnits,
            )
        }

        Napier.d("[$agentName -> Result] Successfully parsed ${parsed.size} medical items", tag = TAG)
        return Result.success(parsed)
    }

    companion object {
        private const val TAG = "MedicalEntityParserAgent"
        private val PrefixRegex = Regex("(?i)^(Tab|Cap|Syr|Inj|Supp|Eye Drop)\\.?\\s*")
    }
}
