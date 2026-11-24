package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.remote.service.CompositeAiService
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.OrchestrationResult
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.common.LipiRxSuspendUseCase
import com.snehonir.lipirx.domain.model.AiProvider

data class ProcessPrescriptionParams(
    val input: TranscriptionInput,
    val agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    val onEvent: (PipelineEvent) -> Unit = {}
)

class ProcessPrescriptionUseCase(
    private val koogSupervisorEngine: KoogRxSupervisorEngine
) : LipiRxSuspendUseCase<ProcessPrescriptionParams, OrchestrationResult> {

    override suspend fun invoke(params: ProcessPrescriptionParams): Result<OrchestrationResult> {
        return koogSupervisorEngine.runOrchestration(
            input = params.input,
            agentConfigs = params.agentConfigs,
            onEvent = params.onEvent
        )
    }
}

data class FetchModelsParams(
    val provider: AiProvider,
    val providedApiKey: String = "",
    val customUrl: String = "",
    val flow: String = ""
)

class FetchAvailableModelsUseCase(
    private val aiService: CompositeAiService
) : LipiRxSuspendUseCase<FetchModelsParams, List<String>> {

    override suspend fun invoke(params: FetchModelsParams): Result<List<String>> {
        return aiService.fetchAvailableModels(
            providedApiKey = params.providedApiKey,
            provider = params.provider,
            customUrl = params.customUrl,
            flow = params.flow
        ).map { models -> models.map { it.name } }
    }
}
