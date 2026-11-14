package com.snehonir.lipirx.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfoDto(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)
