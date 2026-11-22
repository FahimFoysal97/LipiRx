package com.snehonir.lipirx.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "AppConfig")
data class AppConfigEntity(
    @PrimaryKey val configKey: String,
    val configValue: String
)
