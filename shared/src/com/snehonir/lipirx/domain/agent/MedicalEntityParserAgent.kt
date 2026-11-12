package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

class MedicalEntityParserAgent(
    private val aiService: CompositeAiService? = null,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : RxAgent<List<MedicineItem>, List<MedicineItem>> {

    override val agentName: String = "Medical Entity Parser Agent"
    override val description: String = "Parses and normalizes medicine forms, strengths, schedules, and calculated quantities"

    override suspend fun execute(input: List<MedicineItem>): Result<List<MedicineItem>> {
        return execute(input, AiProvider.Gemini, "", "", "", "")
    }

    suspend fun execute(
        input: List<MedicineItem>,
        provider: AiProvider = AiProvider.Gemini,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = "",
        feedbackPrompt: String = ""
    ): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Parsing ${input.size} medicine entities via ${provider.displayName} ($selectedModel, keyLength=${apiKey.length})...", tag = TAG)

        if (input.isEmpty()) return Result.success(emptyList())

        val aiParsedItems = if (canExecuteAiService(provider, apiKey, customUrl)) {
            executeAiParsingLoop(input, provider, selectedModel, apiKey, customUrl, feedbackPrompt) ?: input
        } else {
            input
        }

        val finalParsed = applyLocalNormalizations(aiParsedItems)
        Napier.d("[$agentName -> Result] Successfully parsed ${finalParsed.size} medical items", tag = TAG)
        return Result.success(finalParsed)
    }

    private fun canExecuteAiService(provider: AiProvider, apiKey: String, customUrl: String): Boolean {
        return aiService != null && (apiKey.isNotBlank() || (provider == AiProvider.Custom && customUrl.isNotBlank()))
    }

    private suspend fun executeAiParsingLoop(
        input: List<MedicineItem>,
        provider: AiProvider,
        selectedModel: String,
        apiKey: String,
        customUrl: String,
        feedbackPrompt: String
    ): List<MedicineItem>? {
        val service = aiService ?: return null
        val initialPrompt = buildPrompt(input, feedbackPrompt)
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
                val updated = parseResult.getOrThrow()
                Napier.d("[$agentName -> AI Success] Successfully parsed and updated ${updated.size} medicine entities on attempt ${attempts + 1}", tag = TAG)
                return updated
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
        val decodedResult = runCatching { jsonParser.decodeFromString<List<ParsedEntityDto>>(cleanedJson) }

        if (decodedResult.isFailure) {
            return Result.failure(decodedResult.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        }

        val parsedDtos = decodedResult.getOrThrow()
        val dtoMap = parsedDtos.associateBy { it.id ?: -1 }

        val updatedItems = input.mapIndexed { index, item ->
            val dto = dtoMap[index + 1] ?: return@mapIndexed item
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

        return Result.success(updatedItems)
    }

    private fun applyLocalNormalizations(items: List<MedicineItem>): List<MedicineItem> {
        return items.map { item ->
            val cleanName = item.medicineName.replace(PrefixRegex, "").trim()
            val strengthNote = if (item.strengthValue.isBlank()) "not stated" else item.strengthNote
            val totalUnits = if (item.units <= 0) 1 else item.units

            item.copy(
                medicineName = cleanName,
                strengthNote = strengthNote,
                units = totalUnits
            )
        }
    }

    private fun buildPrompt(input: List<MedicineItem>, feedbackPrompt: String): String = buildString {
        appendLine("You are an expert medical entity parser.")
        if (feedbackPrompt.isNotBlank()) {
            appendLine("SUPERVISOR INSTRUCTIONS: $feedbackPrompt")
        }
        appendLine("Analyze the following medicine items extracted from a prescription and return a JSON array containing normalized values.")
        appendLine("Format for each item in the JSON array:")
        appendLine("""[{"id": 1, "medicineName": "clean brand name without Tab/Cap prefix", "dosageForm": "tablet", "strengthValue": "500", "strengthUnit": "mg", "strengthNote": "500 mg", "schedule": "1 + 0 + 1 (pc)", "units": 14, "unitType": "tablets"}]""")
        appendLine("Return ONLY a JSON array of objects without markdown formatting.")
        appendLine("Input items:")
        input.forEachIndexed { i, item ->
            appendLine("ID: ${i + 1}, Name: '${item.medicineName}', Form: '${item.dosageForm}', StrengthValue: '${item.strengthValue}', StrengthUnit: '${item.strengthUnit}', Schedule: '${item.schedule}', Units: ${item.units}")
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
        private const val TAG = "MedicalEntityParserAgent"
        private val PrefixRegex = Regex("(?i)^(Tab|Cap|Syr|Inj|Supp|Eye Drop)\\.?\\s*")
    }
}
