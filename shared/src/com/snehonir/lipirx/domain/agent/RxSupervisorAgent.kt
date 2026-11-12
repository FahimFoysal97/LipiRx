package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AgentExecutionRecord(
    val agentType: RxAgentType,
    val reasoning: String,
    val feedbackPrompt: String,
    val itemCount: Int,
    val executionTimeMs: Long = 0L,
    val failureReason: PipelineFailureReason? = null
)

data class SupervisorState(
    val input: TranscriptionInput,
    val items: List<MedicineItem> = emptyList(),
    val history: List<AgentExecutionRecord> = emptyList(),
    val episodicMemory: String = "",
    val turnCount: Int = 0,
    val isFinished: Boolean = false,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val safetyAudited: Boolean = true,
    val entityParsed: Boolean = true
)

@Serializable
internal data class SupervisorDecisionDto(
    val nextAgent: String? = null,
    val reasoning: String? = null,
    val feedbackPrompt: String? = null,
    val isSuspendedForApproval: Boolean? = null,
    val approvalReason: String? = null
)

data class SupervisorDecision(
    val nextAgent: RxAgentType?,
    val isFinished: Boolean,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val reasoning: String,
    val feedbackPrompt: String
)

class RxSupervisorAgent(
    private val aiService: CompositeAiService? = null,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    val agentName: String = "Rx Supervisor Manager Agent"
    val description: String = "Evaluates current prescription analysis workspace and routes tasks dynamically to specialized agents"

    suspend fun decideNextStep(
        state: SupervisorState,
        provider: AiProvider = AiProvider.Gemini,
        selectedModel: String = "",
        apiKey: String = "",
        customUrl: String = ""
    ): SupervisorDecision {
        Napier.d("[$agentName] Turn ${state.turnCount + 1}: Evaluating state (items=${state.items.size}, historySize=${state.history.size}, memoryLength=${state.episodicMemory.length})...", tag = TAG)

        if (canExecuteAiService(provider, apiKey, customUrl)) {
            executeSupervisorLlmLoop(state, provider, selectedModel, apiKey, customUrl)?.let { decision ->
                Napier.d("[$agentName -> LLM Decision] Next: ${decision.nextAgent?.displayName ?: "FINISH"}, Reason: ${decision.reasoning}", tag = TAG)
                return decision
            }
        }

        val fallback = getFallbackDecision(state)
        Napier.d("[$agentName -> Rule Fallback] Next: ${fallback.nextAgent?.displayName ?: "FINISH"}, Reason: ${fallback.reasoning}", tag = TAG)
        return fallback
    }

    private fun canExecuteAiService(provider: AiProvider, apiKey: String, customUrl: String): Boolean {
        return aiService != null && (apiKey.isNotBlank() || (provider == AiProvider.Custom && customUrl.isNotBlank()))
    }

    private suspend fun executeSupervisorLlmLoop(
        state: SupervisorState,
        provider: AiProvider,
        selectedModel: String,
        apiKey: String,
        customUrl: String
    ): SupervisorDecision? {
        val service = aiService ?: return null
        val initialPrompt = buildSupervisorPrompt(state)
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
                flow = "Supervisor -> $agentName (Attempt ${attempts + 1})"
            )

            if (aiResult.isFailure) break

            val rawText = aiResult.getOrThrow()
            val parseResult = parseAndValidateDecision(rawText, state)

            if (parseResult.isSuccess) {
                return parseResult.getOrThrow()
            }

            attempts++
            val parseError = parseResult.exceptionOrNull()?.message ?: "JSON parse failure"
            Napier.w("[$agentName -> Self-Correction Attempt $attempts] JSON error: $parseError. Triggering correction loop...", tag = TAG)
            currentPrompt = buildCorrectionPrompt(initialPrompt, parseError)
        }

        return null
    }

    private fun parseAndValidateDecision(rawText: String, state: SupervisorState): Result<SupervisorDecision> {
        val cleanedJson = cleanJson(rawText)
        val decodedResult = runCatching { jsonParser.decodeFromString<SupervisorDecisionDto>(cleanedJson) }

        if (decodedResult.isFailure) {
            return Result.failure(decodedResult.exceptionOrNull() ?: IllegalArgumentException("Invalid JSON"))
        }

        val dto = decodedResult.getOrThrow()
        val decision = mapDtoToDecision(dto, state)
        return Result.success(decision)
    }

    private fun mapDtoToDecision(dto: SupervisorDecisionDto, state: SupervisorState): SupervisorDecision {
        if (dto.isSuspendedForApproval == true) {
            return SupervisorDecision(
                nextAgent = null,
                isFinished = false,
                isSuspendedForApproval = true,
                approvalReason = dto.approvalReason ?: "Pharmacist sign-off required for flagged items",
                reasoning = dto.reasoning ?: "Supervisor requested human approval.",
                feedbackPrompt = ""
            )
        }

        val nextAgentStr = dto.nextAgent?.uppercase()?.trim() ?: ""
        return when {
            nextAgentStr == "FINISH" || nextAgentStr.isBlank() -> handleFinishDecision(dto, state)
            else -> {
                val targetAgent = parseAgentType(nextAgentStr)
                SupervisorDecision(
                    nextAgent = targetAgent,
                    isFinished = false,
                    reasoning = dto.reasoning ?: "Supervisor assigned task to ${targetAgent.displayName}.",
                    feedbackPrompt = dto.feedbackPrompt ?: ""
                )
            }
        }
    }

    private fun handleFinishDecision(dto: SupervisorDecisionDto, state: SupervisorState): SupervisorDecision {
        return if (state.items.isEmpty()) {
            Napier.w("[$agentName] Supervisor prematurely returned FINISH/blank while workspace items is empty. Forcing TRANSCRIPTION step.", tag = TAG)
            SupervisorDecision(
                nextAgent = RxAgentType.TRANSCRIPTION,
                isFinished = false,
                reasoning = "Forced transcription because workspace has no items yet.",
                feedbackPrompt = "Transcribe prescription."
            )
        } else {
            SupervisorDecision(
                nextAgent = null,
                isFinished = true,
                reasoning = dto.reasoning ?: "Supervisor completed all orchestration steps.",
                feedbackPrompt = dto.feedbackPrompt ?: ""
            )
        }
    }

    private fun buildSupervisorPrompt(state: SupervisorState): String = buildString {
        appendLine("You are the master Manager/Supervisor AI Agent for a retail pharmacy prescription processing system.")
        appendLine("Your role is to evaluate the current workspace state and decide which specialized worker agent should execute next, if processing is COMPLETE, or if Human-in-the-Loop approval is required.")
        appendLine()
        appendLine("AVAILABLE WORKER AGENTS:")
        appendLine("1. TRANSCRIPTION: Run Vision handwriting OCR on the prescription image.")
        appendLine("2. ENTITY_PARSER: Clean, normalize, and format medicine brand names, dosage forms, strengths, and schedules.")
        appendLine("3. GENERIC_SUBSTITUTE: Find active generic compositions and 3-5 Bangladeshi brand substitutes.")
        appendLine("4. SAFETY_AUDITOR: Audit dosage safety, interaction risks, unreadable items, and set pharmacist review flags.")
        appendLine("5. FINISH: Mark execution COMPLETE when all necessary steps have been performed satisfactorily.")
        appendLine()
        if (state.episodicMemory.isNotBlank()) {
            appendLine(state.episodicMemory)
            appendLine()
        }
        appendLine("CURRENT WORKSPACE STATE:")
        appendLine("- Turn: ${state.turnCount + 1}")
        appendLine("- Items in Memory: ${state.items.size}")
        if (state.items.isNotEmpty()) {
            appendLine("- Medicines in workspace:")
            state.items.forEachIndexed { i, item ->
                appendLine("  $i: '${item.medicineName}', Strength='${item.formattedStrength}', Generic='${item.genericName}', SubstitutesCount=${item.similarBrands.size}, NeedsReview=${item.needsReview}")
            }
        }
        if (state.history.isNotEmpty()) {
            appendLine("- Execution History:")
            state.history.forEach { record ->
                appendLine("  * Agent ${record.agentType.displayName}: Reason='${record.reasoning}', OutputItems=${record.itemCount}")
            }
        }
        appendLine()
        appendLine("""Return ONLY a JSON object with this structure:""")
        appendLine("""{"nextAgent": "TRANSCRIPTION"|"ENTITY_PARSER"|"GENERIC_SUBSTITUTE"|"SAFETY_AUDITOR"|"FINISH", "reasoning": "Explanation", "feedbackPrompt": "Instructions", "isSuspendedForApproval": false, "approvalReason": ""}""")
    }

    private fun buildCorrectionPrompt(initialPrompt: String, parseError: String): String = buildString {
        appendLine(initialPrompt)
        appendLine()
        appendLine("CRITICAL FIX REQUIRED: Your previous response failed JSON schema validation with error: '$parseError'.")
        appendLine("Fix the JSON syntax and return ONLY strictly valid JSON matching the exact required schema.")
    }

    private fun parseAgentType(str: String): RxAgentType {
        return when {
            str.contains("TRANSCRIPTION") -> RxAgentType.TRANSCRIPTION
            str.contains("PARSER") || str.contains("ENTITY") -> RxAgentType.ENTITY_PARSER
            str.contains("GENERIC") || str.contains("SUBSTITUTE") -> RxAgentType.GENERIC_SUBSTITUTE
            str.contains("SAFETY") || str.contains("AUDITOR") -> RxAgentType.SAFETY_AUDITOR
            else -> RxAgentType.TRANSCRIPTION
        }
    }

    private fun getFallbackDecision(state: SupervisorState): SupervisorDecision {
        val historyTypes = state.history.map { it.agentType }.toSet()
        return when {
            state.items.isEmpty() && !historyTypes.contains(RxAgentType.TRANSCRIPTION) -> {
                SupervisorDecision(
                    nextAgent = RxAgentType.TRANSCRIPTION,
                    isFinished = false,
                    reasoning = "Workspace empty. Requesting Vision Transcription Agent to perform handwriting OCR.",
                    feedbackPrompt = "Transcribe all written medicines accurately."
                )
            }
            !historyTypes.contains(RxAgentType.ENTITY_PARSER) -> {
                SupervisorDecision(
                    nextAgent = RxAgentType.ENTITY_PARSER,
                    isFinished = false,
                    reasoning = "Raw transcription complete. Requesting Medical Entity Parser Agent to clean brand names and normalize forms.",
                    feedbackPrompt = "Clean prefixes and normalize strengths."
                )
            }
            !historyTypes.contains(RxAgentType.GENERIC_SUBSTITUTE) -> {
                SupervisorDecision(
                    nextAgent = RxAgentType.GENERIC_SUBSTITUTE,
                    isFinished = false,
                    reasoning = "Entities normalized. Requesting Generic & Substitute Agent to match DGDA active ingredients and BD brand substitutes.",
                    feedbackPrompt = "Provide 3 to 5 Bangladeshi brand substitutes per item."
                )
            }
            !historyTypes.contains(RxAgentType.SAFETY_AUDITOR) -> {
                SupervisorDecision(
                    nextAgent = RxAgentType.SAFETY_AUDITOR,
                    isFinished = false,
                    reasoning = "Substitutes enriched. Requesting Safety Auditor Agent to verify clinical dosage safety and review flags.",
                    feedbackPrompt = "Audit safety risks and unreadable spellings."
                )
            }
            state.items.any { it.needsReview || it.confidenceEnum.isLow } && !state.isSuspendedForApproval -> {
                SupervisorDecision(
                    nextAgent = null,
                    isFinished = false,
                    isSuspendedForApproval = true,
                    approvalReason = "Flagged items require pharmacist verification",
                    reasoning = "Flagged low confidence or dosage review items requiring human sign-off.",
                    feedbackPrompt = ""
                )
            }
            else -> {
                SupervisorDecision(
                    nextAgent = null,
                    isFinished = true,
                    reasoning = "All pipeline capabilities executed successfully.",
                    feedbackPrompt = ""
                )
            }
        }
    }

    private fun cleanJson(rawText: String): String {
        var trimmed = rawText.trim()
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substringAfter("\n").substringBeforeLast("```").trim()
        }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start != -1 && end != -1 && end >= start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }

    companion object {
        private const val TAG = "RxSupervisorAgent"
    }
}
