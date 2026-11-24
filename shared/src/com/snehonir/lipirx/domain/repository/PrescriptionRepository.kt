package com.snehonir.lipirx.domain.repository

import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy

import com.snehonir.lipirx.data.common.LipiRxRepository

interface PrescriptionRepository : LipiRxRepository {
    suspend fun saveSummary(summaryText: String, timestampMillis: Long = 0L, flow: String = "")
    suspend fun saveDispensedHistory(medicines: List<MedicineItem>, flow: String = "")
    suspend fun getEpisodicMemoryContext(generics: List<String>, flow: String = ""): String
    suspend fun getHistory(flow: String = ""): List<PrescriptionHistoryEntity>
    suspend fun deleteHistoryRecord(id: Long, flow: String = "")
    suspend fun clearAllHistory(flow: String = "")
    suspend fun saveOnDeviceStrategy(strategy: OnDeviceStrategy, flow: String = "")
    suspend fun getOnDeviceStrategy(): OnDeviceStrategy
    suspend fun saveMcpServerEnabled(enabled: Boolean, flow: String = "")
    suspend fun isMcpServerEnabled(): Boolean
    suspend fun saveDismissedUpdateVersion(version: String, flow: String = "")
    suspend fun getDismissedUpdateVersion(): String
    suspend fun saveSlmModelDirectory(path: String, flow: String = "")
    suspend fun getSlmModelDirectory(): String
    suspend fun saveSelectedModel(modelName: String, provider: AiProvider = AiProvider.Gemini, flow: String = "")
    suspend fun getLastSelectedModel(provider: AiProvider = AiProvider.Gemini, flow: String = ""): String
    suspend fun saveApiKey(apiKey: String, flow: String = "")
    suspend fun getApiKey(flow: String = ""): String
    suspend fun saveProviderConfig(providerType: String, customUrl: String, apiKey: String, modelName: String, themeMode: String = "system", flow: String = "")
    suspend fun getApiKeyForProvider(providerType: String): String
    suspend fun getProviderConfig(flow: String = ""): ProviderConfigEntity
    suspend fun saveAgentConfig(agentType: RxAgentType, provider: AiProvider, modelName: String = "", fallbackProvider: AiProvider? = null, fallbackModel: String = "", flow: String = "")
    suspend fun getAgentConfig(agentType: RxAgentType): AgentConfig
    suspend fun getAllAgentConfigs(): Map<RxAgentType, AgentConfig>
    suspend fun saveProviderModels(providerType: String, models: List<String>, flow: String = "")
    suspend fun getProviderModels(providerType: String): List<String>
    suspend fun getAllProviderModelsMap(): Map<String, List<String>>
    suspend fun saveAgentTrace(turnNumber: Int, agentType: String, provider: String, modelName: String, reasoning: String, feedbackPrompt: String = "", itemCount: Int, executionTimeMs: Long, isSuccess: Boolean, errorMessage: String = "", flow: String = "")
    suspend fun getAgentTraces(limit: Int = 100, flow: String = ""): List<AgentExecutionTraceEntity>
    suspend fun deleteAgentTraces(traceIds: List<Long>, flow: String = "")
    suspend fun clearAgentTraces(flow: String = "")
}