package com.snehonir.lipirx.domain.repository

import com.snehonir.lipirx.data.local.PrescriptionHistoryEntity
import com.snehonir.lipirx.data.local.ProviderConfigEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy

interface PrescriptionRepository {
    fun saveSummary(summaryText: String, timestampMillis: Long = 0L, flow: String = "")
    fun saveDispensedHistory(medicines: List<MedicineItem>, flow: String = "")
    fun getEpisodicMemoryContext(generics: List<String>, flow: String = ""): String
    fun getHistory(flow: String = ""): List<PrescriptionHistoryEntity>
    fun deleteHistoryRecord(id: Long, flow: String = "")
    fun clearAllHistory(flow: String = "")
    fun saveOnDeviceStrategy(strategy: OnDeviceStrategy, flow: String = "")
    fun getOnDeviceStrategy(): OnDeviceStrategy
    fun saveMcpServerEnabled(enabled: Boolean, flow: String = "")
    fun isMcpServerEnabled(): Boolean
    fun saveSelectedModel(modelName: String, provider: AiProvider = AiProvider.Gemini, flow: String = "")
    fun getLastSelectedModel(provider: AiProvider = AiProvider.Gemini, flow: String = ""): String
    fun saveApiKey(apiKey: String, flow: String = "")
    fun getApiKey(flow: String = ""): String
    fun saveProviderConfig(providerType: String, customUrl: String, apiKey: String, modelName: String, flow: String = "")
    fun getApiKeyForProvider(providerType: String): String
    fun getProviderConfig(flow: String = ""): ProviderConfigEntity
    fun saveAgentConfig(agentType: RxAgentType, provider: AiProvider, modelName: String = "", flow: String = "")
    fun getAgentConfig(agentType: RxAgentType): AgentConfig
    fun getAllAgentConfigs(): Map<RxAgentType, AgentConfig>
    fun saveProviderModels(providerType: String, models: List<String>, flow: String = "")
    fun getProviderModels(providerType: String): List<String>
    fun getAllProviderModelsMap(): Map<String, List<String>>
}
