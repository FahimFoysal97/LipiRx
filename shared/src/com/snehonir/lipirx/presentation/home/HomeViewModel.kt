package com.snehonir.lipirx.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val historyCount: Int = 0
)

class HomeViewModel(
    private val sessionState: PrescriptionSessionState,
    private val getScanHistoryUseCase: GetScanHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHistoryCount()
    }

    fun loadHistoryCount(flow: String = "App -> HomeViewModel.loadHistoryCount") {
        viewModelScope.launch(Dispatchers.Default) {
            val count = runCatching { getScanHistoryUseCase.getHistory(flow = flow).size }.getOrDefault(0)
            _uiState.update { it.copy(historyCount = count) }
        }
    }

    fun onImageSelected(imageByteArray: ByteArray, flow: String = "App -> HomeViewModel.onImageSelected") {
        Napier.d("[$flow] Image selected (${imageByteArray.size} bytes)", tag = TAG)
        sessionState.setImageBytes(imageByteArray)
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
