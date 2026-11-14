package com.snehonir.lipirx.domain.agent

data class AgentExecutionRecord(
    val agentType: RxAgentType,
    val reasoning: String,
    val feedbackPrompt: String,
    val itemCount: Int,
    val executionTimeMs: Long = 0L,
    val failureReason: PipelineFailureReason? = null
)
