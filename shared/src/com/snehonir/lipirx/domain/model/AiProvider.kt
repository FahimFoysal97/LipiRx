package com.snehonir.lipirx.domain.model

sealed interface AiProvider {
    val id: String
    val displayName: String
    val defaultEndpoint: String
    val defaultModel: String
    val requiresCustomUrl: Boolean
    val requiresApiKey: Boolean

    data object Gemini : AiProvider {
        override val id: String = "gemini"
        override val displayName: String = "🤖 Gemini"
        override val defaultEndpoint: String = "https://generativelanguage.googleapis.com/v1beta/models"
        override val defaultModel: String = "gemini-2.0-flash"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
    }

    data object Groq : AiProvider {
        override val id: String = "groq"
        override val displayName: String = "⚡ Groq"
        override val defaultEndpoint: String = "https://api.groq.com/openai/v1/models"
        override val defaultModel: String = "llama-3.2-90b-vision-instruct"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
    }

    data object OpenAi : AiProvider {
        override val id: String = "openai"
        override val displayName: String = "🌐 OpenAI"
        override val defaultEndpoint: String = "https://api.openai.com/v1/models"
        override val defaultModel: String = "gpt-4o"
        override val requiresCustomUrl: Boolean = false
        override val requiresApiKey: Boolean = true
    }

    data object Custom : AiProvider {
        override val id: String = "custom"
        override val displayName: String = "🛠️ Custom URL"
        override val defaultEndpoint: String = ""
        override val defaultModel: String = "custom-vision-model"
        override val requiresCustomUrl: Boolean = true
        override val requiresApiKey: Boolean = false
    }

    companion object {
        val ALL: List<AiProvider> = listOf(Gemini, Groq, OpenAi, Custom)

        fun fromId(id: String): AiProvider = when (id.trim().lowercase()) {
            Groq.id -> Groq
            OpenAi.id -> OpenAi
            Custom.id -> Custom
            else -> Gemini
        }
    }
}
