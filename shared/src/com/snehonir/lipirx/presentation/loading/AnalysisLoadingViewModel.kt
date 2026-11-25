package com.snehonir.lipirx.presentation.loading

import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import io.github.aakira.napier.Napier

class AnalysisLoadingViewModel(
    private val sessionState: PrescriptionSessionState
) : LipiRxViewModel<AnalysisLoadingUiState>(AnalysisLoadingUiState()) {

    init {
        launchSafe {
            sessionState.session.collect { data ->
                updateState {
                    copy(
                        imageByteArray = data.imageByteArray,
                        isLoading = data.isLoading,
                        progress = data.progress
                    )
                }
            }
        }
    }

    fun stopAnalysis(flow: String = "App -> AnalysisLoadingViewModel.stopAnalysis") {
        Napier.d("[$flow] Cancelling ongoing analysis", tag = TAG)
        sessionState.cancelActiveAnalysis()
    }

    companion object {
        private val TAG = AnalysisLoadingViewModel::class.simpleName
    }
}
