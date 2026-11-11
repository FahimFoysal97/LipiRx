package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.ProcessingStep
import io.github.aakira.napier.Napier

data class OrchestrationResult(
    val items: List<MedicineItem>,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = ""
)

class MultiAgentRxOrchestrator(
    private val rxSupervisorAgent: RxSupervisorAgent,
    private val visionTranscriptionAgent: VisionTranscriptionAgent,
    private val medicalEntityParserAgent: MedicalEntityParserAgent,
    private val genericSubstituteAgent: GenericSubstituteAgent,
    private val safetyAuditorAgent: SafetyAuditorAgent,
    private val database: LipiRxLocalDatabase
) {
    suspend fun executePipeline(
        input: TranscriptionInput,
        agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
        onProgress: (step: ProcessingStep) -> Unit = {}
    ): Result<OrchestrationResult> {
        Napier.d("[$TAG] Initiating Manager/Supervisor-Driven Multiagent Orchestration Pipeline...", tag = TAG)

        // Retrieve Supervisor configuration (default to Gemini if not explicitly assigned)
        val supervisorConfig = agentConfigs[RxAgentType.SUPERVISOR]
        val supervisorProvider = supervisorConfig?.provider ?: AiProvider.Gemini
        val supervisorModel = supervisorConfig?.selectedModel?.ifBlank { supervisorProvider.defaultModel } ?: supervisorProvider.defaultModel
        val supervisorApiKey = database.getApiKeyForProvider(supervisorProvider.id).ifBlank { input.providedApiKey }
        val supervisorCustomUrl = (if (supervisorProvider == AiProvider.Custom) database.getProviderConfig().customUrl else "").ifBlank { input.customUrl }

        var state = SupervisorState(input = input)
        val maxTurns = 6

        while (!state.isFinished && state.turnCount < maxTurns) {
            val decision = rxSupervisorAgent.decideNextStep(
                state = state,
                provider = supervisorProvider,
                selectedModel = supervisorModel,
                apiKey = supervisorApiKey,
                customUrl = supervisorCustomUrl
            )

            if (decision.isSuspendedForApproval) {
                Napier.d("[$TAG -> Suspended] Supervisor suspended pipeline for Pharmacist Approval Gate. Reason: ${decision.approvalReason}", tag = TAG)
                state = state.copy(
                    isSuspendedForApproval = true,
                    approvalReason = decision.approvalReason
                )
                break
            }

            if (decision.isFinished || decision.nextAgent == null) {
                Napier.d("[$TAG -> Finished] Supervisor marked orchestration complete at Turn ${state.turnCount}. Reasoning: ${decision.reasoning}", tag = TAG)
                break
            }

            val targetAgentType = decision.nextAgent
            Napier.d("[$TAG -> Turn ${state.turnCount + 1}] Supervisor dispatches to ${targetAgentType.displayName}. Reason: ${decision.reasoning}", tag = TAG)

            // Resolve target agent's configured provider and model
            val agentConfig = agentConfigs[targetAgentType]
            val agentProvider = agentConfig?.provider ?: targetAgentType.defaultProvider
            val agentModel = agentConfig?.selectedModel?.ifBlank { agentProvider.defaultModel } ?: targetAgentType.defaultModel
            val agentApiKey = database.getApiKeyForProvider(agentProvider.id).ifBlank { input.providedApiKey }
            val agentCustomUrl = (if (agentProvider == AiProvider.Custom) database.getProviderConfig().customUrl else "").ifBlank { input.customUrl }

            val stepEnum = when (targetAgentType) {
                RxAgentType.TRANSCRIPTION -> ProcessingStep.READING_HANDWRITING
                RxAgentType.ENTITY_PARSER -> ProcessingStep.MATCHING_BRANDS
                RxAgentType.GENERIC_SUBSTITUTE -> ProcessingStep.FINDING_SIMILAR
                RxAgentType.SAFETY_AUDITOR -> ProcessingStep.COMPLETE
                RxAgentType.SUPERVISOR -> ProcessingStep.ENCODING
            }
            onProgress(stepEnum)

            val newItemsResult = when (targetAgentType) {
                RxAgentType.TRANSCRIPTION -> {
                    val stage1Input = input.copy(
                        provider = agentProvider,
                        selectedModel = agentModel,
                        providedApiKey = agentApiKey,
                        customUrl = agentCustomUrl
                    )
                    visionTranscriptionAgent.execute(stage1Input)
                }
                RxAgentType.ENTITY_PARSER -> {
                    medicalEntityParserAgent.execute(
                        input = state.items,
                        provider = agentProvider,
                        selectedModel = agentModel,
                        apiKey = agentApiKey,
                        customUrl = agentCustomUrl,
                        feedbackPrompt = decision.feedbackPrompt
                    )
                }
                RxAgentType.GENERIC_SUBSTITUTE -> {
                    genericSubstituteAgent.execute(
                        input = state.items,
                        provider = agentProvider,
                        selectedModel = agentModel,
                        apiKey = agentApiKey,
                        customUrl = agentCustomUrl,
                        feedbackPrompt = decision.feedbackPrompt
                    )
                }
                RxAgentType.SAFETY_AUDITOR -> {
                    safetyAuditorAgent.execute(
                        input = state.items,
                        provider = agentProvider,
                        selectedModel = agentModel,
                        apiKey = agentApiKey,
                        customUrl = agentCustomUrl,
                        feedbackPrompt = decision.feedbackPrompt
                    )
                }
                RxAgentType.SUPERVISOR -> Result.success(state.items)
            }

            if (newItemsResult.isFailure && targetAgentType == RxAgentType.TRANSCRIPTION) {
                val ex = newItemsResult.exceptionOrNull() ?: IllegalStateException("Vision transcription agent failed")
                Napier.e("[$TAG -> Failure] Primary Vision Transcription Agent failed: ${ex.message}", throwable = ex, tag = TAG)
                return Result.failure(ex)
            }

            val updatedItems = newItemsResult.getOrDefault(state.items)
            val record = AgentExecutionRecord(
                agentType = targetAgentType,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = updatedItems.size
            )

            state = state.copy(
                items = updatedItems,
                history = state.history + record,
                turnCount = state.turnCount + 1
            )
        }

        onProgress(ProcessingStep.COMPLETE)
        Napier.d("[$TAG -> Success] Pipeline completed with ${state.items.size} medicines. SuspendedForApproval=${state.isSuspendedForApproval}", tag = TAG)
        return Result.success(
            OrchestrationResult(
                items = state.items,
                isSuspendedForApproval = state.isSuspendedForApproval,
                approvalReason = state.approvalReason
            )
        )
    }

    companion object {
        private const val TAG = "MultiAgentRxOrchestrator"
    }
}
