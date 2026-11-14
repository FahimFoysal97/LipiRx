package com.snehonir.lipirx.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class GeminiModelsResponse(
    val models: List<ModelInfoDto>? = null
)

@Serializable
internal data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: SystemInstruction? = null,
    val generationConfig: GenerationConfig? = null
)

@Serializable
internal data class SystemInstruction(
    val parts: List<Part>
)

@Serializable
internal data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
internal data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
internal data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
internal data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val error: GeminiError? = null
)

@Serializable
internal data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
internal data class Candidate(
    val content: ResponseContent? = null,
    val finishReason: String? = null
)

@Serializable
internal data class ResponseContent(
    val parts: List<ResponsePart>? = null
)

@Serializable
internal data class ResponsePart(
    val text: String? = null
)
