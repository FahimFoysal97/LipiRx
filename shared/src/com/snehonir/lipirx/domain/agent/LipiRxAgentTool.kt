package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.agent.koog.dto.ToolExecutionParams

interface LipiRxAgentTool<in Input, Output> {
    val agentType: RxAgentType
    val agentName: String get() = agentType.id
    val description: String get() = agentType.id

    suspend fun execute(input: Input, params: ToolExecutionParams): Result<Output>
}