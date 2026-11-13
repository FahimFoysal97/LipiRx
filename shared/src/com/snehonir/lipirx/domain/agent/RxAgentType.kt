package com.snehonir.lipirx.domain.agent

import androidx.compose.ui.graphics.vector.ImageVector
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.ui.theme.GenericSubstituteAgentVectorIcon
import com.snehonir.lipirx.ui.theme.MedicalParserAgentVectorIcon
import com.snehonir.lipirx.ui.theme.SafetyAuditorAgentVectorIcon
import com.snehonir.lipirx.ui.theme.SupervisorAgentVectorIcon
import com.snehonir.lipirx.ui.theme.VisionOcrAgentVectorIcon

enum class RxAgentType(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val defaultProvider: AiProvider,
    val defaultModel: String
) {
    SUPERVISOR(
        id = "supervisor",
        displayName = "Manager / Supervisor Agent",
        description = "Evaluates workspace state and dynamically orchestrates worker agents",
        icon = SupervisorAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = "gemini-2.0-flash"
    ),
    TRANSCRIPTION(
        id = "transcription",
        displayName = "Vision Handwriting OCR",
        description = "Reads handwritten doctor text from prescription photo",
        icon = VisionOcrAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = "gemini-2.0-flash"
    ),
    ENTITY_PARSER(
        id = "entity_parser",
        displayName = "Medical Entity Parser",
        description = "Parses brand names, dosage forms, and schedules",
        icon = MedicalParserAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = "gemini-1.5-pro"
    ),
    GENERIC_SUBSTITUTE(
        id = "generic_substitute",
        displayName = "Generic & Substitute Finder",
        description = "Matches generic compositions and BD brand substitutes",
        icon = GenericSubstituteAgentVectorIcon,
        defaultProvider = AiProvider.Groq,
        defaultModel = "llama-3.2-90b-vision-instruct"
    ),
    SAFETY_AUDITOR(
        id = "safety_auditor",
        displayName = "Patient Safety Auditor",
        description = "Audits dosage safety and flags unreadable items",
        icon = SafetyAuditorAgentVectorIcon,
        defaultProvider = AiProvider.OpenAi,
        defaultModel = "gpt-4o-mini"
    );

    companion object {
        val ALL: List<RxAgentType> = entries.toList()

        fun fromId(id: String): RxAgentType = entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: SUPERVISOR
    }
}

data class AgentConfig(
    val agentType: RxAgentType,
    val provider: AiProvider,
    val selectedModel: String = ""
)
