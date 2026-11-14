package com.snehonir.lipirx.domain.agent

import kotlinx.serialization.Serializable

@Serializable
internal data class SupervisorDecisionDto(
    val nextAgent: String? = null,
    val reasoning: String? = null,
    val feedbackPrompt: String? = null,
    val isSuspendedForApproval: Boolean? = null,
    val approvalReason: String? = null
)
