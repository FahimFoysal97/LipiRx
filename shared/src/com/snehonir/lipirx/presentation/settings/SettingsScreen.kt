package com.snehonir.lipirx.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onClose: () -> Unit,
    onViewTelemetry: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsView(
        selectedModel = uiState.selectedModel,
        apiKey = uiState.apiKey,
        geminiApiKey = uiState.geminiApiKey,
        groqApiKey = uiState.groqApiKey,
        openaiApiKey = uiState.openaiApiKey,
        claudeApiKey = uiState.claudeApiKey,
        customApiKey = uiState.customApiKey,
        providerType = uiState.providerType,
        customUrl = uiState.customUrl,
        apiKeyError = uiState.apiKeyError,
        apiKeySuccess = uiState.apiKeySuccess,
        isTestingApiKey = uiState.isTestingApiKey,
        onDeviceStrategy = uiState.onDeviceStrategy,
        isMcpServerEnabled = uiState.isMcpServerEnabled,
        onToggleMcpServer = { enabled -> viewModel.onMcpServerToggled(enabled) },
        agentConfigs = uiState.agentConfigs,
        providerModelsMap = uiState.providerModelsMap,
        onAgentProviderSelected = { agent, provider -> viewModel.onAgentProviderSelected(agent, provider) },
        onAgentModelSelected = { agent, model -> viewModel.onAgentModelSelected(agent, model) },
        onAgentFallbackProviderSelected = { agent, provider -> viewModel.onAgentFallbackProviderSelected(agent, provider) },
        onAgentFallbackModelSelected = { agent, model -> viewModel.onAgentFallbackModelSelected(agent, model) },
        onProviderTabSelected = { providerId -> viewModel.onProviderTabSelected(providerId) },
        onSaveProviderConfig = { providerType, customUrl, apiKey, modelName ->
            viewModel.saveAndTestProviderConfig(providerType, customUrl, apiKey, modelName)
        },
        onSelectOnDeviceStrategy = { strategy -> viewModel.onSelectOnDeviceStrategy(strategy) },
        slmModelDirectory = uiState.slmModelDirectory,
        onSaveSlmDirectoryPath = { path -> viewModel.onSaveSlmDirectoryPath(path) },
        themeMode = uiState.themeMode,
        onSelectThemeMode = { mode -> viewModel.updateThemeMode(mode) },
        onViewTelemetry = onViewTelemetry,
        onClose = onClose
    )
}
