package com.snehonir.lipirx.domain.agent.koog

import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.AgentExecutionRecord
import com.snehonir.lipirx.domain.agent.OrchestrationResult
import com.snehonir.lipirx.domain.agent.PipelineFailureReason
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.RxSupervisorAgent
import com.snehonir.lipirx.domain.agent.SupervisorState
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.ProcessingStep
import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import io.github.aakira.napier.Napier

class KoogRxSupervisorEngine(
    private val rxSupervisorAgent: RxSupervisorAgent,
    private val rxAgentTools: RxAgentTools,
    private val database: LipiRxLocalDatabase
) {
    suspend fun runOrchestration(
        input: TranscriptionInput,
        agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
        onProgress: (step: ProcessingStep) -> Unit = {}
    ): Result<OrchestrationResult> {
        Napier.d("[$TAG] Starting JetBrains Koog Multi-Agent Supervisor Engine...", tag = TAG)

        val supervisorRuntime = resolveRuntimeConfig(
            agentType = RxAgentType.SUPERVISOR,
            agentConfigs = agentConfigs,
            defaultProvider = AiProvider.Gemini,
            defaultModel = AiProvider.Gemini.defaultModel,
            fallbackApiKey = input.providedApiKey,
            fallbackCustomUrl = input.customUrl
        )

        var state = SupervisorState(input = input)
        val maxTurns = 6

        while (!state.isFinished && state.turnCount < maxTurns) {
            val supervisorStartTime = getPlatformEpochMillis()
            val decision = rxSupervisorAgent.decideNextStep(
                state = state,
                provider = supervisorRuntime.provider,
                selectedModel = supervisorRuntime.model,
                apiKey = supervisorRuntime.apiKey,
                customUrl = supervisorRuntime.customUrl
            )
            val supervisorTimeMs = getPlatformEpochMillis() - supervisorStartTime

            if (decision.isSuspendedForApproval) {
                Napier.d("[$TAG -> Suspended] Pipeline suspended for Pharmacist Gate: ${decision.approvalReason}", tag = TAG)
                state = state.copy(
                    isSuspendedForApproval = true,
                    approvalReason = decision.approvalReason
                )
                break
            }

            if (decision.isFinished || decision.nextAgent == null) {
                Napier.d("[$TAG -> Complete] Supervisor finished execution at turn ${state.turnCount}", tag = TAG)
                break
            }

            val targetAgentType = decision.nextAgent
            onProgress(mapAgentToProcessingStep(targetAgentType))

            val supervisorRecord = AgentExecutionRecord(
                agentType = RxAgentType.SUPERVISOR,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = state.items.size,
                executionTimeMs = supervisorTimeMs
            )

            database.saveAgentTrace(
                turnNumber = state.turnCount + 1,
                agentType = RxAgentType.SUPERVISOR.displayName,
                provider = supervisorRuntime.provider.displayName,
                modelName = supervisorRuntime.model,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = state.items.size,
                executionTimeMs = supervisorTimeMs,
                isSuccess = true,
                errorMessage = "",
                flow = "KoogEngine -> SupervisorTrace"
            )

            val workerRuntime = resolveRuntimeConfig(
                agentType = targetAgentType,
                agentConfigs = agentConfigs,
                defaultProvider = targetAgentType.defaultProvider,
                defaultModel = targetAgentType.defaultModel,
                fallbackApiKey = input.providedApiKey,
                fallbackCustomUrl = input.customUrl
            )

            state = updateEpisodicMemoryIfNeeded(state)

            val toolParams = ToolExecutionParams(
                provider = workerRuntime.provider.id,
                model = workerRuntime.model,
                apiKey = workerRuntime.apiKey,
                customUrl = workerRuntime.customUrl,
                feedbackPrompt = decision.feedbackPrompt,
                memoryContext = state.episodicMemory
            )

            val agentStartTime = getPlatformEpochMillis()
            val newItemsResult = executeToolForAgent(targetAgentType, input, state.items, toolParams)
            val agentTimeMs = getPlatformEpochMillis() - agentStartTime

            database.saveAgentTrace(
                turnNumber = state.turnCount + 1,
                agentType = targetAgentType.displayName,
                provider = workerRuntime.provider.displayName,
                modelName = workerRuntime.model,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = if (newItemsResult.isSuccess) newItemsResult.getOrThrow().size else state.items.size,
                executionTimeMs = agentTimeMs,
                isSuccess = newItemsResult.isSuccess,
                errorMessage = newItemsResult.exceptionOrNull()?.message ?: "",
                flow = "KoogEngine -> ToolTrace"
            )

            if (newItemsResult.isFailure) {
                val ex = newItemsResult.exceptionOrNull() ?: IllegalStateException("Tool execution failed.")
                val failureReason = PipelineFailureReason.fromThrowable(ex)

                val failedRecord = AgentExecutionRecord(
                    agentType = targetAgentType,
                    reasoning = decision.reasoning,
                    feedbackPrompt = decision.feedbackPrompt,
                    itemCount = state.items.size,
                    executionTimeMs = agentTimeMs,
                    failureReason = failureReason
                )

                when (targetAgentType) {
                    RxAgentType.TRANSCRIPTION, RxAgentType.ENTITY_PARSER -> return Result.failure(ex)
                    RxAgentType.SAFETY_AUDITOR -> {
                        state = state.copy(
                            history = state.history + supervisorRecord + failedRecord,
                            safetyAudited = false,
                            isSuspendedForApproval = true,
                            approvalReason = "Safety Audit Agent failed (${failureReason.displayName}). Mandatory pharmacist sign-off required.",
                            turnCount = state.turnCount + 1
                        )
                        break
                    }
                    else -> {
                        state = state.copy(
                            history = state.history + supervisorRecord + failedRecord,
                            turnCount = state.turnCount + 1
                        )
                        continue
                    }
                }
            }

            val updatedItems = newItemsResult.getOrThrow()
            val workerRecord = AgentExecutionRecord(
                agentType = targetAgentType,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = updatedItems.size,
                executionTimeMs = agentTimeMs
            )

            val updatedState = when (targetAgentType) {
                RxAgentType.SAFETY_AUDITOR -> state.copy(safetyAudited = true)
                RxAgentType.ENTITY_PARSER -> state.copy(entityParsed = true)
                else -> state
            }

            state = updatedState.copy(
                items = updatedItems,
                history = state.history + supervisorRecord + workerRecord,
                turnCount = state.turnCount + 1
            )
        }

        onProgress(ProcessingStep.COMPLETE)

        return Result.success(
            OrchestrationResult(
                items = state.items,
                safetyAudited = state.safetyAudited,
                entityParsed = state.entityParsed,
                isSuspendedForApproval = state.isSuspendedForApproval,
                approvalReason = state.approvalReason,
                executionHistory = state.history
            )
        )
    }

    private suspend fun executeToolForAgent(
        targetAgentType: RxAgentType,
        input: TranscriptionInput,
        items: List<MedicineItem>,
        params: ToolExecutionParams
    ): Result<List<MedicineItem>> {
        return when (targetAgentType) {
            RxAgentType.TRANSCRIPTION -> rxAgentTools.executeVisionOCR(
                input.copy(
                    provider = AiProvider.fromId(params.provider),
                    selectedModel = params.model,
                    providedApiKey = params.apiKey,
                    customUrl = params.customUrl
                )
            )
            RxAgentType.ENTITY_PARSER -> rxAgentTools.executeEntityParsing(items, params)
            RxAgentType.GENERIC_SUBSTITUTE -> rxAgentTools.executeGenericSubstituteSearch(items, params)
            RxAgentType.SAFETY_AUDITOR -> rxAgentTools.executeSafetyAudit(items, params)
            RxAgentType.SUPERVISOR -> Result.success(items)
        }
    }

    private fun resolveRuntimeConfig(
        agentType: RxAgentType,
        agentConfigs: Map<RxAgentType, AgentConfig>,
        defaultProvider: AiProvider,
        defaultModel: String,
        fallbackApiKey: String,
        fallbackCustomUrl: String
    ): RuntimeConfig {
        val config = agentConfigs[agentType]
        val provider = config?.provider ?: defaultProvider
        val model = config?.selectedModel?.ifBlank { provider.defaultModel } ?: defaultModel
        val apiKey = database.getApiKeyForProvider(provider.id).ifBlank { fallbackApiKey }
        val customUrl = when (provider) {
            AiProvider.Custom -> database.getProviderConfig().customUrl.ifBlank { fallbackCustomUrl }
            else -> ""
        }
        return RuntimeConfig(provider, model, apiKey, customUrl)
    }

    private fun updateEpisodicMemoryIfNeeded(state: SupervisorState): SupervisorState {
        if (state.items.isEmpty()) return state
        val generics = state.items.map { it.genericName }.filter { it.isNotBlank() }
        val memoryContext = database.getEpisodicMemoryContext(generics)
        return if (memoryContext.isNotBlank()) state.copy(episodicMemory = memoryContext) else state
    }

    private fun mapAgentToProcessingStep(agentType: RxAgentType): ProcessingStep {
        return when (agentType) {
            RxAgentType.TRANSCRIPTION -> ProcessingStep.READING_HANDWRITING
            RxAgentType.ENTITY_PARSER -> ProcessingStep.MATCHING_BRANDS
            RxAgentType.GENERIC_SUBSTITUTE -> ProcessingStep.FINDING_SIMILAR
            RxAgentType.SAFETY_AUDITOR -> ProcessingStep.COMPLETE
            RxAgentType.SUPERVISOR -> ProcessingStep.ENCODING
        }
    }

    private data class RuntimeConfig(
        val provider: AiProvider,
        val model: String,
        val apiKey: String,
        val customUrl: String
    )

    companion object {
        private const val TAG = "KoogRxSupervisorEngine"
    }
}
