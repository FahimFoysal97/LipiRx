package com.snehonir.lipirx.presentation.home

import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch

data class HomeUiState(
    val historyCount: Int = 0
)

class HomeViewModel(
    private val sessionState: PrescriptionSessionState,
    private val getScanHistoryUseCase: GetScanHistoryUseCase
) : LipiRxViewModel<HomeUiState>(HomeUiState()) {

    private var observeJob: Job? = null

    init {
        loadHistoryCount()
    }

    fun loadHistoryCount(flow: String = "App -> HomeViewModel.loadHistoryCount") {
        observeJob?.cancel()
        observeJob = launchSafe(Dispatchers.Default) {
            getScanHistoryUseCase.observe(flow = flow)
                .catch { ex ->
                    Napier.e("[$flow] Failed to observe scan history count: ${ex.message}", ex)
                }
                .collect { records ->
                    updateState { copy(historyCount = records.size) }
                }
        }
    }

    fun onImageSelected(imageByteArray: ByteArray, flow: String = "App -> HomeViewModel.onImageSelected") {
        Napier.d("[$flow] Image selected (${imageByteArray.size} bytes)", tag = TAG)
        sessionState.setImageBytes(imageByteArray)
    }

    companion object {
        private val TAG = HomeViewModel::class.simpleName
    }
}
