package com.snehonir.lipirx.presentation.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalysisLoadingUiState(
    val imageByteArray: ByteArray? = null,
    val isLoading: Boolean = false,
    val loadingStep: Int = 0,
    val statusText: String = "",
    val progressPercent: Int = 0,
    val currentAgent: RxAgentType = RxAgentType.SUPERVISOR
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalysisLoadingUiState) return false

        if (isLoading != other.isLoading) return false
        if (loadingStep != other.loadingStep) return false
        if (statusText != other.statusText) return false
        if (progressPercent != other.progressPercent) return false
        if (currentAgent != other.currentAgent) return false
        if (imageByteArray != null) {
            if (other.imageByteArray == null) return false
            if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        } else if (other.imageByteArray != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + loadingStep.hashCode()
        result = 31 * result + statusText.hashCode()
        result = 31 * result + progressPercent.hashCode()
        result = 31 * result + currentAgent.hashCode()
        result = 31 * result + (imageByteArray?.contentHashCode() ?: 0)
        return result
    }
}

class AnalysisLoadingViewModel(
    private val sessionState: PrescriptionSessionState
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisLoadingUiState())
    val uiState: StateFlow<AnalysisLoadingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.session.collect { data ->
                _uiState.update {
                    it.copy(
                        imageByteArray = data.imageByteArray,
                        isLoading = data.isLoading,
                        loadingStep = data.loadingStep,
                        statusText = data.loadingStatusText,
                        progressPercent = data.activeAgentProgressPercent,
                        currentAgent = data.currentAgent
                    )
                }
            }
        }
    }

    fun stopAnalysis(flow: String = "com.snehonir.lipirx.App -> AnalysisLoadingViewModel.stopAnalysis") {
        Napier.d("[$flow] Cancelling ongoing analysis", tag = TAG)
        sessionState.cancelActiveAnalysis()
    }

    companion object {
        private const val TAG = "AnalysisLoadingViewModel"
    }
}
