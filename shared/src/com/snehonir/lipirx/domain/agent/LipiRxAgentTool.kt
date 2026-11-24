package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.agent.koog.dto.ToolExecutionParams

interface LipiRxAgentTool<in Input, Output> {
    val agentType: RxAgentType
    suspend fun execute(input: Input, params: ToolExecutionParams): Result<Output>
}
