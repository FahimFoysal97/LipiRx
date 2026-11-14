package com.snehonir.lipirx.data.local.entity

data class ProviderConfigEntity(
    val providerType: String = "gemini",
    val customUrl: String = "",
    val apiKey: String = "",
    val modelName: String = ""
)
