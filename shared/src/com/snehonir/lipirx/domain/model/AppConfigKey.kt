package com.snehonir.lipirx.domain.model

import com.snehonir.lipirx.domain.agent.RxAgentType

enum class AppConfigKey(val key: String) {
    PROVIDER_TYPE("provider_type"),
    CUSTOM_URL("custom_url"),
    LAST_SELECTED_MODEL("last_selected_model"),
    GEMINI_API_KEY("gemini_api_key"),
    GROQ_API_KEY("groq_api_key"),
    OPENAI_API_KEY("openai_api_key"),
    CUSTOM_API_KEY("custom_api_key"),
    ON_DEVICE_STRATEGY("on_device_strategy"),
    MCP_SERVER_ENABLED("mcp_server_enabled");

    companion object {
        fun keyForProvider(provider: AiProvider): AppConfigKey = when (provider) {
            AiProvider.Groq -> GROQ_API_KEY
            AiProvider.OpenAi -> OPENAI_API_KEY
            AiProvider.Custom -> CUSTOM_API_KEY
            AiProvider.Gemini -> GEMINI_API_KEY
            AiProvider.OnDevice -> GEMINI_API_KEY
        }

        fun keyForAgentProvider(agentType: RxAgentType): String = "agent_provider_${agentType.id}"
        fun keyForAgentModel(agentType: RxAgentType): String = "agent_model_${agentType.id}"
        fun keyForProviderModel(provider: AiProvider): String = "last_selected_model_${provider.id}"
    }
}
