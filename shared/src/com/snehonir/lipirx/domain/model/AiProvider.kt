package com.snehonir.lipirx.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.snehonir.lipirx.ui.theme.Lipi
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
        override val defaultEndpoint: String = "https://generativelanguage.googleapis.com/v1beta/models"
        override val defaultModel: String = "gemini-2.0-flash"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = SparkleVectorIcon
        override val brandColor: Color = Color(0xFF8E24AA)
    }

    data object Groq : AiProvider {
        override val id: String = "groq"
        override val displayName: String = "Groq"
        override val defaultEndpoint: String = "https://api.groq.com/openai/v1/models"
        override val defaultModel: String = "llama-3.2-90b-vision-instruct"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = LightningVectorIcon
        override val brandColor: Color = Color(0xFFF57C00)
    }

    data object OpenAi : AiProvider {
        override val id: String = "openai"
        override val displayName: String = "OpenAI"
        override val defaultEndpoint: String = "https://api.openai.com/v1/models"
        override val defaultModel: String = "gpt-4o"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
        override val icon: ImageVector = OpenAiVectorIcon
        override val brandColor: Color = Color(0xFF00A67E)
    }

    data object Custom : AiProvider {
        override val id: String = "custom"
        override val displayName: String = "Custom URL"
        override val defaultEndpoint: String = ""
        override val defaultModel: String = "custom-vision-model"
        override val requiresCustomUrl: Boolean = true
        override val requiresApiKey: Boolean = false
        override val icon: ImageVector = CustomUrlVectorIcon
        override val brandColor: Color = Color(0xFF546E7A)
    }

    data object OnDevice : AiProvider {
        override val id: String = "on_device"
        override val displayName: String = "On-Device SLM"
        override val defaultEndpoint: String = "local://on-device"
        override val defaultModel: String = "gemini-nano-local"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = false
        override val icon: ImageVector = OnDeviceVectorIcon
        override val brandColor: Color = Lipi.Primary
    }

    companion object {
        val ALL: List<AiProvider> = listOf(Gemini, Groq, OpenAi, Custom, OnDevice)

        fun fromId(id: String): AiProvider = when (id.trim().lowercase()) {
            Groq.id -> Groq
            OpenAi.id -> OpenAi
            Custom.id -> Custom
            OnDevice.id -> OnDevice
            else -> Gemini
        }
    }
}
