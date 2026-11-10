package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class GenericSubstituteAgent : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Generic & Substitute Agent"
    override val description: String = "Cross-references DGDA generic compositions and matches alternative Bangladeshi brand substitutes"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Matching generic substitutes for ${input.size} items...", tag = TAG)

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
