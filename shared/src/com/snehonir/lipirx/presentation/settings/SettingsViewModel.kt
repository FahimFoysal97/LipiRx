package com.snehonir.lipirx.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.domain.usecase.ManageProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.ProcessPrescriptionUseCase
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

class SettingsViewModel(
    private val manageProviderConfigUseCase: ManageProviderConfigUseCase,
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettingsState()
    }

    fun loadSettingsState(flow: String = "App -> SettingsViewModel.loadSettingsState") {
        Napier.d("[$flow] Loading settings state from SQLite DB", tag = TAG)
        viewModelScope.launch(Dispatchers.Default) {
            val config = manageProviderConfigUseCase.getProviderConfig(flow = flow)
            val providerObj = AiProvider.fromId(config.providerType)

            val geminiKey = manageProviderConfigUseCase.getApiKeyForProvider(AiProvider.Gemini.id)
            val groqKey = manageProviderConfigUseCase.getApiKeyForProvider(AiProvider.Groq.id)
            val openaiKey = manageProviderConfigUseCase.getApiKeyForProvider(AiProvider.OpenAi.id)
            val claudeKey = manageProviderConfigUseCase.getApiKeyForProvider(AiProvider.Claude.id)
            val customKey = manageProviderConfigUseCase.getApiKeyForProvider(AiProvider.Custom.id)

            val savedStrategy = manageProviderConfigUseCase.getOnDeviceStrategy()
            val slmDir = runCatching { manageProviderConfigUseCase.getSlmModelDirectory() }.getOrDefault("")
            val mcpEnabled = runCatching { manageProviderConfigUseCase.isMcpServerEnabled() }.getOrDefault(false)
            val themeMode = ThemeMode.fromId(config.themeMode)

            val loadedAgentConfigs = runCatching {
                manageProviderConfigUseCase.getAllAgentConfigs()
            }.getOrDefault(emptyMap()).toImmutableMap()

            val loadedProviderModelsMap = loadProviderModelsMap()

            _uiState.update {
                it.copy(
                    provider = providerObj,
                    customUrl = config.customUrl,
                    onDeviceStrategy = savedStrategy,
                    slmModelDirectory = slmDir,
                    geminiApiKey = geminiKey,
                    groqApiKey = groqKey,
                    openaiApiKey = openaiKey,
                    claudeApiKey = claudeKey,
                    customApiKey = customKey,
                    selectedModel = config.modelName,
                    isMcpServerEnabled = mcpEnabled,
                    themeMode = themeMode,
                    agentConfigs = loadedAgentConfigs,
                    providerModelsMap = loadedProviderModelsMap
                )
            }

            AiProvider.ALL.forEach { prov ->
                val key = manageProviderConfigUseCase.getApiKeyForProvider(prov.id)
                val customUrl = if (prov == AiProvider.Custom) manageProviderConfigUseCase.getProviderConfig().customUrl else ""
                val canFetch = key.isNotBlank() || prov == AiProvider.OnDevice || (prov == AiProvider.Custom && customUrl.isNotBlank())

                if (canFetch) {
                    fetchModelsForProvider(prov, flow = "$flow.${prov.displayName}")
                }
            }
        }
    }

    private suspend fun loadProviderModelsMap(): ImmutableMap<String, ImmutableList<String>> {
        val rawMap = runCatching { manageProviderConfigUseCase.getAllProviderModelsMap() }.getOrDefault(emptyMap())
        val resultMap = mutableMapOf<String, ImmutableList<String>>()

        AiProvider.ALL.forEach { prov ->
            val savedList = rawMap[prov.id] ?: emptyList()
            resultMap[prov.id] = savedList.toImmutableList()
        }
        return resultMap.toImmutableMap()
    }

    fun onProviderTabSelected(
        providerType: String,
        flow: String = "App -> SettingsViewModel.onProviderTabSelected"
    ) {
        val providerObj = AiProvider.fromId(providerType)
        _uiState.update {
            it.copy(
                provider = providerObj,
                apiKeyError = null,
                apiKeySuccess = null
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            val savedModel = runCatching {
                manageProviderConfigUseCase.getLastSelectedModel(providerObj, flow = flow)
            }.getOrDefault("").ifBlank { providerObj.defaultModel }

            _uiState.update {
                it.copy(selectedModel = savedModel)
            }
            fetchModelsForProvider(providerObj, flow = flow)
        }
    }

    fun fetchModelsForProvider(
        targetProvider: AiProvider,
        flow: String = "App -> SettingsViewModel.fetchModelsForProvider"
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val apiKey = manageProviderConfigUseCase.getApiKeyForProvider(targetProvider.id)
            val customUrl = if (targetProvider == AiProvider.Custom) manageProviderConfigUseCase.getProviderConfig().customUrl else ""

            if (apiKey.isBlank() && targetProvider != AiProvider.OnDevice && targetProvider != AiProvider.Custom) {
                runCatching { manageProviderConfigUseCase.saveProviderModels(targetProvider.id, emptyList(), flow = flow) }
                _uiState.update { currentState ->
                    val updatedMap = currentState.providerModelsMap.toMutableMap()
                    updatedMap[targetProvider.id] = persistentListOf()
                    currentState.copy(providerModelsMap = updatedMap.toImmutableMap())
                }
                return@launch
            }

            val result = processPrescriptionUseCase.fetchAvailableModels(
                providedApiKey = apiKey,
                provider = targetProvider,
                customUrl = customUrl,
                flow = "$flow -> processPrescriptionUseCase.fetchAvailableModels"
            )

            result.fold(
                onSuccess = { modelList ->
                    val names = modelList.map { it.removePrefix(AppConstants.MODEL_PREFIX_GEMINI).trim() }.filter { it.isNotBlank() }.toImmutableList()
                    runCatching {
                        manageProviderConfigUseCase.saveProviderModels(targetProvider.id, names, flow = flow)
                    }
                    _uiState.update { currentState ->
                        val updatedMap = currentState.providerModelsMap.toMutableMap()
                        updatedMap[targetProvider.id] = names
                        currentState.copy(providerModelsMap = updatedMap.toImmutableMap())
                    }
                },
                onFailure = { ex ->
                    Napier.w("[$flow -> Warning] Failed to fetch models for ${targetProvider.displayName}: ${ex.message}", throwable = ex, tag = TAG)
                }
            )
        }
    }

    fun saveAndTestProviderConfig(
        providerType: String,
        customUrl: String,
        apiKey: String,
        selectedModel: String,
        flow: String = "App -> SettingsViewModel.saveAndTestProviderConfig"
    ) {
        val prov = AiProvider.fromId(providerType)
        _uiState.update { it.copy(isTestingApiKey = true, apiKeyError = null, apiKeySuccess = null) }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                manageProviderConfigUseCase.saveProviderConfig(
                    providerType = providerType,
                    customUrl = customUrl,
                    apiKey = apiKey,
                    modelName = selectedModel,
                    themeMode = _uiState.value.themeMode.id,
                    flow = flow
                )
            }

            val testResult = processPrescriptionUseCase.fetchAvailableModels(
                providedApiKey = apiKey,
                provider = prov,
                customUrl = customUrl,
                flow = "$flow -> testApiKey"
            )

            testResult.fold(
                onSuccess = { models ->
                    val modelNames = models.map { it.removePrefix(AppConstants.MODEL_PREFIX_GEMINI).trim() }.filter { it.isNotBlank() }.toImmutableList()
                    runCatching { manageProviderConfigUseCase.saveProviderModels(prov.id, modelNames, flow = flow) }

                    _uiState.update { state ->
                        val updatedModelsMap = state.providerModelsMap.toMutableMap()
                        updatedModelsMap[prov.id] = modelNames

                        when (prov) {
                            AiProvider.Gemini -> state.copy(geminiApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = "Settings saved.", providerModelsMap = updatedModelsMap.toImmutableMap())
                            AiProvider.Groq -> state.copy(groqApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = "Settings saved.", providerModelsMap = updatedModelsMap.toImmutableMap())
                            AiProvider.OpenAi -> state.copy(openaiApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = "Settings saved.", providerModelsMap = updatedModelsMap.toImmutableMap())
                            AiProvider.Claude -> state.copy(claudeApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = "Settings saved.", providerModelsMap = updatedModelsMap.toImmutableMap())
                            AiProvider.Custom -> state.copy(customApiKey = apiKey, customUrl = customUrl, isTestingApiKey = false, apiKeySuccess = "Settings saved.", providerModelsMap = updatedModelsMap.toImmutableMap())
                            AiProvider.OnDevice -> state.copy(isTestingApiKey = false, apiKeySuccess = "Settings saved.", providerModelsMap = updatedModelsMap.toImmutableMap())
                        }
                    }
                },
                onFailure = { ex ->
                    _uiState.update {
                        it.copy(isTestingApiKey = false, apiKeyError = ex.message ?: "Failed to verify configuration.")
                    }
                }
            )
        }
    }

    fun onSelectOnDeviceStrategy(strategy: OnDeviceStrategy, flow: String = "App -> SettingsViewModel.onSelectOnDeviceStrategy") {
        _uiState.update { it.copy(onDeviceStrategy = strategy) }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { manageProviderConfigUseCase.saveOnDeviceStrategy(strategy, flow = flow) }
        }
        fetchModelsForProvider(AiProvider.OnDevice, flow = "$flow -> fetchModelsForProvider.OnDevice")
    }

    fun onSaveSlmDirectoryPath(path: String, flow: String = "App -> SettingsViewModel.onSaveSlmDirectoryPath") {
        val trimmed = path.trim()
        _uiState.update { it.copy(slmModelDirectory = trimmed) }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { manageProviderConfigUseCase.saveSlmModelDirectory(trimmed, flow = flow) }
        }
        fetchModelsForProvider(AiProvider.OnDevice, flow = "$flow -> fetchModelsForProvider.OnDevice")
    }

    fun updateThemeMode(themeMode: ThemeMode, flow: String = "App -> SettingsViewModel.updateThemeMode") {
        _uiState.update { it.copy(themeMode = themeMode) }
        val currentProviderType = _uiState.value.providerType
        val customUrl = _uiState.value.customUrl
        val apiKey = _uiState.value.apiKey
        val selectedModel = _uiState.value.selectedModel

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                manageProviderConfigUseCase.saveProviderConfig(
                    providerType = currentProviderType,
                    customUrl = customUrl,
                    apiKey = apiKey,
                    modelName = selectedModel,
                    themeMode = themeMode.id,
                    flow = flow
                )
            }
        }
    }

    fun onMcpServerToggled(enabled: Boolean, flow: String = "App -> SettingsViewModel.onMcpServerToggled") {
        _uiState.update { it.copy(isMcpServerEnabled = enabled) }
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { manageProviderConfigUseCase.saveMcpServerEnabled(enabled, flow = flow) }
        }
    }

    fun onAgentProviderSelected(agentType: RxAgentType, newProvider: AiProvider) {
        val currentConfig = _uiState.value.agentConfigs[agentType]
        val models = _uiState.value.providerModelsMap[newProvider.id] ?: emptyList()
        val chosenModel = models.firstOrNull() ?: ""

        val updatedConfig = AgentConfig(
            agentType = agentType,
            provider = newProvider,
            selectedModel = chosenModel,
            fallbackProvider = currentConfig?.fallbackProvider,
            fallbackModel = currentConfig?.fallbackModel ?: ""
        )

        _uiState.update { currentState ->
            val updatedMap = currentState.agentConfigs.toMutableMap()
            updatedMap[agentType] = updatedConfig
            currentState.copy(agentConfigs = updatedMap.toImmutableMap())
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                manageProviderConfigUseCase.saveAgentConfig(
                    agentType = agentType,
                    provider = newProvider,
                    modelName = chosenModel,
                    fallbackProvider = currentConfig?.fallbackProvider,
                    fallbackModel = currentConfig?.fallbackModel ?: ""
                )
            }
        }

        if (models.isEmpty()) {
            fetchModelsForProvider(newProvider)
        }
    }

    fun onAgentModelSelected(agentType: RxAgentType, modelName: String) {
        val currentConfig = _uiState.value.agentConfigs[agentType] ?: AgentConfig(agentType, AiProvider.Gemini)
        val updated = currentConfig.copy(selectedModel = modelName)

        _uiState.update { currentState ->
            val updatedMap = currentState.agentConfigs.toMutableMap()
            updatedMap[agentType] = updated
            currentState.copy(agentConfigs = updatedMap.toImmutableMap())
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                manageProviderConfigUseCase.saveAgentConfig(
                    agentType = agentType,
                    provider = updated.provider,
                    modelName = updated.selectedModel,
                    fallbackProvider = updated.fallbackProvider,
                    fallbackModel = updated.fallbackModel
                )
            }
        }
    }

    fun onAgentFallbackProviderSelected(agentType: RxAgentType, fallbackProvider: AiProvider) {
        val currentConfig = _uiState.value.agentConfigs[agentType]
        val models = _uiState.value.providerModelsMap[fallbackProvider.id] ?: emptyList()
        val chosenModel = models.firstOrNull() ?: ""

        val existing = currentConfig ?: AgentConfig(agentType, AiProvider.Gemini)
        val updated = existing.copy(fallbackProvider = fallbackProvider, fallbackModel = chosenModel)

        _uiState.update { currentState ->
            val updatedMap = currentState.agentConfigs.toMutableMap()
            updatedMap[agentType] = updated
            currentState.copy(agentConfigs = updatedMap.toImmutableMap())
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                manageProviderConfigUseCase.saveAgentConfig(
                    agentType = agentType,
                    provider = existing.provider,
                    modelName = existing.selectedModel,
                    fallbackProvider = fallbackProvider,
                    fallbackModel = chosenModel
                )
            }
        }

        if (models.isEmpty()) {
            fetchModelsForProvider(fallbackProvider)
        }
    }

    fun onAgentFallbackModelSelected(agentType: RxAgentType, fallbackModel: String) {
        val existing = _uiState.value.agentConfigs[agentType] ?: AgentConfig(agentType, AiProvider.Gemini)
        val updated = existing.copy(fallbackModel = fallbackModel)

        _uiState.update { currentState ->
            val updatedMap = currentState.agentConfigs.toMutableMap()
            updatedMap[agentType] = updated
            currentState.copy(agentConfigs = updatedMap.toImmutableMap())
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                manageProviderConfigUseCase.saveAgentConfig(
                    agentType = agentType,
                    provider = updated.provider,
                    modelName = updated.selectedModel,
                    fallbackProvider = updated.fallbackProvider,
                    fallbackModel = updated.fallbackModel
                )
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
