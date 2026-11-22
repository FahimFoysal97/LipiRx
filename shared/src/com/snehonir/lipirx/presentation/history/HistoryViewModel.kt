package com.snehonir.lipirx.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class HistoryUiState(
    val historyList: ImmutableList<PrescriptionHistoryEntity> = persistentListOf(),
    val isLoading: Boolean = false
)

class HistoryViewModel(
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val sessionState: PrescriptionSessionState,
    private val jsonParser: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistoryList()
    }

    fun loadHistoryList(flow: String = "App -> HistoryViewModel.loadHistoryList") {
        Napier.d("[$flow] Querying scan history list from SQLite database", tag = TAG)
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true) }
            val records = runCatching {
                getScanHistoryUseCase.getHistory(flow = flow)
            }.getOrDefault(emptyList()).toImmutableList()

            _uiState.update {
                it.copy(historyList = records, isLoading = false)
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
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { getScanHistoryUseCase.deleteHistoryRecord(id, flow = flow) }
            loadHistoryList(flow = "$flow -> refreshHistory")
        }
    }

    fun clearAllHistory(flow: String = "App -> HistoryViewModel.clearAllHistory") {
        Napier.d("[$flow] Clearing all history records from SQLite database", tag = TAG)
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { getScanHistoryUseCase.clearAllHistory(flow = flow) }
            loadHistoryList(flow = "$flow -> refreshHistory")
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
        private const val TAG = "HistoryViewModel"
    }
}
