package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.local.ProviderConfigEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.repository.PrescriptionRepository

class ManageProviderConfigUseCase(
    private val repository: PrescriptionRepository
) {
    fun getProviderConfig(flow: String = ""): ProviderConfigEntity {
        return repository.getProviderConfig(flow)
    }

    fun saveProviderConfig(providerType: String, customUrl: String, apiKey: String, modelName: String, flow: String = "") {
        repository.saveProviderConfig(providerType, customUrl, apiKey, modelName, flow)
    }

    fun getApiKeyForProvider(providerType: String): String {
        return repository.getApiKeyForProvider(providerType)
    }

    fun saveSelectedModel(modelName: String, provider: AiProvider = AiProvider.Gemini, flow: String = "") {
        repository.saveSelectedModel(modelName, provider, flow)
    }

    fun getLastSelectedModel(provider: AiProvider = AiProvider.Gemini, flow: String = ""): String {
        return repository.getLastSelectedModel(provider, flow)
    }

    fun getOnDeviceStrategy(): OnDeviceStrategy {
        return repository.getOnDeviceStrategy()
    }

    fun saveOnDeviceStrategy(strategy: OnDeviceStrategy, flow: String = "") {
        repository.saveOnDeviceStrategy(strategy, flow)
    }

    fun getAgentConfig(agentType: RxAgentType): AgentConfig {
        return repository.getAgentConfig(agentType)
    }

    fun getAllAgentConfigs(): Map<RxAgentType, AgentConfig> {
        return repository.getAllAgentConfigs()
    }

    fun saveAgentConfig(agentType: RxAgentType, provider: AiProvider, modelName: String = "", flow: String = "") {
        repository.saveAgentConfig(agentType, provider, modelName, flow)
    }

    fun saveProviderModels(providerType: String, models: List<String>, flow: String = "") {
        repository.saveProviderModels(providerType, models, flow)
    }

    fun getAllProviderModelsMap(): Map<String, List<String>> {
        return repository.getAllProviderModelsMap()
    }
}
