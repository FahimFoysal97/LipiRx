package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class SafetyAuditDto(
    val id: Int? = null,
    val needsReview: Boolean? = null,
    val possibleMisreading: String? = null,
    val confidence: String? = null,
    val enrichmentConfidence: String? = null,
    val auditWarning: String? = null
)

class SafetyAuditorAgent(
    private val aiService: CompositeAiService? = null,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Safety Auditor Agent"
    override val description: String = "Audits dosage safety, confidence levels, and sets review flags for pharmacist verification"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.OpenAi, "", "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.OpenAi,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = "",
        feedbackPrompt: String = ""
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Auditing ${input.size} medicine items for safety via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        var currentItems = input

        val service = aiService
        if (service != null && (apiKey.isNotBlank() || provider == AiProvider.Custom && customUrl.isNotBlank())) {
            runCatching {
                val prompt = buildString {
                    appendLine("You are a clinical patient safety auditor.")
                    if (feedbackPrompt.isNotBlank()) {
                        appendLine("SUPERVISOR INSTRUCTIONS: $feedbackPrompt")
                    }
                    appendLine("Audit the following prescription medicines for dosage safety, interaction risks, unreadable/misread brand names, and high quantity anomalies.")
                    appendLine("""Return ONLY a JSON array: [{"id": 1, "needsReview": true, "possibleMisreading": "Unclear spelling, confirm 20mg vs 40mg", "confidence": "medium", "enrichmentConfidence": "medium"}]""")
                    appendLine("Input items:")
                    input.forEachIndexed { i, item ->
                        appendLine("ID: ${i + 1}, Medicine: '${item.medicineName}', Strength: '${item.formattedStrength}', Units: ${item.units}, Recognized: ${item.brandRecognized}, Confidence: '${item.confidence}'")
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
                    val rawText = aiResult.getOrThrow()
                    val cleanedJson = cleanJson(rawText)
                    val dtos = jsonParser.decodeFromString<List<SafetyAuditDto>>(cleanedJson)
                    val dtoMap = dtos.associateBy { it.id ?: -1 }

                    currentItems = input.mapIndexed { index, item ->
                        val dto = dtoMap[index + 1]
                        if (dto != null) {
                            val llmNeedsReview = dto.needsReview ?: item.needsReview
                            val llmMisreading = dto.possibleMisreading?.takeIf { it.isNotBlank() } ?: item.possibleMisreading
                            val llmConf = dto.confidence?.takeIf { it.isNotBlank() } ?: item.confidence
                            val llmEnrichConf = dto.enrichmentConfidence?.takeIf { it.isNotBlank() } ?: item.enrichmentConfidence

                            item.copy(
                                needsReview = llmNeedsReview,
                                possibleMisreading = llmMisreading,
                                confidence = llmConf,
                                enrichmentConfidence = llmEnrichConf
                            )
                        } else {
                            item
                        }
                    }
                    Napier.d("[$agentName -> AI Success] Received LLM safety audit assessment from ${provider.displayName}", tag = TAG)
                }
            }.onFailure { ex ->
                Napier.w("[$agentName -> AI Fallback] Remote AI prompt failed: ${ex.message}. Falling back to local safety rules.", tag = TAG)
            }
        }

        // Local Rule-Based Safety Audit (Guarantee / Fallback)
        val audited = currentItems.map { item ->
            val isLowConfidence = item.confidenceEnum.isLow || item.enrichmentConfidenceEnum.isLow
            val hasMisreading = item.possibleMisreading.isNotBlank()
            val isUnrecognized = !item.brandRecognized
            val isHighVolume = item.units > 200

            val needsReview = item.needsReview || isLowConfidence || hasMisreading || isUnrecognized || isHighVolume

            item.copy(needsReview = needsReview)
        }

        val flaggedCount = audited.count { it.needsReview }
        Napier.d("[$agentName -> Result] Safety audit completed. Flagged $flaggedCount items requiring pharmacist review.", tag = TAG)
        return Result.success(audited)
    }

    private fun cleanJson(rawText: String): String {
        var trimmed = rawText.trim()
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        }
        val start = trimmed.indexOf('[')
        val end = trimmed.lastIndexOf(']')
        return if (start != -1 && end != -1 && end >= start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    companion object {
        private const val TAG = "SafetyAuditorAgent"
    }
}
