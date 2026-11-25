package com.snehonir.lipirx.presentation.history

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.DeleteHistoryParams
import com.snehonir.lipirx.domain.usecase.DeleteScanHistoryUseCase
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.json.Json

data class HistoryUiState(
    val historyList: ImmutableList<PrescriptionHistoryEntity> = persistentListOf(),
    val isLoading: Boolean = false
)

class HistoryViewModel(
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val deleteScanHistoryUseCase: DeleteScanHistoryUseCase,
    private val sessionState: PrescriptionSessionState,
    private val jsonParser: Json
) : LipiRxViewModel<HistoryUiState>(HistoryUiState()) {

    private var observeJob: Job? = null

    init {
        loadHistoryList()
    }

    fun loadHistoryList(flow: String = "App -> HistoryViewModel.loadHistoryList") {
        Napier.d("[$flow] Observing scan history list from SQLite database", tag = TAG)
        observeJob?.cancel()
        observeJob = launchSafe(Dispatchers.Default) {
            updateState { copy(isLoading = true) }
            getScanHistoryUseCase.observe(flow = flow)
                .catch { ex ->
                    Napier.e("[$flow] Failed to observe scan history: ${ex.message}", ex)
                    updateState { copy(isLoading = false) }
                }
                .collect { records ->
                    updateState {
                        copy(historyList = records.toImmutableList(), isLoading = false)
                    }
                }
        }
    }

    fun openRecord(record: PrescriptionHistoryEntity, flow: String = "App -> HistoryViewModel.openRecord") {
        Napier.d("[$flow] Opening history record ID=${record.id}, scannedAt=${record.formattedDateTime}", tag = TAG)
        val medicines = parseSummaryToMedicineItems(record.medicineSummary)
        sessionState.openHistoryRecord(record, medicines)
    }

    fun deleteRecord(id: Long, flow: String = "App -> HistoryViewModel.deleteRecord") {
        Napier.d("[$flow] Deleting history record ID=$id", tag = TAG)
        launchSafe(Dispatchers.Default) {
            deleteScanHistoryUseCase(DeleteHistoryParams(recordId = id, flow = flow))
        }
    }

    fun clearAllHistory(flow: String = "App -> HistoryViewModel.clearAllHistory") {
        Napier.d("[$flow] Clearing all history records from SQLite database", tag = TAG)
        launchSafe(Dispatchers.Default) {
            deleteScanHistoryUseCase(DeleteHistoryParams(clearAll = true, flow = flow))
        }
    }

    private fun parseSummaryToMedicineItems(summaryText: String): List<MedicineItem> {
        if (summaryText.isBlank()) return emptyList()

        val decodedItems = runCatching {
            jsonParser.decodeFromString<List<MedicineItem>>(summaryText)
        }.getOrNull()

        if (!decodedItems.isNullOrEmpty()) {
            return decodedItems
        }

        return summaryText.split("; ").map { part ->
            val clean = part.trim()
            val namePart = clean.substringBefore(" (").trim().ifBlank { clean.substringBefore(" [").trim() }
            val genericPart = if (clean.contains("(") && clean.contains(")")) clean.substringAfter("(").substringBefore(")").trim() else ""
            val schedulePart = if (clean.contains("[") && clean.contains("]")) clean.substringAfter("[").substringBefore("]").trim() else ""
            val qtyPart = if (clean.contains("Qty: ")) clean.substringAfter("Qty: ").trim() else "1 pcs"
            val units = qtyPart.substringBefore(" ").toIntOrNull() ?: 1
            val unitType = qtyPart.substringAfter(" ", "pcs").trim()

            MedicineItem(
                medicineName = namePart,
                genericName = genericPart,
                schedule = schedulePart,
                units = units,
                unitType = unitType
            )
        }
    }

    companion object {
        private val TAG = HistoryViewModel::class.simpleName
    }
}
