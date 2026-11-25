package com.snehonir.lipirx.presentation.results

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
