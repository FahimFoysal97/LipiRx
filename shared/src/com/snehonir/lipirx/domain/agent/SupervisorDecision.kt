package com.snehonir.lipirx.domain.agent

/**
 * Represents a decision made by the Supervisor Agent during orchestration.
 * Supports single agent dispatch via [nextAgent] or explicit multi-agent parallel execution via [parallelAgents].
 */
data class SupervisorDecision(
    val nextAgent: RxAgentType?,
    val parallelAgents: List<RxAgentType> = emptyList(),
    val isFinished: Boolean,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val reasoning: String,
    val feedbackPrompt: String
)
