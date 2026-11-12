package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.MedicineItem

data class OrchestrationResult(
    val items: List<MedicineItem>,
    val safetyAudited: Boolean = true,
    val entityParsed: Boolean = true,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val executionHistory: List<AgentExecutionRecord> = emptyList()
)
