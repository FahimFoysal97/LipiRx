package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.OrchestrationResult
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.ProcessingStep

class ProcessPrescriptionUseCase(
    private val koogSupervisorEngine: KoogRxSupervisorEngine,
    private val aiService: CompositeAiService
) {
    suspend fun executePipeline(
        input: TranscriptionInput,
        agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
        onProgress: (step: ProcessingStep) -> Unit = {}
    ): Result<OrchestrationResult> {
        return koogSupervisorEngine.runOrchestration(
            input = input,
            agentConfigs = agentConfigs,
            onProgress = onProgress
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
