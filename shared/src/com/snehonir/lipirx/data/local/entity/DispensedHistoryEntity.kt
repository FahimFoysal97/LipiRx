package com.snehonir.lipirx.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "DispensedHistory")
data class DispensedHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dispensedAt: Long,
    val medicineName: String,
    val genericName: String,
    val dispensedBrand: String
)
