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
        return execute(input, AiProvider.OpenAi, "", "", "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.OpenAi,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = "",
        feedbackPrompt: String = "",
        episodicMemory: String = ""
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Auditing ${input.size} medicine items for safety via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        val aiAuditedItems = if (canExecuteAiService(provider, apiKey, customUrl)) {
            executeAiAuditLoop(input, provider, selectedModel, apiKey, customUrl, feedbackPrompt, episodicMemory) ?: input
        } else {
            input
        }

        val finalAudited = applyLocalSafetyRules(aiAuditedItems)
        val flaggedCount = finalAudited.count { it.needsReview }
        Napier.d("[$agentName -> Result] Safety audit completed. Flagged $flaggedCount items requiring pharmacist review.", tag = TAG)
        return Result.success(finalAudited)
    }

    private fun canExecuteAiService(provider: AiProvider, apiKey: String, customUrl: String): Boolean {
        return aiService != null && (apiKey.isNotBlank() || (provider == AiProvider.Custom && customUrl.isNotBlank()))
    }

    private suspend fun executeAiAuditLoop(
        input: List<MedicineItem>,
        provider: AiProvider,
        selectedModel: String,
        apiKey: String,
        customUrl: String,
        feedbackPrompt: String,
        episodicMemory: String
    ): List<MedicineItem>? {
        val service = aiService ?: return null
        val initialPrompt = buildPrompt(input, feedbackPrompt, episodicMemory)
        var currentPrompt = initialPrompt
        var attempts = 0
        val maxAttempts = 2

        while (attempts < maxAttempts) {
            val aiResult = service.processTextPrompt(
                promptText = currentPrompt,
                selectedModel = selectedModel,
                providedApiKey = apiKey,
                provider = provider,
                customUrl = customUrl,
                flow = "Agent -> $agentName (Attempt ${attempts + 1})"
            )

            if (aiResult.isFailure) break

            val rawText = aiResult.getOrThrow()
            val parseResult = parseAndApplyDtos(rawText, input)

            if (parseResult.isSuccess) {
                Napier.d("[$agentName -> AI Success] Received LLM safety audit assessment on attempt ${attempts + 1}", tag = TAG)
                return parseResult.getOrThrow()
            }

            attempts++
            val parseError = parseResult.exceptionOrNull()?.message ?: "JSON parse error"
            Napier.w("[$agentName -> Self-Correction Attempt $attempts] JSON validation error: $parseError. Triggering correction loop...", tag = TAG)
            currentPrompt = buildCorrectionPrompt(initialPrompt, parseError)
        }

        return null
    }

    private fun parseAndApplyDtos(rawText: String, input: List<MedicineItem>): Result<List<MedicineItem>> {
        val cleanedJson = cleanJson(rawText)
        val decodedResult = runCatching { jsonParser.decodeFromString<List<SafetyAuditDto>>(cleanedJson) }

        if (decodedResult.isFailure) {
            return Result.failure(decodedResult.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        }

        val dtos = decodedResult.getOrThrow()
        val dtoMap = dtos.associateBy { it.id ?: -1 }

        val updatedItems = input.mapIndexed { index, item ->
            val dto = dtoMap[index + 1] ?: return@mapIndexed item
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
        }

        return Result.success(updatedItems)
    }

    private fun applyLocalSafetyRules(items: List<MedicineItem>): List<MedicineItem> {
        return items.map { item ->
            val isLowConfidence = item.confidenceEnum.isLow || item.enrichmentConfidenceEnum.isLow
            val hasMisreading = item.possibleMisreading.isNotBlank()
            val isUnrecognized = !item.brandRecognized
            val isHighVolume = item.units > 200

            val needsReview = item.needsReview || isLowConfidence || hasMisreading || isUnrecognized || isHighVolume

            item.copy(needsReview = needsReview)
        }
    }

    private fun buildPrompt(input: List<MedicineItem>, feedbackPrompt: String, episodicMemory: String): String = buildString {
        appendLine("You are a clinical patient safety auditor.")
        if (feedbackPrompt.isNotBlank()) {
            appendLine("SUPERVISOR INSTRUCTIONS: $feedbackPrompt")
        }
        if (episodicMemory.isNotBlank()) {
            appendLine(episodicMemory)
            appendLine("NOTE: Check for dosage anomalies or drug conflicts compared to patient past dispensing history.")
        }
        appendLine("Audit the following prescription medicines for dosage safety, interaction risks, unreadable/misread brand names, and high quantity anomalies.")
        appendLine("""Return ONLY a JSON array: [{"id": 1, "needsReview": true, "possibleMisreading": "Unclear spelling, confirm 20mg vs 40mg", "confidence": "medium", "enrichmentConfidence": "medium"}]""")
        appendLine("Input items:")
        input.forEachIndexed { i, item ->
            appendLine("ID: ${i + 1}, Medicine: '${item.medicineName}', Strength: '${item.formattedStrength}', Units: ${item.units}, Recognized: ${item.brandRecognized}, Confidence: '${item.confidence}'")
        }
    }

    private fun buildCorrectionPrompt(initialPrompt: String, parseError: String): String = buildString {
        appendLine(initialPrompt)
        appendLine()
        appendLine("CRITICAL FIX REQUIRED: Your previous response failed JSON schema validation with error: '$parseError'.")
        appendLine("Fix the JSON syntax and return ONLY strictly valid JSON matching the exact required schema.")
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
