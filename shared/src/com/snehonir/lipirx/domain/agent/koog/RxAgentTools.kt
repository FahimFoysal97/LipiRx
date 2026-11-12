package com.snehonir.lipirx.domain.agent.koog

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ToolExecutionParams(
    val provider: String = "gemini",
    val model: String = "",
    val apiKey: String = "",
    val customUrl: String = "",
    val feedbackPrompt: String = "",
    val memoryContext: String = ""
)

@Serializable
internal data class ParsedEntityDto(
    val id: Int? = null,
    val medicineName: String? = null,
    val dosageForm: String? = null,
    val strengthValue: String? = null,
    val strengthUnit: String? = null,
    val strengthNote: String? = null,
    val schedule: String? = null,
    val units: Int? = null,
    val unitType: String? = null
)

@Serializable
internal data class GenericSubstituteDto(
    val id: Int? = null,
    val genericName: String? = null,
    val similarBrands: List<String>? = null
)

@Serializable
internal data class SafetyAuditDto(
    val id: Int? = null,
    val needsReview: Boolean? = null,
    val possibleMisreading: String? = null,
    val confidence: String? = null,
    val enrichmentConfidence: String? = null,
    val auditWarning: String? = null
)

class RxAgentTools(
    private val aiService: CompositeAiService,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    suspend fun executeVisionOCR(input: TranscriptionInput): Result<List<MedicineItem>> {
        Napier.d("[RxAgentTools] Invoking Vision OCR tool via ${input.provider.displayName}...", tag = TAG)
        return aiService.processPrescriptionImage(
            imageByteArray = input.imageByteArray,
            selectedModel = input.selectedModel,
            providedApiKey = input.providedApiKey,
            provider = input.provider,
            customUrl = input.customUrl,
            mimeType = input.mimeType,
            flow = "Koog -> VisionOCRTool",
            onProgress = input.onProgress
        )
    }

    suspend fun executeEntityParsing(
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        Napier.d("[RxAgentTools] Invoking Medical Entity Parsing tool for ${items.size} items...", tag = TAG)
        if (items.isEmpty()) return Result.success(emptyList())

        val providerObj = AiProvider.fromId(params.provider)
        val aiParsedItems = if (canExecute(providerObj, params.apiKey, params.customUrl)) {
            executeEntityParsingLoop(items, providerObj, params) ?: items
        } else {
            items
        }

        val finalParsed = applyLocalEntityNormalizations(aiParsedItems)
        return Result.success(finalParsed)
    }

    suspend fun executeGenericSubstituteSearch(
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        Napier.d("[RxAgentTools] Invoking Generic Substitute Search tool for ${items.size} items...", tag = TAG)
        if (items.isEmpty()) return Result.success(emptyList())

        val providerObj = AiProvider.fromId(params.provider)
        val aiEnriched = if (canExecute(providerObj, params.apiKey, params.customUrl)) {
            executeGenericLoop(items, providerObj, params) ?: items
        } else {
            items
        }

        val finalEnriched = applyLocalSubstituteFallback(aiEnriched)
        return Result.success(finalEnriched)
    }

    suspend fun executeSafetyAudit(
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        Napier.d("[RxAgentTools] Invoking Patient Safety Audit tool for ${items.size} items...", tag = TAG)
        if (items.isEmpty()) return Result.success(emptyList())

        val providerObj = AiProvider.fromId(params.provider)
        val aiAudited = if (canExecute(providerObj, params.apiKey, params.customUrl)) {
            executeSafetyLoop(items, providerObj, params) ?: items
        } else {
            items
        }

        val finalAudited = applyLocalSafetyRules(aiAudited)
        return Result.success(finalAudited)
    }

    private fun canExecute(provider: AiProvider, apiKey: String, customUrl: String): Boolean {
        return apiKey.isNotBlank() || (provider == AiProvider.Custom && customUrl.isNotBlank())
    }

    private suspend fun executeEntityParsingLoop(
        items: List<MedicineItem>,
        provider: AiProvider,
        params: ToolExecutionParams
    ): List<MedicineItem>? {
        val initialPrompt = buildEntityPrompt(items, params.feedbackPrompt)
        var currentPrompt = initialPrompt
        var attempts = 0
        while (attempts < 2) {
            val result = aiService.processTextPrompt(
                promptText = currentPrompt,
                selectedModel = params.model,
                providedApiKey = params.apiKey,
                provider = provider,
                customUrl = params.customUrl,
                flow = "Koog -> EntityParsingTool (Attempt ${attempts + 1})"
            )
            if (result.isFailure) break
            val rawText = result.getOrThrow()
            val parsed = parseEntityDtos(rawText, items)
            if (parsed.isSuccess) return parsed.getOrThrow()
            attempts++
            currentPrompt = buildCorrectionPrompt(initialPrompt, parsed.exceptionOrNull()?.message ?: "JSON parse error")
        }
        return null
    }

    private fun parseEntityDtos(rawText: String, items: List<MedicineItem>): Result<List<MedicineItem>> {
        val cleanedJson = cleanJson(rawText)
        val decoded = runCatching { jsonParser.decodeFromString<List<ParsedEntityDto>>(cleanedJson) }
        if (decoded.isFailure) return Result.failure(decoded.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        val dtos = decoded.getOrThrow().associateBy { it.id ?: -1 }
        val updated = items.mapIndexed { index, item ->
            val dto = dtos[index + 1] ?: return@mapIndexed item
            item.copy(
                medicineName = dto.medicineName?.takeIf { it.isNotBlank() } ?: item.medicineName,
                dosageForm = dto.dosageForm?.takeIf { it.isNotBlank() } ?: item.dosageForm,
                strengthValue = dto.strengthValue?.takeIf { it.isNotBlank() } ?: item.strengthValue,
                strengthUnit = dto.strengthUnit?.takeIf { it.isNotBlank() } ?: item.strengthUnit,
                strengthNote = dto.strengthNote?.takeIf { it.isNotBlank() } ?: item.strengthNote,
                schedule = dto.schedule?.takeIf { it.isNotBlank() } ?: item.schedule,
                units = dto.units?.takeIf { it > 0 } ?: item.units,
                unitType = dto.unitType?.takeIf { it.isNotBlank() } ?: item.unitType
            )
        }
        return Result.success(updated)
    }

    private fun applyLocalEntityNormalizations(items: List<MedicineItem>): List<MedicineItem> {
        val prefixRegex = Regex("(?i)^(Tab|Cap|Syr|Inj|Supp|Eye Drop)\\.?\\s*")
        return items.map { item ->
            val cleanName = item.medicineName.replace(prefixRegex, "").trim()
            val strengthNote = if (item.strengthValue.isBlank()) "not stated" else item.strengthNote
            val totalUnits = if (item.units <= 0) 1 else item.units
            item.copy(medicineName = cleanName, strengthNote = strengthNote, units = totalUnits)
        }
    }

    private suspend fun executeGenericLoop(
        items: List<MedicineItem>,
        provider: AiProvider,
        params: ToolExecutionParams
    ): List<MedicineItem>? {
        val initialPrompt = buildGenericPrompt(items, params.feedbackPrompt, params.memoryContext)
        var currentPrompt = initialPrompt
        var attempts = 0
        while (attempts < 2) {
            val result = aiService.processTextPrompt(
                promptText = currentPrompt,
                selectedModel = params.model,
                providedApiKey = params.apiKey,
                provider = provider,
                customUrl = params.customUrl,
                flow = "Koog -> GenericSubstituteTool (Attempt ${attempts + 1})"
            )
            if (result.isFailure) break
            val rawText = result.getOrThrow()
            val parsed = parseGenericDtos(rawText, items)
            if (parsed.isSuccess) return parsed.getOrThrow()
            attempts++
            currentPrompt = buildCorrectionPrompt(initialPrompt, parsed.exceptionOrNull()?.message ?: "JSON parse error")
        }
        return null
    }

    private fun parseGenericDtos(rawText: String, items: List<MedicineItem>): Result<List<MedicineItem>> {
        val cleanedJson = cleanJson(rawText)
        val decoded = runCatching { jsonParser.decodeFromString<List<GenericSubstituteDto>>(cleanedJson) }
        if (decoded.isFailure) return Result.failure(decoded.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        val dtos = decoded.getOrThrow().associateBy { it.id ?: -1 }
        val updated = items.mapIndexed { index, item ->
            val dto = dtos[index + 1] ?: return@mapIndexed item
            val newGeneric = dto.genericName?.takeIf { it.isNotBlank() } ?: item.genericName
            val newSubstitutes = dto.similarBrands?.filter { it.isNotBlank() }?.ifEmpty { item.similarBrands } ?: item.similarBrands
            item.copy(genericName = newGeneric, similarBrands = newSubstitutes)
        }
        return Result.success(updated)
    }

    private fun applyLocalSubstituteFallback(items: List<MedicineItem>): List<MedicineItem> {
        return items.map { item ->
            if (item.similarBrands.isEmpty() && item.genericName.isNotBlank()) {
                val clean = item.genericName.lowercase().trim()
                val defaults = when {
                    clean.contains("esomeprazole") -> listOf("Seclo (Square)", "Maxpro (Renata)", "Nexum (Incepta)")
                    clean.contains("omeprazole") -> listOf("Seclo (Square)", "Losectil (Eskayef)")
                    clean.contains("paracetamol") -> listOf("Napa (Beximco)", "Ace (Square)", "Pyrex (Incepta)")
                    clean.contains("cefixime") -> listOf("Cef-3 (Square)", "Denvar (Incepta)")
                    clean.contains("azithromycin") -> listOf("Zimax (Square)", "Zithrin (Beximco)")
                    clean.contains("montelukast") -> listOf("Monas (Pharmaasia)", "Provair (UniMed)")
                    else -> emptyList()
                }
                item.copy(similarBrands = defaults)
            } else {
                item
            }
        }
    }

    private suspend fun executeSafetyLoop(
        items: List<MedicineItem>,
        provider: AiProvider,
        params: ToolExecutionParams
    ): List<MedicineItem>? {
        val initialPrompt = buildSafetyPrompt(items, params.feedbackPrompt, params.memoryContext)
        var currentPrompt = initialPrompt
        var attempts = 0
        while (attempts < 2) {
            val result = aiService.processTextPrompt(
                promptText = currentPrompt,
                selectedModel = params.model,
                providedApiKey = params.apiKey,
                provider = provider,
                customUrl = params.customUrl,
                flow = "Koog -> SafetyAuditTool (Attempt ${attempts + 1})"
            )
            if (result.isFailure) break
            val rawText = result.getOrThrow()
            val parsed = parseSafetyDtos(rawText, items)
            if (parsed.isSuccess) return parsed.getOrThrow()
            attempts++
            currentPrompt = buildCorrectionPrompt(initialPrompt, parsed.exceptionOrNull()?.message ?: "JSON parse error")
        }
        return null
    }

    private fun parseSafetyDtos(rawText: String, items: List<MedicineItem>): Result<List<MedicineItem>> {
        val cleanedJson = cleanJson(rawText)
        val decoded = runCatching { jsonParser.decodeFromString<List<SafetyAuditDto>>(cleanedJson) }
        if (decoded.isFailure) return Result.failure(decoded.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        val dtos = decoded.getOrThrow().associateBy { it.id ?: -1 }
        val updated = items.mapIndexed { index, item ->
            val dto = dtos[index + 1] ?: return@mapIndexed item
            item.copy(
                needsReview = dto.needsReview ?: item.needsReview,
                possibleMisreading = dto.possibleMisreading?.takeIf { it.isNotBlank() } ?: item.possibleMisreading,
                confidence = dto.confidence?.takeIf { it.isNotBlank() } ?: item.confidence,
                enrichmentConfidence = dto.enrichmentConfidence?.takeIf { it.isNotBlank() } ?: item.enrichmentConfidence
            )
        }
        return Result.success(updated)
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

    private fun buildEntityPrompt(items: List<MedicineItem>, feedback: String): String = buildString {
        appendLine("You are an expert medical entity parser.")
        if (feedback.isNotBlank()) appendLine("SUPERVISOR INSTRUCTIONS: $feedback")
        appendLine("Analyze items and return JSON array: [{\"id\": 1, \"medicineName\": \"brand\", \"dosageForm\": \"tablet\", \"strengthValue\": \"500\", \"strengthUnit\": \"mg\", \"strengthNote\": \"500 mg\", \"schedule\": \"1+0+1\", \"units\": 14, \"unitType\": \"tablets\"}]")
        items.forEachIndexed { i, item ->
            appendLine("ID: ${i + 1}, Name: '${item.medicineName}', Form: '${item.dosageForm}', Schedule: '${item.schedule}', Units: ${item.units}")
        }
    }

    private fun buildGenericPrompt(items: List<MedicineItem>, feedback: String, memory: String): String = buildString {
        appendLine("You are a pharmaceutical substitute finder for Bangladeshi (DGDA) brand substitutes.")
        if (feedback.isNotBlank()) appendLine("SUPERVISOR INSTRUCTIONS: $feedback")
        if (memory.isNotBlank()) appendLine(memory)
        appendLine("Return JSON array: [{\"id\": 1, \"genericName\": \"Esomeprazole\", \"similarBrands\": [\"Seclo (Square)\", \"Maxpro (Renata)\"]}]")
        items.forEachIndexed { i, item ->
            appendLine("ID: ${i + 1}, Medicine: '${item.medicineName}', Written Generic: '${item.genericName}'")
        }
    }

    private fun buildSafetyPrompt(items: List<MedicineItem>, feedback: String, memory: String): String = buildString {
        appendLine("You are a clinical patient safety auditor.")
        if (feedback.isNotBlank()) appendLine("SUPERVISOR INSTRUCTIONS: $feedback")
        if (memory.isNotBlank()) appendLine(memory)
        appendLine("Return JSON array: [{\"id\": 1, \"needsReview\": true, \"possibleMisreading\": \"Confirm dose\", \"confidence\": \"medium\", \"enrichmentConfidence\": \"medium\"}]")
        items.forEachIndexed { i, item ->
            appendLine("ID: ${i + 1}, Medicine: '${item.medicineName}', Strength: '${item.formattedStrength}', Units: ${item.units}")
        }
    }

    private fun buildCorrectionPrompt(initialPrompt: String, parseError: String): String = buildString {
        appendLine(initialPrompt)
        appendLine("\nCRITICAL FIX REQUIRED: Previous JSON parsing failed with error: '$parseError'. Fix syntax and return ONLY strictly valid JSON.")
    }

    private fun cleanJson(rawText: String): String {
        var trimmed = rawText.trim()
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        }
        val start = trimmed.indexOf('[')
        val end = trimmed.lastIndexOf(']')
        return if (start != -1 && end != -1 && end >= start) trimmed.substring(start, end + 1) else trimmed
    }

    companion object {
        private const val TAG = "RxAgentTools"
    }
}
