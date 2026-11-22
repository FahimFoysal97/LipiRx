package com.snehonir.lipirx.data.local

import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.data.local.entity.AppConfigEntity
import com.snehonir.lipirx.data.local.entity.DispensedHistoryEntity
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.AppConfigKey
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class LipiRxRoomRepository(
    private val database: LipiRxRoomDatabase
) : PrescriptionRepository {

    private val historyDao get() = database.prescriptionHistoryDao()
    private val configDao get() = database.appConfigDao()
    private val dispensedDao get() = database.dispensedHistoryDao()
    private val traceDao get() = database.agentExecutionTraceDao()

    override suspend fun saveSummary(
        summaryText: String,
        timestampMillis: Long,
        flow: String
    ) = withContext(Dispatchers.IO) {
        val currentTime = if (timestampMillis > 0) timestampMillis else getPlatformEpochMillis()
        Napier.d("[$flow] Room DB INSERT into PrescriptionHistory: summaryLength=${summaryText.length}", tag = TAG)
        historyDao.insertHistory(
            PrescriptionHistoryEntity(
                scannedAt = currentTime,
                medicineSummary = summaryText
            )
        )
    }

    override suspend fun saveDispensedHistory(
        medicines: List<MedicineItem>,
        flow: String
    ) = withContext(Dispatchers.IO) {
        val currentTime = getPlatformEpochMillis()
        Napier.d("[$flow] Room DB saving ${medicines.size} items to DispensedHistory", tag = TAG)
        medicines.forEach { item ->
            val brand = item.formattedFullName
            val generic = item.genericName
            if (generic.isNotBlank() && brand.isNotBlank()) {
                dispensedDao.insertDispensedHistory(
                    DispensedHistoryEntity(
                        dispensedAt = currentTime,
                        medicineName = item.medicineName,
                        genericName = generic,
                        dispensedBrand = brand
                    )
                )
            }
        }
    }

    override suspend fun getEpisodicMemoryContext(
        generics: List<String>,
        flow: String
    ): String = withContext(Dispatchers.IO) {
        if (generics.isEmpty()) return@withContext ""
        val historyList = mutableListOf<String>()

        generics.filter { it.isNotBlank() }.distinct().forEach { generic ->
            val matchedBrands = dispensedDao.getMatchedBrands(generic)
            if (matchedBrands.isNotEmpty()) {
                historyList.add("Generic '$generic': Previously dispensed ${matchedBrands.joinToString(", ")}")
            }
        }

        if (historyList.isEmpty()) return@withContext ""
        Napier.d("[$flow -> Room Memory] Found ${historyList.size} past episodic memory records for generics", tag = TAG)
        buildString {
            appendLine("CROSS-SESSION EPISODIC MEMORY (PAST PATIENT REFILL & DISPENSING HISTORY):")
            historyList.forEach { appendLine("- $it") }
        }
    }

    override suspend fun getHistory(flow: String): List<PrescriptionHistoryEntity> =
        withContext(Dispatchers.IO) {
            Napier.d("[$flow] Room DB querying SELECT PrescriptionHistory...", tag = TAG)
            historyDao.getAllHistory()
        }

    override suspend fun deleteHistoryRecord(id: Long, flow: String) =
        withContext(Dispatchers.IO) {
            Napier.d("[$flow] Room DB deleting history record ID=$id", tag = TAG)
            historyDao.deleteHistoryRecord(id)
        }

    override suspend fun clearAllHistory(flow: String) =
        withContext(Dispatchers.IO) {
            Napier.d("[$flow] Room DB deleting all history records", tag = TAG)
            historyDao.clearAllHistory()
        }

    override suspend fun saveOnDeviceStrategy(
        strategy: OnDeviceStrategy,
        flow: String
    ) {
        saveConfigValue(AppConfigKey.ON_DEVICE_STRATEGY.key, strategy.id)
    }

    override suspend fun getOnDeviceStrategy(): OnDeviceStrategy {
        val raw = getConfigValue(AppConfigKey.ON_DEVICE_STRATEGY.key)
        return if (raw.isNotBlank()) OnDeviceStrategy.fromId(raw) else OnDeviceStrategy.HYBRID_FALLBACK
    }

    override suspend fun saveMcpServerEnabled(enabled: Boolean, flow: String) {
        saveConfigValue(AppConfigKey.MCP_SERVER_ENABLED.key, enabled.toString())
    }

    override suspend fun isMcpServerEnabled(): Boolean {
        val raw = getConfigValue(AppConfigKey.MCP_SERVER_ENABLED.key)
        return raw.toBooleanStrictOrNull() ?: false
    }

    override suspend fun saveDismissedUpdateVersion(version: String, flow: String) {
        saveConfigValue(AppConfigKey.DISMISSED_UPDATE_VERSION.key, version)
    }

    override suspend fun getDismissedUpdateVersion(): String {
        return getConfigValue(AppConfigKey.DISMISSED_UPDATE_VERSION.key)
    }

    override suspend fun saveSlmModelDirectory(path: String, flow: String) {
        saveConfigValue(AppConfigKey.SLM_MODEL_DIRECTORY.key, path)
    }

    override suspend fun getSlmModelDirectory(): String {
        return getConfigValue(AppConfigKey.SLM_MODEL_DIRECTORY.key)
    }

    override suspend fun saveSelectedModel(
        modelName: String,
        provider: AiProvider,
        flow: String
    ) {
        val modelKey = AppConfigKey.keyForProviderModel(provider)
        saveConfigValue(modelKey, modelName)
        saveConfigValue(AppConfigKey.LAST_SELECTED_MODEL.key, modelName)
    }

    override suspend fun getLastSelectedModel(
        provider: AiProvider,
        flow: String
    ): String {
        val modelKey = AppConfigKey.keyForProviderModel(provider)
        val value = getConfigValue(modelKey)
        return value.ifBlank {
            getConfigValue(AppConfigKey.LAST_SELECTED_MODEL.key).ifBlank { provider.defaultModel }
        }
    }

    override suspend fun saveApiKey(apiKey: String, flow: String) {
        saveConfigValue(AppConfigKey.GEMINI_API_KEY.key, apiKey.trim())
    }

    override suspend fun getApiKey(flow: String): String {
        return getConfigValue(AppConfigKey.GEMINI_API_KEY.key)
    }

    override suspend fun saveProviderConfig(
        providerType: String,
        customUrl: String,
        apiKey: String,
        modelName: String,
        themeMode: String,
        flow: String
    ) {
        val cleanKey = apiKey.trim()
        val providerObj = AiProvider.fromId(providerType)
        val providerKey = AppConfigKey.keyForProvider(providerObj)

        saveConfigValue(AppConfigKey.PROVIDER_TYPE.key, providerObj.id)
        saveConfigValue(AppConfigKey.CUSTOM_URL.key, customUrl.trim())
        saveConfigValue(providerKey.key, cleanKey)

        if (modelName.isNotBlank()) {
            saveConfigValue(AppConfigKey.LAST_SELECTED_MODEL.key, modelName.trim())
        }

        saveConfigValue(AppConfigKey.THEME_MODE.key, themeMode.trim().lowercase())
    }

    override suspend fun getApiKeyForProvider(providerType: String): String {
        val providerObj = AiProvider.fromId(providerType)
        val key = AppConfigKey.keyForProvider(providerObj)
        return getConfigValue(key.key)
    }

    override suspend fun getProviderConfig(flow: String): ProviderConfigEntity {
        val providerType = getConfigValue(AppConfigKey.PROVIDER_TYPE.key).ifBlank { AiProvider.Gemini.id }
        val customUrl = getConfigValue(AppConfigKey.CUSTOM_URL.key)
        val apiKey = getApiKeyForProvider(providerType)
        val modelName = getConfigValue(AppConfigKey.LAST_SELECTED_MODEL.key)
        val themeMode = getConfigValue(AppConfigKey.THEME_MODE.key).ifBlank { "system" }

        return ProviderConfigEntity(
            providerType = providerType,
            customUrl = customUrl,
            apiKey = apiKey,
            modelName = modelName,
            themeMode = themeMode
        )
    }

    override suspend fun saveAgentConfig(
        agentType: RxAgentType,
        provider: AiProvider,
        modelName: String,
        fallbackProvider: AiProvider?,
        fallbackModel: String,
        flow: String
    ) {
        val providerKey = AppConfigKey.keyForAgentProvider(agentType)
        val modelKey = AppConfigKey.keyForAgentModel(agentType)
        val fallbackProviderKey = AppConfigKey.keyForAgentFallbackProvider(agentType)
        val fallbackModelKey = AppConfigKey.keyForAgentFallbackModel(agentType)

        saveConfigValue(providerKey, provider.id)
        if (modelName.isNotBlank()) saveConfigValue(modelKey, modelName.trim())
        if (fallbackProvider != null) saveConfigValue(fallbackProviderKey, fallbackProvider.id)
        if (fallbackModel.isNotBlank()) saveConfigValue(fallbackModelKey, fallbackModel.trim())
    }

    override suspend fun getAgentConfig(agentType: RxAgentType): AgentConfig {
        val providerKey = AppConfigKey.keyForAgentProvider(agentType)
        val modelKey = AppConfigKey.keyForAgentModel(agentType)
        val fallbackProviderKey = AppConfigKey.keyForAgentFallbackProvider(agentType)
        val fallbackModelKey = AppConfigKey.keyForAgentFallbackModel(agentType)

        val rawProviderId = getConfigValue(providerKey)
        val provider = if (rawProviderId.isNotBlank()) AiProvider.fromId(rawProviderId) else agentType.defaultProvider
        val modelName = getConfigValue(modelKey).ifBlank { agentType.defaultModel }

        val rawFallbackProviderId = getConfigValue(fallbackProviderKey)
        val fallbackProvider = if (rawFallbackProviderId.isNotBlank()) AiProvider.fromId(rawFallbackProviderId) else null
        val fallbackModel = getConfigValue(fallbackModelKey)

        return AgentConfig(
            agentType = agentType,
            provider = provider,
            selectedModel = modelName,
            fallbackProvider = fallbackProvider,
            fallbackModel = fallbackModel
        )
    }

    override suspend fun getAllAgentConfigs(): Map<RxAgentType, AgentConfig> {
        return RxAgentType.ALL.associateWith { getAgentConfig(it) }
    }

    override suspend fun saveProviderModels(
        providerType: String,
        models: List<String>,
        flow: String
    ) {
        val configKey = "models_list_${providerType.trim().lowercase()}"
        saveConfigValue(configKey, models.joinToString("||"))
    }

    override suspend fun getProviderModels(providerType: String): List<String> {
        val configKey = "models_list_${providerType.trim().lowercase()}"
        val rawValue = getConfigValue(configKey)
        if (rawValue.isBlank()) return emptyList()
        return rawValue.split("||").map { it.trim() }.filter { it.isNotBlank() }
    }

    override suspend fun getAllProviderModelsMap(): Map<String, List<String>> {
        return AiProvider.ALL.associate { prov -> prov.id to getProviderModels(prov.id) }
    }

    override suspend fun saveAgentTrace(
        turnNumber: Int,
        agentType: String,
        provider: String,
        modelName: String,
        reasoning: String,
        feedbackPrompt: String,
        itemCount: Int,
        executionTimeMs: Long,
        isSuccess: Boolean,
        errorMessage: String,
        flow: String
    ) = withContext(Dispatchers.IO) {
        val currentTime = getPlatformEpochMillis()
        traceDao.insertTrace(
            AgentExecutionTraceEntity(
                timestamp = currentTime,
                turnNumber = turnNumber,
                agentType = agentType,
                provider = provider,
                modelName = modelName,
                reasoning = reasoning,
                feedbackPrompt = feedbackPrompt,
                itemCount = itemCount,
                executionTimeMs = executionTimeMs,
                isSuccess = isSuccess,
                errorMessage = errorMessage
            )
        )
    }

    override suspend fun getAgentTraces(limit: Int, flow: String): List<AgentExecutionTraceEntity> =
        withContext(Dispatchers.IO) {
            traceDao.getAgentTraces(limit)
        }

    override suspend fun deleteAgentTraces(traceIds: List<Long>, flow: String) =
        withContext(Dispatchers.IO) {
            traceDao.deleteAgentTraces(traceIds)
        }

    override suspend fun clearAgentTraces(flow: String) =
        withContext(Dispatchers.IO) {
            traceDao.clearAgentTraces()
        }

    private suspend fun saveConfigValue(key: String, value: String) =
        withContext(Dispatchers.IO) {
            configDao.saveConfigValue(AppConfigEntity(configKey = key, configValue = value))
        }

    private suspend fun getConfigValue(key: String): String =
        withContext(Dispatchers.IO) {
            configDao.getConfigValue(key) ?: ""
        }

    companion object {
        private const val TAG = "LipiRxRoomRepository"
    }
}