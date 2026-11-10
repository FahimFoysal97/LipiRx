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

        var currentItems = input

        val service = aiService
        if (service != null && (apiKey.isNotBlank() || provider == AiProvider.Custom && customUrl.isNotBlank())) {
            runCatching {
                val prompt = buildString {
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
                    val parsedDtos = jsonParser.decodeFromString<List<ParsedEntityDto>>(cleanedJson)
                    val dtoMap = parsedDtos.associateBy { it.id ?: -1 }

                    currentItems = input.mapIndexed { index, item ->
                        val dto = dtoMap[index + 1]
                        if (dto != null) {
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
                        } else {
                            item
                        }
                    }
                    Napier.d("[$agentName -> AI Success] Successfully parsed and updated ${currentItems.size} medicine entities from ${provider.displayName}", tag = TAG)
                }
            }.onFailure { ex ->
                Napier.w("[$agentName -> AI Fallback] Remote AI prompt failed: ${ex.message}. Falling back to local rules.", tag = TAG)
            }
        }

        // Local Rule-Based Normalization (Guarantee / Fallback)
        val finalParsed = currentItems.map { item ->
            val cleanName = item.medicineName.replace(PrefixRegex, "").trim()
            val strengthNote = if (item.strengthValue.isBlank()) "not stated" else item.strengthNote
            val totalUnits = if (item.units <= 0) 1 else item.units

            item.copy(
                medicineName = cleanName,
                strengthNote = strengthNote,
                units = totalUnits,
            )
        }

        Napier.d("[$agentName -> Result] Successfully parsed ${finalParsed.size} medical items", tag = TAG)
        return Result.success(finalParsed)
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
