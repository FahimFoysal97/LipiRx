package com.snehonir.lipirx.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.ui.theme.icons.ClaudeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.CustomUrlVectorIcon
import com.snehonir.lipirx.ui.theme.icons.LightningVectorIcon
import com.snehonir.lipirx.ui.theme.icons.OnDeviceVectorIcon
import com.snehonir.lipirx.ui.theme.icons.OpenAiVectorIcon
import com.snehonir.lipirx.ui.theme.icons.SparkleVectorIcon

sealed interface AiProvider {
    val id: String
    val displayName: String
    val defaultEndpoint: String
    val defaultModel: String
    val requiresCustomUrl: Boolean
    val requiresApiKey: Boolean
    val icon: ImageVector
    val brandColor: Color

    data object Gemini : AiProvider {
        override val id: String = "gemini"
        override val displayName: String = "Gemini"
        override val defaultEndpoint: String = AppConstants.GEMINI_DEFAULT_ENDPOINT
        override val defaultModel: String = AppConstants.GEMINI_DEFAULT_MODEL
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = SparkleVectorIcon
        override val brandColor: Color = Color(0xFF4285F4)
    }

    data object OpenAi : AiProvider {
        override val id: String = "openai"
        override val displayName: String = "OpenAI"
        override val defaultEndpoint: String = AppConstants.OPENAI_DEFAULT_ENDPOINT
        override val defaultModel: String = AppConstants.OPENAI_DEFAULT_MODEL
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = OpenAiVectorIcon
        override val brandColor: Color = Color(0xFF10A37F)
    }

    data object Claude : AiProvider {
        override val id: String = "claude"
        override val displayName: String = "Claude"
        override val defaultEndpoint: String = AppConstants.CLAUDE_DEFAULT_ENDPOINT
        override val defaultModel: String = AppConstants.CLAUDE_DEFAULT_MODEL
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = ClaudeVectorIcon
        override val brandColor: Color = Color(0xFFD97757)
    }

    data object Groq : AiProvider {
        override val id: String = "groq"
        override val displayName: String = "Groq"
        override val defaultEndpoint: String = AppConstants.GROQ_DEFAULT_ENDPOINT
        override val defaultModel: String = AppConstants.GROQ_DEFAULT_MODEL
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = LightningVectorIcon
        override val brandColor: Color = Color(0xFFF55036)
    }

    data object Custom : AiProvider {
        override val id: String = "custom"
        override val displayName: String = "Custom URL"
        override val defaultEndpoint: String = ""
        override val defaultModel: String = AppConstants.CUSTOM_DEFAULT_MODEL
        override val requiresCustomUrl: Boolean = true
        override val requiresApiKey: Boolean = false
        override val icon: ImageVector = CustomUrlVectorIcon
        override val brandColor: Color = Color(0xFF546E7A)
    }

    data object OnDevice : AiProvider {
        override val id: String = "on_device"
        override val displayName: String = "On-Device SLM"
        override val defaultEndpoint: String = "local://on-device"
        override val defaultModel: String = AppConstants.ON_DEVICE_DEFAULT_MODEL
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = false
        override val icon: ImageVector = OnDeviceVectorIcon
        override val brandColor: Color = Color(0xFF0B7A6E)
    }

    companion object {
        val ALL: List<AiProvider> = listOf(Gemini, OpenAi, Claude, Groq, Custom, OnDevice)

        fun fromId(id: String): AiProvider = when (id.trim().lowercase()) {
            OpenAi.id -> OpenAi
            Claude.id -> Claude
            Groq.id -> Groq
            Custom.id -> Custom
            OnDevice.id -> OnDevice
            else -> Gemini
        }
    }
}
