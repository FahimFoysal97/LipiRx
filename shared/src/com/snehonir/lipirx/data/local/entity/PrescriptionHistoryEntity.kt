package com.snehonir.lipirx.data.local.entity

import com.snehonir.lipirx.data.local.formatEpochMillisToDateTime

data class PrescriptionHistoryEntity(
    val id: Long,
    val scannedAt: Long,
    val medicineSummary: String
) {
    val formattedDateTime: String
        get() = formatEpochMillisToDateTime(scannedAt)
}
