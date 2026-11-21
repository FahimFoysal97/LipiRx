package com.snehonir.lipirx.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class ScanResultsUiState(
    val imageByteArray: ByteArray? = null,
    val medicines: ImmutableList<MedicineItem> = persistentListOf(),
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String? = null,
    val selectedHistoryItem: PrescriptionHistoryEntity? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScanResultsUiState) return false

        if (isSuspendedForApproval != other.isSuspendedForApproval) return false
        if (approvalReason != other.approvalReason) return false
        if (medicines != other.medicines) return false
        if (selectedHistoryItem != other.selectedHistoryItem) return false
        if (imageByteArray != null) {
            if (other.imageByteArray == null) return false
            if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        } else if (other.imageByteArray != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isSuspendedForApproval.hashCode()
        result = 31 * result + approvalReason.hashCode()
        result = 31 * result + medicines.hashCode()
        result = 31 * result + selectedHistoryItem.hashCode()
        result = 31 * result + (imageByteArray?.contentHashCode() ?: 0)
        return result
    }
}

class ScanResultsViewModel(
    private val sessionState: PrescriptionSessionState,
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val jsonParser: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanResultsUiState())
    val uiState: StateFlow<ScanResultsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.session.collect { data ->
                _uiState.update {
                    it.copy(
                        imageByteArray = data.imageByteArray,
                        medicines = data.medicines,
                        isSuspendedForApproval = data.isSuspendedForApproval,
                        approvalReason = data.approvalReason,
                        selectedHistoryItem = data.selectedHistoryItem
                    )
                }
            }
        }
    }

    fun onUpdateMedicineItem(
        index: Int,
        updatedItem: MedicineItem,
        flow: String = "App -> ScanResultsViewModel.onUpdateMedicineItem"
    ) {
        Napier.d("[$flow] Updating medicine item at index $index: '${updatedItem.medicineName}'", tag = TAG)
        val currentMedicines = _uiState.value.medicines.toMutableList()
        if (index < 0 || index >= currentMedicines.size) return
        currentMedicines[index] = updatedItem
        sessionState.updateMedicineList(currentMedicines)

        viewModelScope.launch(Dispatchers.Default) {
            val updatedJsonSummary = runCatching { jsonParser.encodeToString(currentMedicines) }.getOrDefault("")
            if (updatedJsonSummary.isNotBlank()) {
                val historyRecord = _uiState.value.selectedHistoryItem
                val timestamp = historyRecord?.scannedAt ?: getPlatformEpochMillis()
                runCatching {
                    getScanHistoryUseCase.saveSummary(updatedJsonSummary, timestamp, flow = "$flow -> saveSummary")
                }.onFailure { ex ->
                    Napier.w("[$flow -> Error] Failed to update history summary: ${ex.message}", throwable = ex, tag = TAG)
                }
            }
        }
    }

    fun copyToClipboard(flow: String = "App -> ScanResultsViewModel.copyToClipboard"): String {
        val medicines = _uiState.value.medicines
        if (medicines.isEmpty()) return ""
        val summary = medicines.joinToString(separator = "\n") { item ->
            "• ${item.formattedFullName} (${item.genericName}) - Schedule: ${item.schedule} | Qty: ${item.formattedQuantity}"
        }
        Napier.d("[$flow] Formatted summary for POS clipboard:\n$summary", tag = TAG)
        return summary
    }

    fun approveAndResumeAnalysis(flow: String = "App -> ScanResultsViewModel.approveAndResumeAnalysis") {
        Napier.d("[$flow] Pharmacist signed off prescription summary", tag = TAG)
        val currentMedicines = _uiState.value.medicines
        sessionState.setResults(
            medicines = currentMedicines,
            isSuspended = false,
            approvalReason = null
        )
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                getScanHistoryUseCase.saveDispensedHistory(currentMedicines, flow = "$flow -> saveDispensedHistory")
            }.onFailure { ex ->
                Napier.w("[$flow -> Error] Failed to save dispensed history: ${ex.message}", throwable = ex, tag = TAG)
            }
        }
    }

    fun clearResults() {
        sessionState.resetSession()
    }

    fun clearSelectedHistoryRecord() {
        sessionState.clearHistoryRecord()
    }

    companion object {
        private const val TAG = "ScanResultsViewModel"
    }
}
