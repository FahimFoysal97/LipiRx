package com.snehonir.lipirx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.0
)

@Serializable
internal data class OpenAiMessage(
    val role: String,
    val content: List<OpenAiContentPart>
)

@Serializable
internal data class OpenAiContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: OpenAiImageUrl? = null
)

@Serializable
internal data class OpenAiImageUrl(
    val url: String
)

@Serializable
internal data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiError? = null
)

@Serializable
internal data class OpenAiChoice(
    val message: OpenAiResponseMessage? = null
)

@Serializable
internal data class OpenAiResponseMessage(
    val content: String? = null
)

@Serializable
internal data class OpenAiError(
    val message: String? = null
)
