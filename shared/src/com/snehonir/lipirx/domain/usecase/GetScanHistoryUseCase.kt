package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.local.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.repository.PrescriptionRepository

class GetScanHistoryUseCase(
    private val repository: PrescriptionRepository
) {
    fun getHistory(flow: String = ""): List<PrescriptionHistoryEntity> {
        return repository.getHistory(flow)
    }

    fun saveSummary(summaryText: String, timestampMillis: Long = 0L, flow: String = "") {
        repository.saveSummary(summaryText, timestampMillis, flow)
    }

    fun saveDispensedHistory(medicines: List<MedicineItem>, flow: String = "") {
        repository.saveDispensedHistory(medicines, flow)
    }

    fun deleteHistoryRecord(id: Long, flow: String = "") {
        repository.deleteHistoryRecord(id, flow)
    }

    fun clearAllHistory(flow: String = "") {
        repository.clearAllHistory(flow)
    }
}
