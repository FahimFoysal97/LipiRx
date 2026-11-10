package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.domain.model.AiProvider

enum class RxAgentType(
    val id: String,
    val displayName: String,
    val description: String,
    val defaultProvider: AiProvider,
    val defaultModel: String
) {
    TRANSCRIPTION(
        id = "transcription",
        displayName = "1. Vision Handwriting OCR",
        description = "Reads handwritten doctor text from prescription photo",
        defaultProvider = AiProvider.Gemini,
        defaultModel = "gemini-2.0-flash"
    ),
    ENTITY_PARSER(
        id = "entity_parser",
        displayName = "2. Medical Entity Parser",
        description = "Parses brand names, dosage forms, and schedules",
        defaultProvider = AiProvider.Gemini,
        defaultModel = "gemini-1.5-pro"
    ),
    GENERIC_SUBSTITUTE(
        id = "generic_substitute",
        displayName = "3. Generic & Substitute Finder",
        description = "Matches generic compositions and BD brand substitutes",
        defaultProvider = AiProvider.Groq,
        defaultModel = "llama-3.2-90b-vision-instruct"
    ),
    SAFETY_AUDITOR(
        id = "safety_auditor",
        displayName = "4. Patient Safety Auditor",
        description = "Audits dosage safety and flags unreadable items",
        defaultProvider = AiProvider.OpenAi,
        defaultModel = "gpt-4o-mini"
    );

    companion object {
        val ALL: List<RxAgentType> = entries.toList()

        fun fromId(id: String): RxAgentType = entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: TRANSCRIPTION
    }
}

data class AgentConfig(
    val agentType: RxAgentType,
    val provider: AiProvider,
    val selectedModel: String = ""
)
