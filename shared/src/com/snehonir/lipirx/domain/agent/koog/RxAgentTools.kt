package com.snehonir.lipirx.domain.agent.koog

import com.snehonir.lipirx.data.remote.service.CompositeAiService
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.dto.*
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import io.github.aakira.napier.Napier
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

class RxAgentTools(
    private val aiService: CompositeAiService,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    suspend fun executeVisionOCR(input: TranscriptionInput): Result<List<MedicineItem>> {
        Napier.d("Invoking Vision OCR via ${input.provider.displayName}...", tag = TAG)
        return aiService.processPrescriptionImage(
            imageByteArray = input.imageByteArray,
            selectedModel = input.selectedModel,
            providedApiKey = input.providedApiKey,
            provider = input.provider,
            customUrl = input.customUrl,
            mimeType = input.mimeType,
            strategy = input.strategy,
            fallbackProvider = input.fallbackProvider,
            fallbackModel = input.fallbackModel,
            fallbackApiKey = input.fallbackApiKey,
            flow = "Koog -> VisionOCRTool",
            onProgress = input.onProgress
        )
    }

    suspend fun executeEntityParsing(
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        if (items.isEmpty()) return Result.success(emptyList())
        val aiParsed = runAiJsonTool(
            toolName = "EntityParsingTool",
            items = items,
            params = params,
            prompt = buildEntityPrompt(items, params.feedbackPrompt),
            serializer = ListSerializer(ParsedEntityDto.serializer()),
            idOf = { it.id },
            merge = ::mergeEntity
        ) ?: items
        return Result.success(applyLocalEntityNormalizations(aiParsed))
    }

    suspend fun executeGenericSubstituteSearch(
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        if (items.isEmpty()) return Result.success(emptyList())
        val enriched = runAiJsonTool(
            toolName = "GenericSubstituteTool",
            items = items,
            params = params,
            prompt = buildGenericPrompt(items, params.feedbackPrompt, params.memoryContext),
            serializer = ListSerializer(GenericSubstituteDto.serializer()),
            idOf = { it.id },
            merge = ::mergeGeneric
        ) ?: items
        return Result.success(enriched)
    }

    suspend fun executeSafetyAudit(
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        if (items.isEmpty()) return Result.success(emptyList())
        val audited = runAiJsonTool(
            toolName = "SafetyAuditTool",
            items = items,
            params = params,
            prompt = buildSafetyPrompt(items, params.feedbackPrompt, params.memoryContext),
            serializer = ListSerializer(SafetyAuditDto.serializer()),
            idOf = { it.id },
            merge = ::mergeSafety
        ) ?: items
        return Result.success(applyLocalSafetyRules(audited))
    }

    /**
     * Merges results from multiple parallel worker agent executions non-destructively into a unified item list.
     */
    fun mergeParallelResults(
        baseItems: List<MedicineItem>,
        results: List<Result<List<MedicineItem>>>
    ): List<MedicineItem> {
        if (baseItems.isEmpty() || results.isEmpty()) return baseItems

        var currentList = baseItems
        for (res in results) {
            val agentList = res.getOrNull() ?: continue
            currentList = currentList.mapIndexed { index, baseItem ->
                val agentItem = agentList.getOrNull(index) ?: return@mapIndexed baseItem
                baseItem.copy(
                    genericName = agentItem.genericName.ifBlank { baseItem.genericName },
                    similarBrands = agentItem.similarBrands.ifEmpty { baseItem.similarBrands },
                    dosageForm = agentItem.dosageForm.ifBlank { baseItem.dosageForm },
                    strengthValue = agentItem.strengthValue.ifBlank { baseItem.strengthValue },
                    strengthUnit = agentItem.strengthUnit.ifBlank { baseItem.strengthUnit },
                    schedule = agentItem.schedule.ifBlank { baseItem.schedule },
                    units = agentItem.units.takeIf { it > 0 } ?: baseItem.units,
                    needsReview = baseItem.needsReview || agentItem.needsReview,
                    possibleMisreading = agentItem.possibleMisreading.ifBlank { baseItem.possibleMisreading },
                    confidence = agentItem.confidence.ifBlank { baseItem.confidence }
                )
            }
        }
        return currentList
    }

    // ---------- Shared AI loop ----------

    private fun canExecute(provider: AiProvider, apiKey: String, customUrl: String) =
        apiKey.isNotBlank() || (provider == AiProvider.Custom && customUrl.isNotBlank())

    /**
     * Sends [prompt], parses a JSON array of [D], and merges each DTO into the item
     * at position (id - 1). Retries once with the parse error appended.
     * Returns null if AI is unavailable or every attempt failed (caller falls back to input).
     */
    private suspend fun <D> runAiJsonTool(
        toolName: String,
        items: List<MedicineItem>,
        params: ToolExecutionParams,
        prompt: String,
        serializer: KSerializer<List<D>>,
        idOf: (D) -> Int?,
        merge: (MedicineItem, D) -> MedicineItem
    ): List<MedicineItem>? {
        val provider = AiProvider.fromId(params.provider)
        if (!canExecute(provider, params.apiKey, params.customUrl)) {
            Napier.w("$toolName skipped: no API key/custom URL", tag = TAG)
            return null
        }
        Napier.d("Invoking $toolName for ${items.size} items...", tag = TAG)

        val strategy = OnDeviceStrategy.fromId(params.strategy)
        val fallbackProvider = AiProvider.fromId(params.fallbackProvider)
        var currentPrompt = prompt

        for (attempt in 1..MAX_ATTEMPTS) {
            val raw = aiService.processTextPrompt(
                promptText = currentPrompt,
                selectedModel = params.model,
                providedApiKey = params.apiKey,
                provider = provider,
                customUrl = params.customUrl,
                strategy = strategy,
                fallbackProvider = fallbackProvider,
                fallbackModel = params.fallbackModel,
                flow = "Koog -> $toolName (Attempt $attempt)"
            ).getOrElse {
                Napier.w("$toolName AI call failed: ${it.message}", tag = TAG)
                return null
            }

            val decoded = runCatching {
                jsonParser.decodeFromString(serializer, extractJsonArray(raw))
            }
            decoded.onSuccess { dtos ->
                val byId = dtos.mapNotNull { d -> idOf(d)?.let { it to d } }.toMap()
                return items.mapIndexed { index, item ->
                    byId[index + 1]?.let { merge(item, it) } ?: item
                }
            }
            val error = decoded.exceptionOrNull()?.message ?: "JSON parse error"
            Napier.w("$toolName parse failed (attempt $attempt): $error", tag = TAG)
            currentPrompt = buildCorrectionPrompt(prompt, error)
        }
        return null
    }

    // ---------- Merge rules ----------

    private fun mergeEntity(item: MedicineItem, dto: ParsedEntityDto): MedicineItem {
        val merged = item.copy(
            medicineName = dto.medicineName.orKeep(item.medicineName),
            dosageForm = dto.dosageForm.orKeep(item.dosageForm),
            strengthValue = dto.strengthValue.orKeep(item.strengthValue),
            strengthUnit = dto.strengthUnit.orKeep(item.strengthUnit),
            strengthNote = dto.strengthNote.orKeep(item.strengthNote),
            schedule = dto.schedule.orKeep(item.schedule),
            units = dto.units?.takeIf { it > 0 } ?: item.units,
            unitType = dto.unitType.orKeep(item.unitType)
        )
        // Dose-critical fields changed by the model -> a human should confirm.
        val criticalChanged = merged.strengthValue != item.strengthValue ||
                merged.strengthUnit != item.strengthUnit ||
                merged.schedule != item.schedule ||
                merged.units != item.units
        return if (criticalChanged) merged.copy(needsReview = true) else merged
    }

    private fun mergeGeneric(item: MedicineItem, dto: GenericSubstituteDto): MedicineItem =
        item.copy(
            genericName = dto.genericName.orKeep(item.genericName),
            similarBrands = dto.similarBrands
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?: item.similarBrands
        )

    private fun mergeSafety(item: MedicineItem, dto: SafetyAuditDto): MedicineItem =
        item.copy(
            // The audit may raise a flag but never clear one.
            needsReview = item.needsReview || dto.needsReview == true,
            possibleMisreading = dto.possibleMisreading.orKeep(item.possibleMisreading),
            confidence = dto.confidence.orKeep(item.confidence),
            enrichmentConfidence = dto.enrichmentConfidence.orKeep(item.enrichmentConfidence)
        )

    private fun String?.orKeep(fallback: String): String = this?.takeIf { it.isNotBlank() } ?: fallback

    // ---------- Local deterministic rules ----------

    private fun applyLocalEntityNormalizations(items: List<MedicineItem>): List<MedicineItem> =
        items.map { item ->
            val unitsMissing = item.units <= 0
            item.copy(
                medicineName = item.medicineName.replace(PREFIX_REGEX, "").trim(),
                strengthNote = if (item.strengthValue.isBlank()) NOT_STATED else item.strengthNote,
                units = if (unitsMissing) 1 else item.units,
                needsReview = item.needsReview || unitsMissing
            )
        }

    private fun applyLocalSafetyRules(items: List<MedicineItem>): List<MedicineItem> =
        items.map { item ->
            val needsReview = item.needsReview ||
                    item.confidenceEnum.isLow ||
                    item.enrichmentConfidenceEnum.isLow ||
                    item.possibleMisreading.isNotBlank() ||
                    !item.brandRecognized ||
                    item.units > HIGH_VOLUME_UNITS
            item.copy(needsReview = needsReview)
        }

    // ---------- Prompts (items sent as JSON, so quotes in names can't break them) ----------

    private fun buildEntityPrompt(items: List<MedicineItem>, feedback: String) = buildString {
        appendLine("You are an expert medical entity parser.")
        appendSupervisor(feedback)
        appendLine("Return ONLY a JSON array like: [{\"id\":1,\"medicineName\":\"brand\",\"dosageForm\":\"tablet\",\"strengthValue\":\"500\",\"strengthUnit\":\"mg\",\"strengthNote\":\"500 mg\",\"schedule\":\"1+0+1\",\"units\":14,\"unitType\":\"tablets\"}]")
        appendLine("Do not invent values. Omit a field if unsure.")
        appendLine("Items:")
        appendLine(itemsJson(items) { item ->
            put("name", item.medicineName); put("form", item.dosageForm)
            put("schedule", item.schedule); put("units", item.units)
        })
    }

    private fun buildGenericPrompt(items: List<MedicineItem>, feedback: String, memory: String) = buildString {
        appendLine("You are a pharmaceutical substitute finder for Bangladeshi (DGDA) brand substitutes.")
        appendSupervisor(feedback)
        if (memory.isNotBlank()) appendLine(memory)
        appendLine("Return ONLY a JSON array like: [{\"id\":1,\"genericName\":\"Esomeprazole\",\"similarBrands\":[\"Seclo (Square)\",\"Maxpro (Renata)\"]}]")
        appendLine("Items:")
        appendLine(itemsJson(items) { item ->
            put("medicine", item.medicineName); put("writtenGeneric", item.genericName)
        })
    }

    private fun buildSafetyPrompt(items: List<MedicineItem>, feedback: String, memory: String) = buildString {
        appendLine("You are a clinical patient safety auditor.")
        appendSupervisor(feedback)
        if (memory.isNotBlank()) appendLine(memory)
        appendLine("Return ONLY a JSON array like: [{\"id\":1,\"needsReview\":true,\"possibleMisreading\":\"Confirm dose\",\"confidence\":\"medium\",\"enrichmentConfidence\":\"medium\"}]")
        appendLine("Items:")
        appendLine(itemsJson(items) { item ->
            put("medicine", item.medicineName); put("strength", item.formattedStrength)
            put("units", item.units)
        })
    }

    private fun StringBuilder.appendSupervisor(feedback: String) {
        if (feedback.isNotBlank()) appendLine("SUPERVISOR INSTRUCTIONS: $feedback")
    }

    private fun itemsJson(
        items: List<MedicineItem>,
        fields: JsonObjectBuilder.(MedicineItem) -> Unit
    ): String = buildJsonArray {
        items.forEachIndexed { i, item ->
            add(buildJsonObject { put("id", i + 1); fields(item) })
        }
    }.toString()

    private fun buildCorrectionPrompt(initialPrompt: String, parseError: String) = buildString {
        appendLine(initialPrompt)
        appendLine("\nCRITICAL FIX REQUIRED: Previous JSON parsing failed with error: '$parseError'. Return ONLY strictly valid JSON.")
    }

    private fun extractJsonArray(raw: String): String {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        return if (start != -1 && end > start) raw.substring(start, end + 1) else raw.trim()
    }

    companion object {
        private const val TAG = "RxAgentTools"
        private const val MAX_ATTEMPTS = 2
        private const val HIGH_VOLUME_UNITS = 200
        private const val NOT_STATED = "not stated"
        private val PREFIX_REGEX = Regex(
            "^(?:tab(?:let)?|cap(?:sule)?|syr(?:up)?|inj(?:ection)?|supp(?:ository)?|eye\\s*drops?)(?:\\.\\s*|\\s+)",
            RegexOption.IGNORE_CASE
        )
    }
}