package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.common.LipiRxSuspendUseCase
import com.snehonir.lipirx.domain.common.LipiRxUseCase
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.repository.PrescriptionRepository

// ── Query Scan History ────────────────────────────────────────────────────────
data class GetHistoryParams(val flow: String = "")

class GetScanHistoryUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<GetHistoryParams, List<PrescriptionHistoryEntity>> {

    override suspend fun invoke(params: GetHistoryParams): Result<List<PrescriptionHistoryEntity>> {
        return repository.safeDbCall(params.flow) {
            repository.getHistory(params.flow)
        }
    }
}

// ── Save Scan Summary & Dispensed Records ────────────────────────────────────
data class SaveScanSummaryParams(
    val summaryText: String,
    val timestampMillis: Long = 0L,
    val medicines: List<MedicineItem> = emptyList(),
    val flow: String = ""
)

class SaveScanSummaryUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<SaveScanSummaryParams, Unit> {

    override suspend fun invoke(params: SaveScanSummaryParams): Result<Unit> {
        return repository.safeDbCall(params.flow) {
            if (params.summaryText.isNotBlank()) {
                repository.saveSummary(params.summaryText, params.timestampMillis, params.flow)
            }
            if (params.medicines.isNotEmpty()) {
                repository.saveDispensedHistory(params.medicines, params.flow)
            }
        }
    }
}

// ── Delete History Records ───────────────────────────────────────────────────
data class DeleteHistoryParams(
    val recordId: Long? = null,
    val clearAll: Boolean = false,
    val flow: String = ""
)

class DeleteScanHistoryUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<DeleteHistoryParams, Unit> {

    override suspend fun invoke(params: DeleteHistoryParams): Result<Unit> {
        return repository.safeDbCall(params.flow) {
            if (params.clearAll) {
                repository.clearAllHistory(params.flow)
            } else if (params.recordId != null) {
                repository.deleteHistoryRecord(params.recordId, params.flow)
            }
        }
    }
}

// ── Format Summary for Clipboard ─────────────────────────────────────────────
class FormatMedicineSummaryUseCase : LipiRxUseCase<List<MedicineItem>, String> {
    override fun invoke(params: List<MedicineItem>): String {
        if (params.isEmpty()) return ""
        return params.joinToString(separator = "\n") { item ->
            val genericPart = if (item.genericName.isNotBlank()) " (${item.genericName})" else ""
            val schedulePart = if (item.schedule.isNotBlank()) " - ${item.schedule}" else ""
            "• ${item.formattedFullName}$genericPart$schedulePart | Qty: ${item.formattedQuantity}"
        }
    }
}
