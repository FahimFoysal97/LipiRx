package com.snehonir.lipirx.presentation.settings

import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.model.ThemeMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

data class SettingsUiState(
    val selectedModel: String = "",
    val provider: AiProvider = AiProvider.Gemini,
    val customUrl: String = "",
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val openaiApiKey: String = "",
    val claudeApiKey: String = "",
    val customApiKey: String = "",
    val apiKeyError: String? = null,
    val apiKeySuccess: String? = null,
    val isTestingApiKey: Boolean = false,
    val isMcpServerEnabled: Boolean = false,
    val onDeviceStrategy: OnDeviceStrategy = OnDeviceStrategy.HYBRID_FALLBACK,
    val slmModelDirectory: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val agentConfigs: ImmutableMap<RxAgentType, AgentConfig> = persistentMapOf(),
    val providerModelsMap: ImmutableMap<String, ImmutableList<String>> = persistentMapOf()
) {
    val providerType: String get() = provider.id

    val apiKey: String
        get() = when (provider) {
            AiProvider.Groq -> groqApiKey
            AiProvider.OpenAi -> openaiApiKey
            AiProvider.Claude -> claudeApiKey
            AiProvider.Custom -> customApiKey
            AiProvider.Gemini -> geminiApiKey
            AiProvider.OnDevice -> geminiApiKey
        }
}
