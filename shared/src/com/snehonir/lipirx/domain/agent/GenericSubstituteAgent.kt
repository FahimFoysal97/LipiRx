package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class GenericSubstituteAgent(
    private val aiService: CompositeAiService? = null
) : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Generic & Substitute Agent"
    override val description: String = "Cross-references DGDA generic compositions and matches alternative Bangladeshi brand substitutes"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.Groq, "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.Groq,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = "",
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Matching generic substitutes for ${input.size} items via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        val service = aiService
        if (service != null && (apiKey.isNotBlank() || provider == AiProvider.Custom && customUrl.isNotBlank())) {
            runCatching {
                val prompt = buildString {
                    appendLine("You are a pharmaceutical substitute finder for Bangladeshi (DGDA) pharmaceutical brands.")
                    appendLine("For each of the following medicine generics/brands, suggest 2-3 popular Bangladeshi alternative brand substitutes.")
                    appendLine("Input items:")
                    input.forEachIndexed { i, item ->
                        appendLine("$i: Medicine='${item.medicineName}', Generic='${item.genericName}'")
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
                    Napier.d("[$agentName -> AI Success] Received LLM generic substitute enrichment from ${provider.displayName}", tag = TAG)
                }
            }.onFailure { ex ->
                Napier.w("[$agentName -> AI Fallback] Remote AI prompt failed: ${ex.message}. Falling back to local rules.", tag = TAG)
            }
        }

        // Local Rule-Based Substitute Enrichment (Fallback / Guarantee)
        val enriched = input.map { item ->
            if (item.similarBrands.isEmpty() && item.genericName.isNotBlank()) {
                val defaultSubstitutes = findSubstitutesForGeneric(item.genericName)
                item.copy(similarBrands = defaultSubstitutes)
            } else {
                item
            }
        }

        Napier.d("[$agentName -> Result] Enriched ${enriched.size} items with generic substitute mappings", tag = TAG)
        return Result.success(enriched)
    }

    private fun findSubstitutesForGeneric(genericName: String): List<String> {
        val clean = genericName.lowercase().trim()
        return when {
            clean.contains("esomeprazole") -> listOf("Seclo", "Maxpro", "Nexum")
            clean.contains("omeprazole") -> listOf("Seclo", "Losectil")
            clean.contains("paracetamol") -> listOf("Napa", "Ace", "Pyrex")
            clean.contains("cefixime") -> listOf("Cef-3", "Denvar")
            clean.contains("azithromycin") -> listOf("Zimax", "Zithrin")
            clean.contains("montelukast") -> listOf("Monas", "Provair")
            else -> emptyList()
        }
    }

    companion object {
        private const val TAG = "GenericSubstituteAgent"
    }
}
