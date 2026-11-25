package com.snehonir.lipirx.presentation.telemetry

import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.eval.RxAgentEvaluator
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.presentation.common.LipiRxViewModel

import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch

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
    val sessionGroups = mutableListOf<MutableList<AgentExecutionTraceEntity>>()
    var currentGroup: MutableList<AgentExecutionTraceEntity>? = null

    for (trace in chronologicalTraces) {
        val lastTrace = currentGroup?.lastOrNull()
        val isNewScan = currentGroup == null ||
                (trace.turnNumber == 1 && trace.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true)) ||
                (trace.turnNumber == 1 && lastTrace?.turnNumber != 1) ||
                (lastTrace != null && trace.turnNumber < lastTrace.turnNumber) ||
                (lastTrace != null && (trace.timestamp - lastTrace.timestamp) > 30_000L)

        if (isNewScan) {
            currentGroup = mutableListOf()
            sessionGroups.add(currentGroup)
        }
        currentGroup.add(trace)
    }

    return sessionGroups.mapIndexed { idx, sessionTraces ->
        val firstTrace = sessionTraces.first()
        val lastTrace = sessionTraces.last()

        // Calculate true wall-clock session duration by summing per-turn durations.
        // For parallel agents in the same turn, their execution times overlap, so we take max(workerTimeMs).
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

        TelemetryScanSession(
            sessionId = "scan_${firstTrace.id}_$idx",
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

class AgentTraceViewModel(
    private val repository: PrescriptionRepository,
    private val rxAgentEvaluator: RxAgentEvaluator? = null
) : LipiRxViewModel<TelemetryUiState>(TelemetryUiState()) {

    private var observeJob: Job? = null

    init {
        loadAgentTraces()
    }

    fun loadAgentTraces(flow: String = "App -> AgentTraceViewModel.loadAgentTraces") {
        observeJob?.cancel()
        observeJob = launchSafe {
            updateState { copy(isLoading = true) }
            repository.getAgentTracesFlow(limit = 100, flow = flow)
                .catch { ex ->
                    Napier.e("[$flow] Failed to observe agent traces: ${ex.message}", ex)
                    updateState { copy(isLoading = false) }
                }
                .collect { rawTraces ->
                    val sessions = groupTracesIntoScanSessions(rawTraces)
                    val totalTime = sessions.sumOf { it.totalTimeMs }
                    val successCount = rawTraces.count { it.isSuccess }
                    val rate = if (rawTraces.isNotEmpty()) successCount.toFloat() / rawTraces.size.toFloat() else 1f

                    updateState {
                        copy(
                            traces = rawTraces,
                            totalPipelineTimeMs = totalTime,
                            successRate = rate,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun runSampleBenchmarkTest(flow: String = "App -> AgentTraceViewModel.runSampleBenchmarkTest") {
        val evaluator = rxAgentEvaluator ?: return
        launchSafe {
            updateState { copy(isLoading = true) }
            runCatching {
                evaluator.runEvaluationSuite()
            }
            updateState { copy(isLoading = false) }
        }
    }

    fun deleteTraces(traceIds: List<Long>, flow: String = "App -> AgentTraceViewModel.deleteTraces") {
        if (traceIds.isEmpty()) return
        launchSafe {
            runCatching {
                repository.deleteAgentTraces(traceIds = traceIds, flow = flow)
            }
        }
    }

    fun clearAllTraces(flow: String = "App -> AgentTraceViewModel.clearAllTraces") {
        launchSafe {
            runCatching {
                repository.clearAgentTraces(flow = flow)
            }
        }
    }
}
