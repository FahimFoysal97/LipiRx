package com.snehonir.lipirx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiModelsListResponse(
    val data: List<OpenAiModelDto>? = null
)

@Serializable
internal data class OpenAiModelDto(
    val id: String,
    @SerialName("input_modalities") val inputModalities: List<String>? = null
)
