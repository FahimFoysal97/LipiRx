package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.MedicineItem

data class SupervisorState(
    val input: TranscriptionInput,
    val items: List<MedicineItem> = emptyList(),
    val history: List<AgentExecutionRecord> = emptyList(),
    val episodicMemory: String = "",
    val turnCount: Int = 0,
    val isFinished: Boolean = false,
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String = "",
    val safetyAudited: Boolean = true,
    val entityParsed: Boolean = true
)
