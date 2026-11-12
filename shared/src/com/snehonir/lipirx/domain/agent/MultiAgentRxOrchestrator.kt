package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.ProcessingStep

data class OrchestrationResult(
    val items: List<MedicineItem>,
    val safetyAudited: Boolean = true,
    val entityParsed: Boolean = true,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val executionHistory: List<AgentExecutionRecord> = emptyList()
)

class MultiAgentRxOrchestrator(
    private val koogSupervisorEngine: KoogRxSupervisorEngine
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
}
