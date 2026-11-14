package com.snehonir.lipirx.domain.agent

data class SupervisorDecision(
    val nextAgent: RxAgentType?,
    val isFinished: Boolean,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val reasoning: String,
    val feedbackPrompt: String
)
