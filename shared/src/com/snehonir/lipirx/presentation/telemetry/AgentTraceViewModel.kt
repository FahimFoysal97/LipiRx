package com.snehonir.lipirx.presentation.telemetry

import com.snehonir.lipirx.domain.eval.RxAgentEvaluator
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch

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
                evaluator.runEvaluationSuite(flow = flow)
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
