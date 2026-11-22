package com.snehonir.lipirx.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "ProviderConfig")
data class ProviderConfigEntity(
    @PrimaryKey val providerType: String = "gemini",
    val customUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val themeMode: String = "system"
)
