package com.snehonir.lipirx.presentation.telemetry

import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.domain.eval.RxAgentEvaluator
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.presentation.common.LipiRxViewModel

data class TelemetryUiState(
    val traces: List<AgentExecutionTraceEntity> = emptyList(),
    val totalPipelineTimeMs: Long = 0L,
    val successRate: Float = 0f,
    val isLoading: Boolean = false
)

class AgentTraceViewModel(
    private val repository: PrescriptionRepository,
    private val rxAgentEvaluator: RxAgentEvaluator? = null
) : LipiRxViewModel<TelemetryUiState>(TelemetryUiState()) {

    init {
        loadAgentTraces()
    }

    fun loadAgentTraces(flow: String = "App -> AgentTraceViewModel.loadAgentTraces") {
        launchSafe {
            updateState { copy(isLoading = true) }
            val rawTraces = runCatching {
                repository.getAgentTraces(limit = 100, flow = flow)
            }.getOrDefault(emptyList())

            val totalTime = rawTraces.sumOf { it.executionTimeMs }
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

    fun runSampleBenchmarkTest(flow: String = "App -> AgentTraceViewModel.runSampleBenchmarkTest") {
        val evaluator = rxAgentEvaluator ?: return
        launchSafe {
            updateState { copy(isLoading = true) }
            runCatching {
                evaluator.runEvaluationSuite()
            }
            loadAgentTraces(flow = flow)
        }
    }

    fun deleteTraces(traceIds: List<Long>, flow: String = "App -> AgentTraceViewModel.deleteTraces") {
        if (traceIds.isEmpty()) return
        launchSafe {
            runCatching {
                repository.deleteAgentTraces(traceIds = traceIds, flow = flow)
            }
            loadAgentTraces(flow = flow)
        }
    }

    fun clearAllTraces(flow: String = "App -> AgentTraceViewModel.clearAllTraces") {
        launchSafe {
            runCatching {
                repository.clearAgentTraces(flow = flow)
            }
            loadAgentTraces(flow = flow)
        }
    }
}
