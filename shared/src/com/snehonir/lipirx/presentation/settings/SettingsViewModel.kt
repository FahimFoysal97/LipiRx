package com.snehonir.lipirx.presentation.settings

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.common.LipiRxNoParams
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.usecase.FetchAvailableModelsUseCase
import com.snehonir.lipirx.domain.usecase.FetchModelsParams
import com.snehonir.lipirx.domain.usecase.GetAgentConfigsUseCase
import com.snehonir.lipirx.domain.usecase.GetProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.ManageMcpServerUseCase
import com.snehonir.lipirx.domain.usecase.ManageOnDeviceStrategyUseCase
import com.snehonir.lipirx.domain.usecase.McpServerParams
import com.snehonir.lipirx.domain.usecase.SaveAgentConfigParams
import com.snehonir.lipirx.domain.usecase.SaveAgentConfigUseCase
import com.snehonir.lipirx.domain.usecase.SaveProviderConfigParams
import com.snehonir.lipirx.domain.usecase.SaveProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.StrategyParams
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_connection_failed
import com.snehonir.lipirx.resources.error_no_models_found
import com.snehonir.lipirx.resources.success_on_device_saved
import com.snehonir.lipirx.resources.success_provider_verified
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.getString

class SettingsViewModel(
    private val getProviderConfigUseCase: GetProviderConfigUseCase,
    private val saveProviderConfigUseCase: SaveProviderConfigUseCase,
    private val getAgentConfigsUseCase: GetAgentConfigsUseCase,
    private val saveAgentConfigUseCase: SaveAgentConfigUseCase,
    private val manageOnDeviceStrategyUseCase: ManageOnDeviceStrategyUseCase,
    private val manageMcpServerUseCase: ManageMcpServerUseCase,
    private val fetchAvailableModelsUseCase: FetchAvailableModelsUseCase,
    private val repository: PrescriptionRepository
) : LipiRxViewModel<SettingsUiState>(SettingsUiState()) {

    init {
        loadSettingsState()
    }

    fun loadSettingsState(flow: String = "App -> $TAG.loadSettingsState") {
        Napier.d("[$flow] Loading settings state from SQLite DB", tag = TAG)
        launchSafe(Dispatchers.Default) {
            val config = getProviderConfigUseCase(LipiRxNoParams).getOrDefault(ProviderConfigEntity())
            val providerObj = AiProvider.fromId(config.providerType)

            val geminiKey = repository.getApiKeyForProvider(AiProvider.Gemini.id)
            val groqKey = repository.getApiKeyForProvider(AiProvider.Groq.id)
            val openaiKey = repository.getApiKeyForProvider(AiProvider.OpenAi.id)
            val claudeKey = repository.getApiKeyForProvider(AiProvider.Claude.id)
            val customKey = repository.getApiKeyForProvider(AiProvider.Custom.id)

            val savedStrategy = repository.getOnDeviceStrategy()
            val slmDir = runCatching { repository.getSlmModelDirectory() }.getOrDefault("")
            val mcpEnabled = runCatching { repository.isMcpServerEnabled() }.getOrDefault(false)
            val themeMode = ThemeMode.fromId(config.themeMode)

            val loadedAgentConfigs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()
            val loadedProviderModelsMap = loadProviderModelsMap()

            updateState {
                copy(
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
                val key = repository.getApiKeyForProvider(prov.id)
                val customUrl = if (prov == AiProvider.Custom) getProviderConfigUseCase(LipiRxNoParams).getOrDefault(ProviderConfigEntity()).customUrl else ""
                val canFetch = key.isNotBlank() || prov == AiProvider.OnDevice || (prov == AiProvider.Custom && customUrl.isNotBlank())

                if (canFetch) {
                    fetchModelsForProvider(prov, flow = "$flow.${prov.displayName}")
                }
            }
        }
    }

    private suspend fun loadProviderModelsMap(): ImmutableMap<String, ImmutableList<String>> {
        val rawMap = runCatching { repository.getAllProviderModelsMap() }.getOrDefault(emptyMap())
        val resultMap = mutableMapOf<String, ImmutableList<String>>()

        AiProvider.ALL.forEach { prov ->
            val savedList = rawMap[prov.id] ?: emptyList()
            resultMap[prov.id] = savedList.toImmutableList()
        }
        return resultMap.toImmutableMap()
    }

    fun onProviderTabSelected(
        providerType: String,
        flow: String = "App -> $TAG.onProviderTabSelected"
    ) {
        val providerObj = AiProvider.fromId(providerType)
        updateState {
            copy(
                provider = providerObj,
                apiKeyError = null,
                apiKeySuccess = null
            )
        }

        launchSafe(Dispatchers.Default) {
            val savedModel = runCatching {
                repository.getLastSelectedModel(providerObj, flow = flow)
            }.getOrDefault("").ifBlank { providerObj.defaultModel }

            updateState {
                copy(selectedModel = savedModel)
            }
            fetchModelsForProvider(providerObj, flow = flow)
        }
    }

    fun fetchModelsForProvider(
        targetProvider: AiProvider,
        flow: String = "App -> $TAG.fetchModelsForProvider"
    ) {
        launchSafe(Dispatchers.Default) {
            val apiKey = repository.getApiKeyForProvider(targetProvider.id)
            val customUrl = if (targetProvider == AiProvider.Custom) getProviderConfigUseCase(LipiRxNoParams).getOrDefault(ProviderConfigEntity()).customUrl else ""

            if (apiKey.isBlank() && targetProvider != AiProvider.OnDevice && targetProvider != AiProvider.Custom) {
                runCatching { repository.saveProviderModels(targetProvider.id, emptyList(), flow = flow) }
                updateState {
                    val updatedMap = providerModelsMap.toMutableMap()
                    updatedMap[targetProvider.id] = persistentListOf()
                    copy(providerModelsMap = updatedMap.toImmutableMap())
                }
                return@launchSafe
            }

            val result = fetchAvailableModelsUseCase(
                FetchModelsParams(
                    provider = targetProvider,
                    providedApiKey = apiKey,
                    customUrl = customUrl,
                    flow = "$flow -> fetchAvailableModelsUseCase"
                )
            )

            result.fold(
                onSuccess = { modelList ->
                    val names = modelList.map { it.removePrefix(AppConstants.MODEL_PREFIX_GEMINI).trim() }.filter { it.isNotBlank() }.toImmutableList()
                    runCatching {
                        repository.saveProviderModels(targetProvider.id, names, flow = flow)
                    }
                    updateState {
                        val updatedMap = providerModelsMap.toMutableMap()
                        updatedMap[targetProvider.id] = names
                        copy(providerModelsMap = updatedMap.toImmutableMap())
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
        flow: String = "App -> $TAG.saveAndTestProviderConfig"
    ) {
        val prov = AiProvider.fromId(providerType)
        updateState { copy(isTestingApiKey = true, apiKeyError = null, apiKeySuccess = null) }

        launchSafe(Dispatchers.Default) {
            saveProviderConfigUseCase(
                SaveProviderConfigParams(
                    providerType = providerType,
                    customUrl = customUrl,
                    apiKey = apiKey,
                    modelName = selectedModel,
                    themeMode = currentState.themeMode.id,
                    flow = flow
                )
            )

            val testResult = fetchAvailableModelsUseCase(
                FetchModelsParams(
                    provider = prov,
                    providedApiKey = apiKey,
                    customUrl = customUrl,
                    flow = "$flow -> testApiKey"
                )
            )

            testResult.fold(
                onSuccess = { models ->
                    val modelNames = models.map { it.removePrefix(AppConstants.MODEL_PREFIX_GEMINI).trim() }.filter { it.isNotBlank() }.toImmutableList()
                    runCatching { repository.saveProviderModels(prov.id, modelNames, flow = flow) }

                    if (modelNames.isEmpty() && prov.requiresApiKey) {
                        val noModelsMsg = getString(Res.string.error_no_models_found)
                        updateState {
                            copy(
                                isTestingApiKey = false,
                                apiKeySuccess = null,
                                apiKeyError = noModelsMsg
                            )
                        }
                    } else {
                        val verifiedMsg = if (prov == AiProvider.OnDevice) {
                            getString(Res.string.success_on_device_saved)
                        } else {
                            getString(Res.string.success_provider_verified, prov.displayName, modelNames.size)
                        }
                        updateState {
                            val updatedModelsMap = providerModelsMap.toMutableMap()
                            updatedModelsMap[prov.id] = modelNames

                            when (prov) {
                                AiProvider.Gemini -> copy(geminiApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = verifiedMsg, apiKeyError = null, providerModelsMap = updatedModelsMap.toImmutableMap())
                                AiProvider.Groq -> copy(groqApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = verifiedMsg, apiKeyError = null, providerModelsMap = updatedModelsMap.toImmutableMap())
                                AiProvider.OpenAi -> copy(openaiApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = verifiedMsg, apiKeyError = null, providerModelsMap = updatedModelsMap.toImmutableMap())
                                AiProvider.Claude -> copy(claudeApiKey = apiKey, isTestingApiKey = false, apiKeySuccess = verifiedMsg, apiKeyError = null, providerModelsMap = updatedModelsMap.toImmutableMap())
                                AiProvider.Custom -> copy(customApiKey = apiKey, customUrl = customUrl, isTestingApiKey = false, apiKeySuccess = verifiedMsg, apiKeyError = null, providerModelsMap = updatedModelsMap.toImmutableMap())
                                AiProvider.OnDevice -> copy(isTestingApiKey = false, apiKeySuccess = verifiedMsg, apiKeyError = null, providerModelsMap = updatedModelsMap.toImmutableMap())
                            }
                        }
                    }
                },
                onFailure = { ex ->
                    val connFailedMsg = getString(Res.string.error_connection_failed)
                    val errorDetail = ex.message?.ifBlank { null }
                    val finalMsg = if (errorDetail != null) "$connFailedMsg ($errorDetail)" else connFailedMsg
                    updateState {
                        copy(
                            isTestingApiKey = false,
                            apiKeySuccess = null,
                            apiKeyError = finalMsg
                        )
                    }
                }
            )
        }
    }

    fun onSelectOnDeviceStrategy(strategy: OnDeviceStrategy, flow: String = "App -> $TAG.onSelectOnDeviceStrategy") {
        updateState { copy(onDeviceStrategy = strategy) }
        launchSafe(Dispatchers.Default) {
            manageOnDeviceStrategyUseCase(StrategyParams(strategy = strategy, flow = flow))
        }
        fetchModelsForProvider(AiProvider.OnDevice, flow = "$flow -> fetchModelsForProvider.OnDevice")
    }

    fun onSaveSlmDirectoryPath(path: String, flow: String = "App -> $TAG.onSaveSlmDirectoryPath") {
        val trimmed = path.trim()
        updateState { copy(slmModelDirectory = trimmed) }
        launchSafe(Dispatchers.Default) {
            manageOnDeviceStrategyUseCase(StrategyParams(slmDirectoryPath = trimmed, flow = flow))
        }
        fetchModelsForProvider(AiProvider.OnDevice, flow = "$flow -> fetchModelsForProvider.OnDevice")
    }

    fun updateThemeMode(themeMode: ThemeMode, flow: String = "App -> $TAG.updateThemeMode") {
        updateState { copy(themeMode = themeMode) }
        val currentProviderType = currentState.providerType
        val customUrl = currentState.customUrl
        val apiKey = currentState.apiKey
        val selectedModel = currentState.selectedModel

        launchSafe(Dispatchers.Default) {
            saveProviderConfigUseCase(
                SaveProviderConfigParams(
                    providerType = currentProviderType,
                    customUrl = customUrl,
                    apiKey = apiKey,
                    modelName = selectedModel,
                    themeMode = themeMode.id,
                    flow = flow
                )
            )
        }
    }

    fun onMcpServerToggled(enabled: Boolean, flow: String = "App -> $TAG.onMcpServerToggled") {
        updateState { copy(isMcpServerEnabled = enabled) }
        launchSafe(Dispatchers.Default) {
            manageMcpServerUseCase(McpServerParams(enabled = enabled, flow = flow))
        }
    }

    fun onAgentProviderSelected(agentType: RxAgentType, newProvider: AiProvider) {
        val currentConfig = currentState.agentConfigs[agentType]
        val models = currentState.providerModelsMap[newProvider.id] ?: emptyList()
        val chosenModel = models.firstOrNull() ?: ""

        val updatedConfig = AgentConfig(
            agentType = agentType,
            provider = newProvider,
            selectedModel = chosenModel,
            fallbackProvider = currentConfig?.fallbackProvider,
            fallbackModel = currentConfig?.fallbackModel ?: ""
        )

        updateState {
            val updatedMap = agentConfigs.toMutableMap()
            updatedMap[agentType] = updatedConfig
            copy(agentConfigs = updatedMap.toImmutableMap())
        }

        launchSafe(Dispatchers.Default) {
            saveAgentConfigUseCase(
                SaveAgentConfigParams(
                    agentType = agentType,
                    provider = newProvider,
                    modelName = chosenModel,
                    fallbackProvider = currentConfig?.fallbackProvider,
                    fallbackModel = currentConfig?.fallbackModel ?: "",
                    flow = "$TAG.onAgentProviderSelected"
                )
            )
        }

        if (models.isEmpty()) {
            fetchModelsForProvider(newProvider)
        }
    }

    fun onAgentModelSelected(agentType: RxAgentType, modelName: String) {
        val currentConfig = currentState.agentConfigs[agentType] ?: AgentConfig(agentType, AiProvider.Gemini)
        val updated = currentConfig.copy(selectedModel = modelName)

        updateState {
            val updatedMap = agentConfigs.toMutableMap()
            updatedMap[agentType] = updated
            copy(agentConfigs = updatedMap.toImmutableMap())
        }

        launchSafe(Dispatchers.Default) {
            saveAgentConfigUseCase(
                SaveAgentConfigParams(
                    agentType = agentType,
                    provider = updated.provider,
                    modelName = updated.selectedModel,
                    fallbackProvider = updated.fallbackProvider,
                    fallbackModel = updated.fallbackModel,
                    flow = "$TAG.onAgentModelSelected"
                )
            )
        }
    }

    fun onAgentFallbackProviderSelected(agentType: RxAgentType, fallbackProvider: AiProvider) {
        val currentConfig = currentState.agentConfigs[agentType]
        val models = currentState.providerModelsMap[fallbackProvider.id] ?: emptyList()
        val chosenModel = models.firstOrNull() ?: ""

        val existing = currentConfig ?: AgentConfig(agentType, AiProvider.Gemini)
        val updated = existing.copy(fallbackProvider = fallbackProvider, fallbackModel = chosenModel)

        updateState {
            val updatedMap = agentConfigs.toMutableMap()
            updatedMap[agentType] = updated
            copy(agentConfigs = updatedMap.toImmutableMap())
        }

        launchSafe(Dispatchers.Default) {
            saveAgentConfigUseCase(
                SaveAgentConfigParams(
                    agentType = agentType,
                    provider = existing.provider,
                    modelName = existing.selectedModel,
                    fallbackProvider = fallbackProvider,
                    fallbackModel = chosenModel,
                    flow = "$TAG.onAgentFallbackProviderSelected"
                )
            )
        }

        if (models.isEmpty()) {
            fetchModelsForProvider(fallbackProvider)
        }
    }

    fun onAgentFallbackModelSelected(agentType: RxAgentType, fallbackModel: String) {
        val existing = currentState.agentConfigs[agentType] ?: AgentConfig(agentType, AiProvider.Gemini)
        val updated = existing.copy(fallbackModel = fallbackModel)

        updateState {
            val updatedMap = agentConfigs.toMutableMap()
            updatedMap[agentType] = updated
            copy(agentConfigs = updatedMap.toImmutableMap())
        }

        launchSafe(Dispatchers.Default) {
            saveAgentConfigUseCase(
                SaveAgentConfigParams(
                    agentType = agentType,
                    provider = updated.provider,
                    modelName = updated.selectedModel,
                    fallbackProvider = updated.fallbackProvider,
                    fallbackModel = fallbackModel,
                    flow = "$TAG.onAgentFallbackModelSelected"
                )
            )
        }
    }

    companion object {
        private val TAG = SettingsViewModel::class.simpleName
    }
}
