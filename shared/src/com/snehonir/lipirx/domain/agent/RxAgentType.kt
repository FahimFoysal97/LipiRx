package com.snehonir.lipirx.domain.agent

import androidx.compose.ui.graphics.vector.ImageVector
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.agent_entity_parser_desc
import com.snehonir.lipirx.resources.agent_entity_parser_name
import com.snehonir.lipirx.resources.agent_generic_substitute_desc
import com.snehonir.lipirx.resources.agent_generic_substitute_name
import com.snehonir.lipirx.resources.agent_safety_auditor_desc
import com.snehonir.lipirx.resources.agent_safety_auditor_name
import com.snehonir.lipirx.resources.agent_supervisor_desc
import com.snehonir.lipirx.resources.agent_supervisor_name
import com.snehonir.lipirx.resources.agent_transcription_desc
import com.snehonir.lipirx.resources.agent_transcription_name
import com.snehonir.lipirx.ui.theme.icons.GenericSubstituteAgentVectorIcon
import com.snehonir.lipirx.ui.theme.icons.MedicalParserAgentVectorIcon
import com.snehonir.lipirx.ui.theme.icons.SafetyAuditorAgentVectorIcon
import com.snehonir.lipirx.ui.theme.icons.SupervisorAgentVectorIcon
import com.snehonir.lipirx.ui.theme.icons.VisionOcrAgentVectorIcon
import org.jetbrains.compose.resources.StringResource

enum class RxAgentType(
    val id: String,
    val displayNameRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
    val defaultProvider: AiProvider,
    val defaultModel: String
) {
    SUPERVISOR(
        id = "supervisor",
        displayNameRes = Res.string.agent_supervisor_name,
        descriptionRes = Res.string.agent_supervisor_desc,
        icon = SupervisorAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = AppConstants.GEMINI_DEFAULT_MODEL
    ),
    TRANSCRIPTION(
        id = "transcription",
        displayNameRes = Res.string.agent_transcription_name,
        descriptionRes = Res.string.agent_transcription_desc,
        icon = VisionOcrAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = AppConstants.GEMINI_DEFAULT_MODEL
    ),
    ENTITY_PARSER(
        id = "entity_parser",
        displayNameRes = Res.string.agent_entity_parser_name,
        descriptionRes = Res.string.agent_entity_parser_desc,
        icon = MedicalParserAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = AppConstants.GEMINI_DEFAULT_MODEL
    ),
    GENERIC_SUBSTITUTE(
        id = "generic_substitute",
        displayNameRes = Res.string.agent_generic_substitute_name,
        descriptionRes = Res.string.agent_generic_substitute_desc,
        icon = GenericSubstituteAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = AppConstants.GEMINI_DEFAULT_MODEL
    ),
    SAFETY_AUDITOR(
        id = "safety_auditor",
        displayNameRes = Res.string.agent_safety_auditor_name,
        descriptionRes = Res.string.agent_safety_auditor_desc,
        icon = SafetyAuditorAgentVectorIcon,
        defaultProvider = AiProvider.Gemini,
        defaultModel = AppConstants.GEMINI_DEFAULT_MODEL
    );

    companion object {
        val ALL: List<RxAgentType> = entries.toList()

        fun fromId(id: String): RxAgentType = entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: SUPERVISOR
    }
}

data class AgentConfig(
    val agentType: RxAgentType,
    val provider: AiProvider,
    val selectedModel: String = "",
    val fallbackProvider: AiProvider? = null,
    val fallbackModel: String = ""
)
