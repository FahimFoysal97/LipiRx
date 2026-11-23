package com.snehonir.lipirx.domain.agent.koog

import com.snehonir.lipirx.common.AppConstants.MAX_SUPERVISOR_TURNS
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.AgentExecutionRecord
import com.snehonir.lipirx.domain.agent.OrchestrationResult
import com.snehonir.lipirx.domain.agent.PipelineFailureReason
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.RxSupervisorAgent
import com.snehonir.lipirx.domain.agent.SupervisorConstants
import com.snehonir.lipirx.domain.agent.SupervisorState
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.agent.SelfHealingAgentExecutor
import com.snehonir.lipirx.domain.agent.koog.dto.ToolExecutionParams
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class KoogRxSupervisorEngine(
    private val rxSupervisorAgent: RxSupervisorAgent,
    private val rxAgentTools: RxAgentTools,
    private val repository: PrescriptionRepository,
    private val selfHealingExecutor: SelfHealingAgentExecutor = SelfHealingAgentExecutor()
) {
    suspend fun runOrchestration(
        input: TranscriptionInput,
        initialItems: List<MedicineItem> = emptyList(),
        agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
        onProgress: (agentType: RxAgentType, reasoning: String) -> Unit = { _, _ -> }
    ): Result<OrchestrationResult> {
        Napier.d("[$TAG] Starting JetBrains Koog Multi-Agent Supervisor Engine...", tag = TAG)
        onProgress(RxAgentType.SUPERVISOR, SupervisorConstants.PROGRESS_EVALUATING_WORKSPACE)

        val supervisorRuntime = resolveRuntimeConfig(
            agentType = RxAgentType.SUPERVISOR,
            agentConfigs = agentConfigs,
            fallbackApiKey = input.providedApiKey,
            fallbackCustomUrl = input.customUrl
        )

        var state = SupervisorState(input = input, items = initialItems)
        val maxTurns = MAX_SUPERVISOR_TURNS

        while (!state.isFinished && state.turnCount < maxTurns) {
            val supervisorStartTime = getPlatformEpochMillis()
            val decision = rxSupervisorAgent.decideNextStep(
                state = state,
                provider = supervisorRuntime.provider,
                selectedModel = supervisorRuntime.model,
                apiKey = supervisorRuntime.apiKey,
                customUrl = supervisorRuntime.customUrl,
                strategy = input.strategy
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

            if (decision.isFinished || (decision.nextAgent == null && decision.parallelAgents.isEmpty())) {
                Napier.d("[$TAG -> Complete] Supervisor finished execution at turn ${state.turnCount}", tag = TAG)
                break
            }

            // Handle Explicit Multi-Agent Parallel Execution (e.g. Generic Substitute Search + Safety Audit)
            if (decision.parallelAgents.size > 1) {
                val parallelWorkers = decision.parallelAgents
                Napier.d("[$TAG -> Parallel Dispatch Turn ${state.turnCount + 1}] Concurrently executing worker agents: ${parallelWorkers.map { it.id }}...", tag = TAG)
                onProgress(parallelWorkers.first(), decision.reasoning)

                val supervisorRecord = AgentExecutionRecord(
                    agentType = RxAgentType.SUPERVISOR,
                    reasoning = decision.reasoning,
                    feedbackPrompt = decision.feedbackPrompt,
                    itemCount = state.items.size,
                    executionTimeMs = supervisorTimeMs
                )

                repository.saveAgentTrace(
                    turnNumber = state.turnCount + 1,
                    agentType = RxAgentType.SUPERVISOR.id,
                    provider = supervisorRuntime.provider.id,
                    modelName = supervisorRuntime.model,
                    reasoning = decision.reasoning,
                    feedbackPrompt = decision.feedbackPrompt,
                    itemCount = state.items.size,
                    executionTimeMs = supervisorTimeMs,
                    isSuccess = true,
                    errorMessage = "",
                    flow = "KoogEngine -> ParallelSupervisorTrace"
                )

                state = updateEpisodicMemoryIfNeeded(state)

                val agentStartTime = getPlatformEpochMillis()
                val parallelResults = coroutineScope {
                    parallelWorkers.map { agentType ->
                        val workerRuntime = resolveRuntimeConfig(
                            agentType = agentType,
                            agentConfigs = agentConfigs,
                            fallbackApiKey = input.providedApiKey,
                            fallbackCustomUrl = input.customUrl
                        )
                        val toolParams = ToolExecutionParams(
                            provider = workerRuntime.provider.id,
                            model = workerRuntime.model,
                            apiKey = workerRuntime.apiKey,
                            customUrl = workerRuntime.customUrl,
                            feedbackPrompt = decision.feedbackPrompt,
                            memoryContext = state.episodicMemory,
                            strategy = input.strategy.id,
                            fallbackProvider = workerRuntime.fallbackProvider.id,
                            fallbackModel = workerRuntime.fallbackModel
                        )
                        async {
                            val res = executeToolForAgent(agentType, input, state.items, toolParams)
                            Triple(agentType, workerRuntime, res)
                        }
                    }.awaitAll()
                }
                val agentTimeMs = getPlatformEpochMillis() - agentStartTime

                // Record individual AgentOps trace logs for each parallel worker agent
                parallelResults.forEach { (agentType, runtime, res) ->
                    repository.saveAgentTrace(
                        turnNumber = state.turnCount + 1,
                        agentType = agentType.id,
                        provider = runtime.provider.id,
                        modelName = runtime.model,
                        reasoning = decision.reasoning,
                        feedbackPrompt = decision.feedbackPrompt,
                        itemCount = if (res.isSuccess) res.getOrThrow().size else state.items.size,
                        executionTimeMs = agentTimeMs,
                        isSuccess = res.isSuccess,
                        errorMessage = res.exceptionOrNull()?.message ?: "",
                        flow = "KoogEngine -> ParallelToolTrace"
                    )
                }

                val mergedItems = rxAgentTools.mergeParallelResults(state.items, parallelResults.map { it.third })
                val workerRecords = parallelResults.map { (agentType, _, _) ->
                    AgentExecutionRecord(
                        agentType = agentType,
                        reasoning = decision.reasoning,
                        feedbackPrompt = decision.feedbackPrompt,
                        itemCount = mergedItems.size,
                        executionTimeMs = agentTimeMs
                    )
                }

                state = state.copy(
                    items = mergedItems,
                    safetyAudited = parallelWorkers.contains(RxAgentType.SAFETY_AUDITOR),
                    history = state.history + supervisorRecord + workerRecords,
                    turnCount = state.turnCount + 1
                )
                continue
            }

            val targetAgentType = decision.nextAgent ?: break
            Napier.d("[$TAG -> Turn ${state.turnCount + 1}] Dispatching agent '${targetAgentType.id}' with reasoning: '${decision.reasoning}'", tag = TAG)
            onProgress(targetAgentType, decision.reasoning)

            val supervisorRecord = AgentExecutionRecord(
                agentType = RxAgentType.SUPERVISOR,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = state.items.size,
                executionTimeMs = supervisorTimeMs
            )

            repository.saveAgentTrace(
                turnNumber = state.turnCount + 1,
                agentType = RxAgentType.SUPERVISOR.id,
                provider = supervisorRuntime.provider.id,
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
                memoryContext = state.episodicMemory,
                strategy = input.strategy.id,
                fallbackProvider = workerRuntime.fallbackProvider.id,
                fallbackModel = workerRuntime.fallbackModel
            )

            val agentStartTime = getPlatformEpochMillis()
            val newItemsResult = selfHealingExecutor.executeWithSelfHealing(targetAgentType.id) { repairPrompt ->
                val currentParams = if (repairPrompt.isNotBlank()) {
                    toolParams.copy(
                        feedbackPrompt = if (toolParams.feedbackPrompt.isNotBlank()) {
                            "${toolParams.feedbackPrompt}\n$repairPrompt"
                        } else {
                            repairPrompt
                        }
                    )
                } else {
                    toolParams
                }
                executeToolForAgent(targetAgentType, input, state.items, currentParams)
            }
            val agentTimeMs = getPlatformEpochMillis() - agentStartTime
            Napier.d("[$TAG -> ToolResult] Agent '${targetAgentType.id}' execution finished in ${agentTimeMs}ms. Success=${newItemsResult.isSuccess}, ExtractedItemsCount=${if (newItemsResult.isSuccess) newItemsResult.getOrThrow().size else 0}", tag = TAG)

            repository.saveAgentTrace(
                turnNumber = state.turnCount + 1,
                agentType = targetAgentType.id,
                provider = workerRuntime.provider.id,
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
                            approvalReason = com.snehonir.lipirx.domain.model.ClinicalApprovalReasons.auditAgentFailed(failureReason.displayName),
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

        onProgress(RxAgentType.SAFETY_AUDITOR, SupervisorConstants.PROGRESS_FINALIZING_AUDIT)

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
                    customUrl = params.customUrl,
                    fallbackProvider = AiProvider.fromId(params.fallbackProvider),
                    fallbackModel = params.fallbackModel
                )
            )
            RxAgentType.ENTITY_PARSER -> rxAgentTools.executeEntityParsing(items, params)
            RxAgentType.GENERIC_SUBSTITUTE -> rxAgentTools.executeGenericSubstituteSearch(items, params)
            RxAgentType.SAFETY_AUDITOR -> rxAgentTools.executeSafetyAudit(items, params)
            RxAgentType.SUPERVISOR -> Result.success(items)
        }
    }

    private suspend fun resolveRuntimeConfig(
        agentType: RxAgentType,
        agentConfigs: Map<RxAgentType, AgentConfig>,
        fallbackApiKey: String,
        fallbackCustomUrl: String
    ): RuntimeConfig {
        val config = agentConfigs[agentType] ?: repository.getAgentConfig(agentType)
        val provider = config.provider
        val savedProviderModel = repository.getLastSelectedModel(provider, flow = "KoogEngine -> getLastSelectedModel")
            .ifBlank { provider.defaultModel }
        val model = config.selectedModel.ifBlank { savedProviderModel }

        val activeStrategy = repository.getOnDeviceStrategy()
        val defaultFallbackProv = when (activeStrategy) {
            OnDeviceStrategy.CLOUD_ONLY -> if (provider != AiProvider.OnDevice) provider else AiProvider.Gemini
            OnDeviceStrategy.ON_DEVICE_ONLY -> AiProvider.OnDevice
            OnDeviceStrategy.HYBRID_FALLBACK -> AiProvider.OnDevice
            OnDeviceStrategy.ON_DEVICE_FIRST -> AiProvider.Gemini
        }
        val fallbackProvider = config.fallbackProvider ?: defaultFallbackProv
        val fallbackModel = config.fallbackModel.ifBlank {
            repository.getLastSelectedModel(fallbackProvider, flow = "KoogEngine -> getFallbackModel")
                .ifBlank { fallbackProvider.defaultModel }
        }

        val apiKey = repository.getApiKeyForProvider(provider.id).ifBlank { fallbackApiKey }
        val customUrl = when (provider) {
            AiProvider.Custom -> repository.getProviderConfig().customUrl.ifBlank { fallbackCustomUrl }
            else -> ""
        }

        return RuntimeConfig(provider, model, apiKey, customUrl, fallbackProvider, fallbackModel)
    }

    private fun updateEpisodicMemoryIfNeeded(state: SupervisorState): SupervisorState {
        if (state.history.isEmpty()) return state

        val memoryBuilder = StringBuilder()
        memoryBuilder.appendLine("PAST EXECUTIONS & MEMORY:")
        state.history.forEach { record ->
            memoryBuilder.appendLine("- Agent ${record.agentType.id}: Reason='${record.reasoning}', OutputItems=${record.itemCount}, Success=${record.failureReason == null}")
            if (record.failureReason != null) {
                memoryBuilder.appendLine("  * Error: ${record.failureReason.displayName}")
            }
        }
        return state.copy(episodicMemory = memoryBuilder.toString())
    }

    companion object {
        private const val TAG = "KoogRxSupervisorEngine"
    }

    private data class RuntimeConfig(
        val provider: AiProvider,
        val model: String,
        val apiKey: String,
        val customUrl: String,
        val fallbackProvider: AiProvider = AiProvider.OnDevice,
        val fallbackModel: String = ""
    )
}
