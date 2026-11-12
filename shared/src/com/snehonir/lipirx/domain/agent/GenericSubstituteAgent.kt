package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class GenericSubstituteDto(
    val id: Int? = null,
    val genericName: String? = null,
    val similarBrands: List<String>? = null
)

class GenericSubstituteAgent(
    private val aiService: CompositeAiService? = null,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Generic & Substitute Agent"
    override val description: String = "Cross-references DGDA generic compositions and matches alternative Bangladeshi brand substitutes"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.Groq, "", "", "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.Groq,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = "",
        feedbackPrompt: String = "",
        episodicMemory: String = ""
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Matching generic substitutes for ${input.size} items via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        val aiEnrichedItems = if (canExecuteAiService(provider, apiKey, customUrl)) {
            executeAiEnrichmentLoop(input, provider, selectedModel, apiKey, customUrl, feedbackPrompt, episodicMemory) ?: input
        } else {
            input
        }

        val finalEnriched = applyLocalFallbackEnrichment(aiEnrichedItems)
        Napier.d("[$agentName -> Result] Enriched ${finalEnriched.size} items with generic substitute mappings", tag = TAG)
        return Result.success(finalEnriched)
    }

    private fun canExecuteAiService(provider: AiProvider, apiKey: String, customUrl: String): Boolean {
        return aiService != null && (apiKey.isNotBlank() || (provider == AiProvider.Custom && customUrl.isNotBlank()))
    }

    private suspend fun executeAiEnrichmentLoop(
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
                Napier.d("[$agentName -> AI Success] Received LLM generic substitute enrichment on attempt ${attempts + 1}", tag = TAG)
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
        val decodedResult = runCatching { jsonParser.decodeFromString<List<GenericSubstituteDto>>(cleanedJson) }

        if (decodedResult.isFailure) {
            return Result.failure(decodedResult.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        }

        val dtos = decodedResult.getOrThrow()
        val dtoMap = dtos.associateBy { it.id ?: -1 }

        val updatedItems = input.mapIndexed { index, item ->
            val dto = dtoMap[index + 1] ?: return@mapIndexed item
            val newGeneric = dto.genericName?.takeIf { it.isNotBlank() } ?: item.genericName
            val newSubstitutes = dto.similarBrands?.filter { it.isNotBlank() }?.ifEmpty { item.similarBrands } ?: item.similarBrands
            item.copy(
                genericName = newGeneric,
                similarBrands = newSubstitutes
            )
        }

        return Result.success(updatedItems)
    }

    private fun applyLocalFallbackEnrichment(items: List<MedicineItem>): List<MedicineItem> {
        return items.map { item ->
            if (item.similarBrands.isEmpty() && item.genericName.isNotBlank()) {
                val defaultSubstitutes = findSubstitutesForGeneric(item.genericName)
                item.copy(similarBrands = defaultSubstitutes)
            } else {
                item
            }
        }
    }

    private fun findSubstitutesForGeneric(genericName: String): List<String> {
        val clean = genericName.lowercase().trim()
        return when {
            clean.contains("esomeprazole") -> listOf("Seclo (Square)", "Maxpro (Renata)", "Nexum (Incepta)")
            clean.contains("omeprazole") -> listOf("Seclo (Square)", "Losectil (Eskayef)")
            clean.contains("paracetamol") -> listOf("Napa (Beximco)", "Ace (Square)", "Pyrex (Incepta)")
            clean.contains("cefixime") -> listOf("Cef-3 (Square)", "Denvar (Incepta)")
            clean.contains("azithromycin") -> listOf("Zimax (Square)", "Zithrin (Beximco)")
            clean.contains("montelukast") -> listOf("Monas (Pharmaasia)", "Provair (UniMed)")
            else -> emptyList()
        }
    }

    private fun buildPrompt(input: List<MedicineItem>, feedbackPrompt: String, episodicMemory: String): String = buildString {
        appendLine("You are a pharmaceutical substitute finder for Bangladeshi (DGDA) pharmaceutical brands.")
        if (feedbackPrompt.isNotBlank()) {
            appendLine("SUPERVISOR INSTRUCTIONS: $feedbackPrompt")
        }
        if (episodicMemory.isNotBlank()) {
            appendLine(episodicMemory)
            appendLine("NOTE: Prioritize previously dispensed Bangladeshi brand substitutes if matching generics.")
        }
        appendLine("For each of the following medicine generics/brands, identify its active generic composition (genericName) and suggest 3 to 5 popular Bangladeshi alternative brand substitutes with manufacturer (similarBrands).")
        appendLine("""Return ONLY a JSON array: [{"id": 1, "genericName": "Esomeprazole", "similarBrands": ["Seclo (Square)", "Maxpro (Renata)", "Nexum (Incepta)"]}]""")
        appendLine("Input items:")
        input.forEachIndexed { i, item ->
            appendLine("ID: ${i + 1}, Medicine: '${item.medicineName}', Written Generic: '${item.genericName}'")
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
        private const val TAG = "GenericSubstituteAgent"
    }
}
