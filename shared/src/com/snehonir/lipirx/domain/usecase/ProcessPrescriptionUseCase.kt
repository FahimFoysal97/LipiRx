package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.remote.service.CompositeAiService
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.OrchestrationResult
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.model.AiProvider

class ProcessPrescriptionUseCase(
    private val koogSupervisorEngine: KoogRxSupervisorEngine,
    private val aiService: CompositeAiService
) {
    suspend fun executePipeline(
        input: TranscriptionInput,
        agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
        onEvent: (PipelineEvent) -> Unit = {}
    ): Result<OrchestrationResult> {
        return koogSupervisorEngine.runOrchestration(
            input = input,
            agentConfigs = agentConfigs,
            onEvent = onEvent
        )
    }

    suspend fun fetchAvailableModels(
        provider: AiProvider,
        providedApiKey: String = "",
        customUrl: String = "",
        flow: String = ""
    ): Result<List<String>> {
        return aiService.fetchAvailableModels(
            provider = provider,
            providedApiKey = providedApiKey,
            customUrl = customUrl,
            flow = flow
        ).map { models -> models.map { it.name } }
    }
}
