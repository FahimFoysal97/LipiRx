package com.snehonir.lipirx.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.snehonir.lipirx.data.local.formatEpochMillisToDateTime

@Entity(tableName = "PrescriptionHistory")
data class PrescriptionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannedAt: Long,
    val medicineSummary: String
) {
    val formattedDateTime: String
        get() = formatEpochMillisToDateTime(scannedAt)
}
