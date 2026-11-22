package com.snehonir.lipirx.presentation.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.domain.eval.RxAgentEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TelemetryUiState(
    val traces: List<AgentExecutionTraceEntity> = emptyList(),
    val totalPipelineTimeMs: Long = 0L,
    val successRate: Float = 0f,
    val isLoading: Boolean = false
)

class AgentTraceViewModel(
    private val repository: PrescriptionRepository,
    private val rxAgentEvaluator: RxAgentEvaluator? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init {
        loadAgentTraces()
    }

    fun loadAgentTraces(flow: String = "App -> AgentTraceViewModel.loadAgentTraces") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val rawTraces = runCatching {
                repository.getAgentTraces(limit = 100, flow = flow)
            }.getOrDefault(emptyList())

            val totalTime = rawTraces.sumOf { it.executionTimeMs }
            val successCount = rawTraces.count { it.isSuccess }
            val rate = if (rawTraces.isNotEmpty()) successCount.toFloat() / rawTraces.size.toFloat() else 1f

            _uiState.update {
                it.copy(
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                evaluator.runEvaluationSuite()
            }
            loadAgentTraces(flow = flow)
        }
    }

    fun deleteTraces(traceIds: List<Long>, flow: String = "App -> AgentTraceViewModel.deleteTraces") {
        if (traceIds.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.deleteAgentTraces(traceIds = traceIds, flow = flow)
            }
            loadAgentTraces(flow = flow)
        }
    }

    fun clearAllTraces(flow: String = "App -> AgentTraceViewModel.clearAllTraces") {
        viewModelScope.launch {
            runCatching {
                repository.clearAgentTraces(flow = flow)
            }
            loadAgentTraces(flow = flow)
        }
    }
}
