package com.snehonir.lipirx.presentation.results

import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.FormatMedicineSummaryUseCase
import com.snehonir.lipirx.domain.usecase.SaveScanSummaryParams
import com.snehonir.lipirx.domain.usecase.SaveScanSummaryUseCase
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json

class ScanResultsViewModel(
    private val sessionState: PrescriptionSessionState,
    private val saveScanSummaryUseCase: SaveScanSummaryUseCase,
    private val formatMedicineSummaryUseCase: FormatMedicineSummaryUseCase = FormatMedicineSummaryUseCase(),
    private val jsonParser: Json
) : LipiRxViewModel<ScanResultsUiState>(ScanResultsUiState()) {

    init {
        launchSafe {
            sessionState.session.collect { data ->
                updateState {
                    copy(
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
        flow: String = "App -> $TAG.onUpdateMedicineItem"
    ) {
        Napier.d("[$flow] Updating medicine item at index $index: '${updatedItem.medicineName}'", tag = TAG)
        val currentMedicines = currentState.medicines.toMutableList()
        if (index < 0 || index >= currentMedicines.size) return
        currentMedicines[index] = updatedItem
        sessionState.updateMedicineList(currentMedicines)

        launchSafe(Dispatchers.Default) {
            val updatedJsonSummary = runCatching { jsonParser.encodeToString(currentMedicines) }.getOrDefault("")
            if (updatedJsonSummary.isNotBlank()) {
                val historyRecord = currentState.selectedHistoryItem
                val timestamp = historyRecord?.scannedAt ?: getPlatformEpochMillis()
                saveScanSummaryUseCase(
                    SaveScanSummaryParams(
                        summaryText = updatedJsonSummary,
                        timestampMillis = timestamp,
                        flow = "$flow -> saveSummary"
                    )
                )
            }
        }
    }

    fun copyToClipboard(flow: String = "App -> $TAG.copyToClipboard"): String {
        val medicines = currentState.medicines
        val summary = formatMedicineSummaryUseCase(medicines)
        if (summary.isBlank()) return ""
        Napier.d("[$flow] Formatted summary for POS clipboard:\n$summary", tag = TAG)
        return summary
    }

    fun approveAndResumeAnalysis(flow: String = "App -> $TAG.approveAndResumeAnalysis") {
        Napier.d("[$flow] Pharmacist signed off prescription summary", tag = TAG)
        val currentMedicines = currentState.medicines
        sessionState.setResults(
            medicines = currentMedicines,
            isSuspended = false,
            approvalReason = null
        )
        launchSafe(Dispatchers.Default) {
            saveScanSummaryUseCase(
                SaveScanSummaryParams(
                    summaryText = "",
                    medicines = currentMedicines,
                    flow = "$flow -> saveDispensedHistory"
                )
            )
        }
    }

    fun clearResults() {
        sessionState.resetSession()
    }

    fun clearSelectedHistoryRecord() {
        sessionState.clearHistoryRecord()
    }

    companion object {
        private val TAG = ScanResultsViewModel::class.simpleName
    }
}
