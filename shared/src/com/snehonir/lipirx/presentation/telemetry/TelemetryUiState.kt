package com.snehonir.lipirx.presentation.telemetry

import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.domain.agent.RxAgentType

data class TelemetryUiState(
    val traces: List<AgentExecutionTraceEntity> = emptyList(),
    val totalPipelineTimeMs: Long = 0L,
    val successRate: Float = 0f,
    val isLoading: Boolean = false
)

internal data class TelemetryScanSession(
    val sessionId: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val totalTimeMs: Long,
    val totalHops: Int,
    val isSuccess: Boolean,
    val failedTurnCount: Int,
    val finalItemCount: Int,
    val modelSummary: String,
    val traces: List<AgentExecutionTraceEntity>
)

internal fun groupTracesIntoScanSessions(rawTraces: List<AgentExecutionTraceEntity>): List<TelemetryScanSession> {
    if (rawTraces.isEmpty()) return emptyList()

    val chronologicalTraces = rawTraces.sortedBy { it.id }
    val tracesBySessionId = chronologicalTraces.groupBy { it.sessionId }
    val sessionGroups = mutableListOf<List<AgentExecutionTraceEntity>>()

    for ((sessionId, traces) in tracesBySessionId) {
        var currentGroup: MutableList<AgentExecutionTraceEntity>? = null
        for (trace in traces) {
            val lastTrace = currentGroup?.lastOrNull()
            val isNewScan = if (sessionId.isNotBlank()) {
                currentGroup == null ||
                        (lastTrace != null && trace.turnNumber == 1 && trace.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true)) ||
                        (lastTrace != null && trace.turnNumber < lastTrace.turnNumber)
            } else {
                currentGroup == null ||
                        (trace.turnNumber == 1 && trace.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true)) ||
                        (trace.turnNumber == 1 && lastTrace?.turnNumber != 1) ||
                        (lastTrace != null && trace.turnNumber < lastTrace.turnNumber) ||
                        (lastTrace != null && (trace.timestamp - lastTrace.timestamp) > 30_000L)
            }

            if (isNewScan) {
                val newGroup = mutableListOf<AgentExecutionTraceEntity>()
                currentGroup = newGroup
                sessionGroups.add(newGroup)
            }
            currentGroup?.add(trace)
        }
    }

    return sessionGroups.mapIndexed { idx, sessionTraces ->
        val firstTrace = sessionTraces.first()
        val lastTrace = sessionTraces.last()

        val totalTime = sessionTraces.groupBy { it.turnNumber }.values.sumOf { turnTraces ->
            val supervisorMs = turnTraces.firstOrNull { it.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true) }?.executionTimeMs ?: 0L
            val maxWorkerMs = turnTraces.filterNot { it.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true) }.maxOfOrNull { it.executionTimeMs } ?: 0L
            supervisorMs + maxWorkerMs
        }

        val allSuccessful = sessionTraces.all { it.isSuccess }
        val failedCount = sessionTraces.count { !it.isSuccess }

        val providers = sessionTraces.map { it.provider }.filter { it.isNotBlank() }.distinct()
        val models = sessionTraces.map { it.modelName }.filter { it.isNotBlank() }.distinct()

        val summary = when {
            providers.isEmpty() -> ""
            providers.size == 1 && models.size == 1 -> "${providers.first()} • ${models.first()}"
            providers.size == 1 -> "${providers.first()} (${models.joinToString(", ")})"
            else -> "Multi-Provider (${providers.joinToString(", ")})"
        }

        val finalItemCount = sessionTraces.lastOrNull { it.itemCount > 0 }?.itemCount ?: lastTrace.itemCount
        val rawSessionId = firstTrace.sessionId.ifBlank { "scan_${firstTrace.id}" }
        val hasDuplicateSessionId = sessionGroups.count { it.first().sessionId == firstTrace.sessionId } > 1
        val effectiveSessionId = if (hasDuplicateSessionId || firstTrace.sessionId.isBlank()) {
            "${rawSessionId}_${firstTrace.id}_$idx"
        } else {
            firstTrace.sessionId
        }

        TelemetryScanSession(
            sessionId = effectiveSessionId,
            timestamp = lastTrace.timestamp,
            formattedDateTime = lastTrace.formattedDateTime,
            totalTimeMs = totalTime,
            totalHops = sessionTraces.size,
            isSuccess = allSuccessful,
            failedTurnCount = failedCount,
            finalItemCount = finalItemCount,
            modelSummary = summary,
            traces = sessionTraces
        )
    }.sortedByDescending { it.timestamp }
}
