package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.repository.PrescriptionRepository

class GetScanHistoryUseCase(
    private val repository: PrescriptionRepository
) {
    suspend fun getHistory(flow: String = ""): List<PrescriptionHistoryEntity> {
        return repository.getHistory(flow)
    }

    suspend fun saveSummary(summaryText: String, timestampMillis: Long = 0L, flow: String = "") {
        repository.saveSummary(summaryText, timestampMillis, flow)
    }

    suspend fun saveDispensedHistory(medicines: List<MedicineItem>, flow: String = "") {
        repository.saveDispensedHistory(medicines, flow)
    }

    suspend fun deleteHistoryRecord(id: Long, flow: String = "") {
        repository.deleteHistoryRecord(id, flow)
    }

    suspend fun clearAllHistory(flow: String = "") {
        repository.clearAllHistory(flow)
    }
}