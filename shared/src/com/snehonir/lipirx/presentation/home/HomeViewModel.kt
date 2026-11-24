package com.snehonir.lipirx.presentation.home

import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetHistoryParams
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers

data class HomeUiState(
    val historyCount: Int = 0
)

class HomeViewModel(
    private val sessionState: PrescriptionSessionState,
    private val getScanHistoryUseCase: GetScanHistoryUseCase
) : LipiRxViewModel<HomeUiState>(HomeUiState()) {

    init {
        loadHistoryCount()
    }

    fun loadHistoryCount(flow: String = "App -> HomeViewModel.loadHistoryCount") {
        launchSafe(Dispatchers.Default) {
            val count = getScanHistoryUseCase(GetHistoryParams(flow = flow))
                .getOrDefault(emptyList()).size
            updateState { copy(historyCount = count) }
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
