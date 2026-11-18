package com.snehonir.lipirx.domain.agent.koog.dto

import kotlinx.serialization.Serializable

@Serializable
data class ToolExecutionParams(
    val provider: String = "gemini",
    val model: String = "",
    val apiKey: String = "",
    val customUrl: String = "",
    val feedbackPrompt: String = "",
    val memoryContext: String = "",
    val strategy: String = "hybrid_fallback",
    val fallbackProvider: String = "",
    val fallbackModel: String = ""
)
