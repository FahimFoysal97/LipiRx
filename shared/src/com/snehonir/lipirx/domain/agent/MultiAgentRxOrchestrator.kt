package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.ProcessingStep
import io.github.aakira.napier.Napier

data class OrchestrationResult(
    val items: List<MedicineItem>,
    val safetyAudited: Boolean = true,
    val entityParsed: Boolean = true,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val executionHistory: List<AgentExecutionRecord> = emptyList()
)

private data class AgentRuntimeConfig(
    val provider: AiProvider,
    val model: String,
    val apiKey: String,
    val customUrl: String
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
        val pipelineStartTime = getPlatformEpochMillis()
        Napier.d("[$TAG] Initiating Manager/Supervisor-Driven Multiagent Orchestration Pipeline...", tag = TAG)

        val supervisorRuntimeConfig = resolveRuntimeConfig(
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
                provider = supervisorRuntimeConfig.provider,
                selectedModel = supervisorRuntimeConfig.model,
                apiKey = supervisorRuntimeConfig.apiKey,
                customUrl = supervisorRuntimeConfig.customUrl
            )
            val supervisorTimeMs = getPlatformEpochMillis() - supervisorStartTime

            when {
                decision.isSuspendedForApproval -> {
                    Napier.d("[$TAG -> Suspended] Supervisor suspended pipeline for Pharmacist Approval Gate. Reason: ${decision.approvalReason}", tag = TAG)
                    state = state.copy(
                        isSuspendedForApproval = true,
                        approvalReason = decision.approvalReason
                    )
                    break
                }
                decision.isFinished || decision.nextAgent == null -> {
                    Napier.d("[$TAG -> Finished] Supervisor marked orchestration complete at Turn ${state.turnCount}. Reasoning: ${decision.reasoning}", tag = TAG)
                    break
                }
            }

            val targetAgentType = decision.nextAgent
            Napier.d("[$TAG -> Turn ${state.turnCount + 1}] Supervisor dispatches to ${targetAgentType.displayName}. Reason: ${decision.reasoning}", tag = TAG)

            val supervisorRecord = AgentExecutionRecord(
                agentType = RxAgentType.SUPERVISOR,
                reasoning = decision.reasoning,
                feedbackPrompt = decision.feedbackPrompt,
                itemCount = state.items.size,
                executionTimeMs = supervisorTimeMs
            )

            val workerRuntimeConfig = resolveRuntimeConfig(
                agentType = targetAgentType,
                agentConfigs = agentConfigs,
                defaultProvider = targetAgentType.defaultProvider,
                defaultModel = targetAgentType.defaultModel,
                fallbackApiKey = input.providedApiKey,
                fallbackCustomUrl = input.customUrl
            )

            onProgress(mapAgentToProcessingStep(targetAgentType))

            state = updateEpisodicMemoryIfNeeded(state)

            val agentStartTime = getPlatformEpochMillis()
            val newItemsResult = dispatchToWorkerAgent(
                targetAgentType = targetAgentType,
                decision = decision,
                state = state,
                input = input,
                runtimeConfig = workerRuntimeConfig
            )
            val agentTimeMs = getPlatformEpochMillis() - agentStartTime

            if (newItemsResult.isFailure) {
                val ex = newItemsResult.exceptionOrNull() ?: IllegalStateException("${targetAgentType.displayName} execution failed.")
                val failureReason = PipelineFailureReason.fromThrowable(ex)

                Napier.w("[$TAG -> Agent Failure] ${targetAgentType.displayName} failed (${failureReason.displayName}, retryable=${failureReason.isRetryable}): ${ex.message}", tag = TAG)

                val failedRecord = AgentExecutionRecord(
                    agentType = targetAgentType,
                    reasoning = decision.reasoning,
                    feedbackPrompt = decision.feedbackPrompt,
                    itemCount = state.items.size,
                    executionTimeMs = agentTimeMs,
                    failureReason = failureReason
                )

                when (targetAgentType) {
                    RxAgentType.TRANSCRIPTION -> {
                        Napier.e("[$TAG -> Fatal] Primary Vision Transcription Agent failed. Aborting pipeline execution.", throwable = ex, tag = TAG)
                        return Result.failure(ex)
                    }
                    RxAgentType.ENTITY_PARSER -> {
                        Napier.e("[$TAG -> Fatal] Medical Entity Parser Agent failed. Cannot guarantee entity normalization. Aborting pipeline.", throwable = ex, tag = TAG)
                        return Result.failure(ex)
                    }
                    RxAgentType.SAFETY_AUDITOR -> {
                        Napier.w("[$TAG -> Safety Gate] Safety Auditor Agent failed. Marking safetyAudited=false and suspending for Pharmacist Sign-off.", tag = TAG)
                        state = state.copy(
                            history = state.history + supervisorRecord + failedRecord,
                            safetyAudited = false,
                            isSuspendedForApproval = true,
                            approvalReason = "Safety Audit Agent failed (${failureReason.displayName}). Mandatory pharmacist sign-off required.",
                            turnCount = state.turnCount + 1
                        )
                        break
                    }
                    RxAgentType.GENERIC_SUBSTITUTE -> {
                        Napier.w("[$TAG -> Degradable Failure] Generic & Substitute Agent failed (${failureReason.displayName}). Continuing pipeline with items un-enriched.", tag = TAG)
                        state = state.copy(
                            history = state.history + supervisorRecord + failedRecord,
                            turnCount = state.turnCount + 1
                        )
                        continue
                    }
                    RxAgentType.SUPERVISOR -> {
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

        val totalPipelineTimeMs = getPlatformEpochMillis() - pipelineStartTime
        logTimingReport(state.history, totalPipelineTimeMs)

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

    private fun resolveRuntimeConfig(
        agentType: RxAgentType,
        agentConfigs: Map<RxAgentType, AgentConfig>,
        defaultProvider: AiProvider,
        defaultModel: String,
        fallbackApiKey: String,
        fallbackCustomUrl: String
    ): AgentRuntimeConfig {
        val config = agentConfigs[agentType]
        val provider = config?.provider ?: defaultProvider
        val model = config?.selectedModel?.ifBlank { provider.defaultModel } ?: defaultModel
        val apiKey = database.getApiKeyForProvider(provider.id).ifBlank { fallbackApiKey }
        val customUrl = when (provider) {
            AiProvider.Custom -> database.getProviderConfig().customUrl.ifBlank { fallbackCustomUrl }
            else -> ""
        }
        return AgentRuntimeConfig(
            provider = provider,
            model = model,
            apiKey = apiKey,
            customUrl = customUrl
        )
    }

    private fun updateEpisodicMemoryIfNeeded(state: SupervisorState): SupervisorState {
        if (state.items.isEmpty()) return state
        val generics = state.items.map { it.genericName }.filter { it.isNotBlank() }
        val memoryContext = database.getEpisodicMemoryContext(generics)
        return if (memoryContext.isNotBlank()) {
            state.copy(episodicMemory = memoryContext)
        } else {
            state
        }
    }

    private suspend fun dispatchToWorkerAgent(
        targetAgentType: RxAgentType,
        decision: SupervisorDecision,
        state: SupervisorState,
        input: TranscriptionInput,
        runtimeConfig: AgentRuntimeConfig
    ): Result<List<MedicineItem>> {
        return when (targetAgentType) {
            RxAgentType.TRANSCRIPTION -> {
                val stage1Input = input.copy(
                    provider = runtimeConfig.provider,
                    selectedModel = runtimeConfig.model,
                    providedApiKey = runtimeConfig.apiKey,
                    customUrl = runtimeConfig.customUrl
                )
                visionTranscriptionAgent.execute(stage1Input)
            }
            RxAgentType.ENTITY_PARSER -> {
                medicalEntityParserAgent.execute(
                    input = state.items,
                    provider = runtimeConfig.provider,
                    selectedModel = runtimeConfig.model,
                    apiKey = runtimeConfig.apiKey,
                    customUrl = runtimeConfig.customUrl,
                    feedbackPrompt = decision.feedbackPrompt
                )
            }
            RxAgentType.GENERIC_SUBSTITUTE -> {
                genericSubstituteAgent.execute(
                    input = state.items,
                    provider = runtimeConfig.provider,
                    selectedModel = runtimeConfig.model,
                    apiKey = runtimeConfig.apiKey,
                    customUrl = runtimeConfig.customUrl,
                    feedbackPrompt = decision.feedbackPrompt,
                    episodicMemory = state.episodicMemory
                )
            }
            RxAgentType.SAFETY_AUDITOR -> {
                safetyAuditorAgent.execute(
                    input = state.items,
                    provider = runtimeConfig.provider,
                    selectedModel = runtimeConfig.model,
                    apiKey = runtimeConfig.apiKey,
                    customUrl = runtimeConfig.customUrl,
                    feedbackPrompt = decision.feedbackPrompt,
                    episodicMemory = state.episodicMemory
                )
            }
            RxAgentType.SUPERVISOR -> Result.success(state.items)
        }
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

    private fun logTimingReport(history: List<AgentExecutionRecord>, totalTimeMs: Long) {
        val timingReport = buildString {
            appendLine()
            appendLine("==========================================================================")
            appendLine("⏱️ MULTI-AGENT EXECUTION TIMING REPORT (Total: ${totalTimeMs}ms)")
            appendLine("==========================================================================")
            history.forEachIndexed { index, record ->
                val status = if (record.failureReason != null) "❌ FAILED (${record.failureReason.displayName})" else "✓ OK"
                appendLine("  Step ${index + 1}: ${record.agentType.displayName.padEnd(30)} -> ${record.executionTimeMs}ms [$status] (${record.itemCount} items)")
            }
            appendLine("==========================================================================")
        }
        Napier.i("[$TAG -> Performance] $timingReport", tag = TAG)
    }

    companion object {
        private const val TAG = "MultiAgentRxOrchestrator"
    }
}
