package com.snehonir.lipirx.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.snehonir.lipirx.data.remote.ModelInfoDto
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.AppConfigKey
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

expect fun getPlatformEpochMillis(): Long
expect fun formatEpochMillisToDateTime(epochMillis: Long): String
expect fun scanInstalledOnDeviceModels(): List<ModelInfoDto>

val LipiRxSchema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        Napier.d("Creating PrescriptionHistory, AppConfig, and DispensedHistory tables in SQLite database", tag = "LipiRxSchema")
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS PrescriptionHistory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scannedAt INTEGER NOT NULL,
                    medicineSummary TEXT NOT NULL
                );
            """.trimIndent(),
            parameters = 0
        )
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS AppConfig (
                    configKey TEXT PRIMARY KEY,
                    configValue TEXT NOT NULL
                );
            """.trimIndent(),
            parameters = 0
        )
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS DispensedHistory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dispensedAt INTEGER NOT NULL,
                    medicineName TEXT NOT NULL,
                    genericName TEXT NOT NULL,
                    dispensedBrand TEXT NOT NULL
                );
            """.trimIndent(),
            parameters = 0
        )
        return QueryResult.Unit
    }

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: app.cash.sqldelight.db.AfterVersion
    ): QueryResult.Value<Unit> {
        return QueryResult.Unit
    }
}

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

data class PrescriptionHistoryEntity(
    val id: Long,
    val scannedAt: Long,
    val medicineSummary: String
) {
    val formattedDateTime: String
        get() = formatEpochMillisToDateTime(scannedAt)
}

data class ProviderConfigEntity(
    val providerType: String = "gemini", // "gemini", "groq", "openai", "custom"
    val customUrl: String = "",
    val apiKey: String = "",
    val modelName: String = ""
)

class LipiRxLocalDatabase(private val driver: SqlDriver) {

    init {
        runCatching {
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE IF NOT EXISTS AppConfig (
                        configKey TEXT PRIMARY KEY,
                        configValue TEXT NOT NULL
                    );
                """.trimIndent(),
                parameters = 0
            )
            driver.execute(
                identifier = null,
                sql = """
                    CREATE TABLE IF NOT EXISTS DispensedHistory (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        dispensedAt INTEGER NOT NULL,
                        medicineName TEXT NOT NULL,
                        genericName TEXT NOT NULL,
                        dispensedBrand TEXT NOT NULL
                    );
                """.trimIndent(),
                parameters = 0
            )
            Napier.d("Guaranteed AppConfig and DispensedHistory tables exist in SQLite database", tag = TAG)
        }.onFailure { ex ->
            Napier.w("Failed to ensure tables exist in SQLite database: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun saveSummary(
        summaryText: String,
        timestampMillis: Long = getPlatformEpochMillis(),
        flow: String = "App -> LipiRxLocalDatabase.saveSummary"
    ) {
        val currentTime = if (timestampMillis > 0) timestampMillis else getPlatformEpochMillis()
        Napier.d("[$flow] Executing SQL INSERT into PrescriptionHistory: summaryLength=${summaryText.length}, timestamp=$currentTime", tag = TAG)
        driver.execute(
            identifier = null,
            sql = "INSERT INTO PrescriptionHistory (scannedAt, medicineSummary) VALUES (?, ?)",
            parameters = 2
        ) {
            bindLong(0, currentTime)
            bindString(1, summaryText)
        }
        Napier.d("[$flow -> Success] Saved prescription summary into SQLite database successfully", tag = TAG)
    }

    fun saveDispensedHistory(
        medicines: List<MedicineItem>,
        flow: String = "App -> LipiRxLocalDatabase.saveDispensedHistory"
    ) {
        val currentTime = getPlatformEpochMillis()
        Napier.d("[$flow] Saving ${medicines.size} items to DispensedHistory episodic memory table", tag = TAG)
        medicines.forEach { item ->
            val brand = item.formattedFullName
            val generic = item.genericName
            if (generic.isNotBlank() && brand.isNotBlank()) {
                runCatching {
                    driver.execute(
                        identifier = null,
                        sql = "INSERT INTO DispensedHistory (dispensedAt, medicineName, genericName, dispensedBrand) VALUES (?, ?, ?, ?)",
                        parameters = 4
                    ) {
                        bindLong(0, currentTime)
                        bindString(1, item.medicineName)
                        bindString(2, generic)
                        bindString(3, brand)
                    }
                }
            }
        }
    }

    fun getEpisodicMemoryContext(
        generics: List<String>,
        flow: String = "App -> LipiRxLocalDatabase.getEpisodicMemoryContext"
    ): String {
        if (generics.isEmpty()) return ""
        val historyList = mutableListOf<String>()

        generics.filter { it.isNotBlank() }.distinct().forEach { generic ->
            runCatching {
                val queryResult = driver.executeQuery(
                    identifier = null,
                    sql = "SELECT dispensedBrand, dispensedAt FROM DispensedHistory WHERE genericName LIKE ? ORDER BY id DESC LIMIT 3",
                    mapper = { cursor ->
                        val brands = mutableListOf<String>()
                        while (cursor.next().value) {
                            val brand = cursor.getString(0) ?: ""
                            if (brand.isNotBlank() && !brands.contains(brand)) {
                                brands.add(brand)
                            }
                        }
                        QueryResult.Value(brands)
                    },
                    parameters = 1
                ) {
                    bindString(0, "%$generic%")
                }
                val matchedBrands = queryResult.value
                if (matchedBrands.isNotEmpty()) {
                    historyList.add("Generic '$generic': Previously dispensed ${matchedBrands.joinToString(", ")}")
                }
            }
        }

        if (historyList.isEmpty()) return ""
        Napier.d("[$flow -> Episodic Memory] Found ${historyList.size} past episodic memory records for generics", tag = TAG)
        return buildString {
            appendLine("CROSS-SESSION EPISODIC MEMORY (PAST PATIENT REFILL & DISPENSING HISTORY):")
            historyList.forEach { appendLine("- $it") }
        }
    }

    fun getHistory(flow: String = "App -> LipiRxLocalDatabase.getHistory"): List<PrescriptionHistoryEntity> {
        Napier.d("[$flow] Querying SELECT PrescriptionHistory from SQLite database...", tag = TAG)
        val queryResult = driver.executeQuery(
            identifier = null,
            sql = "SELECT id, scannedAt, medicineSummary FROM PrescriptionHistory ORDER BY id DESC",
            mapper = { cursor ->
                val list = mutableListOf<PrescriptionHistoryEntity>()
                while (cursor.next().value) {
                    list.add(
                        PrescriptionHistoryEntity(
                            id = cursor.getLong(0) ?: 0L,
                            scannedAt = cursor.getLong(1) ?: 0L,
                            medicineSummary = cursor.getString(2) ?: ""
                        )
                    )
                }
                QueryResult.Value(list)
            },
            parameters = 0
        )
        val records = queryResult.value
        Napier.d("[$flow -> Result] Retrieved ${records.size} history records from SQLite database", tag = TAG)
        return records
    }

    fun deleteHistoryRecord(id: Long, flow: String = "App -> LipiRxLocalDatabase.deleteHistoryRecord") {
        Napier.d("[$flow] Deleting history record ID=$id from SQLite", tag = TAG)
        runCatching {
            driver.execute(
                identifier = null,
                sql = "DELETE FROM PrescriptionHistory WHERE id = ?",
                parameters = 1
            ) {
                bindLong(0, id)
            }
        }.onFailure { ex ->
            Napier.w("[$flow -> Warning] Failed to delete history record $id: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun clearAllHistory(flow: String = "App -> LipiRxLocalDatabase.clearAllHistory") {
        Napier.d("[$flow] Deleting all records from PrescriptionHistory table in SQLite", tag = TAG)
        runCatching {
            driver.execute(
                identifier = null,
                sql = "DELETE FROM PrescriptionHistory",
                parameters = 0
            )
        }.onFailure { ex ->
            Napier.w("[$flow -> Warning] Failed to clear all history: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun saveOnDeviceStrategy(
        strategy: com.snehonir.lipirx.domain.model.OnDeviceStrategy,
        flow: String = "App -> LipiRxLocalDatabase.saveOnDeviceStrategy"
    ) {
        Napier.d("[$flow] Saving On-Device strategy '${strategy.id}' into SQLite", tag = TAG)
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, AppConfigKey.ON_DEVICE_STRATEGY.key)
            bindString(1, strategy.id)
        }
    }

    fun getOnDeviceStrategy(): com.snehonir.lipirx.domain.model.OnDeviceStrategy {
        val raw = getConfigValue(AppConfigKey.ON_DEVICE_STRATEGY.key)
        return if (raw.isNotBlank()) com.snehonir.lipirx.domain.model.OnDeviceStrategy.fromId(raw) else com.snehonir.lipirx.domain.model.OnDeviceStrategy.HYBRID_FALLBACK
    }

    fun saveSelectedModel(
        modelName: String,
        provider: AiProvider = AiProvider.Gemini,
        flow: String = "App -> LipiRxLocalDatabase.saveSelectedModel"
    ) {
        val modelKey = AppConfigKey.keyForProviderModel(provider)
        Napier.d("[$flow] Executing SQL INSERT OR REPLACE INTO AppConfig: $modelKey='$modelName'", tag = TAG)
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, modelKey)
            bindString(1, modelName)
        }
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, AppConfigKey.LAST_SELECTED_MODEL.key)
            bindString(1, modelName)
        }
    }

    fun getLastSelectedModel(
        provider: AiProvider = AiProvider.Gemini,
        flow: String = "App -> LipiRxLocalDatabase.getLastSelectedModel"
    ): String {
        val modelKey = AppConfigKey.keyForProviderModel(provider)
        val value = getConfigValue(modelKey)
        return if (value.isNotBlank()) value else getConfigValue(AppConfigKey.LAST_SELECTED_MODEL.key).ifBlank { provider.defaultModel }
    }

    fun saveApiKey(apiKey: String, flow: String = "App -> LipiRxLocalDatabase.saveApiKey") {
        val cleanKey = apiKey.trim()
        Napier.d("[$flow] Executing SQL INSERT OR REPLACE INTO AppConfig: gemini_api_key (length=${cleanKey.length})", tag = TAG)
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, AppConfigKey.GEMINI_API_KEY.key)
            bindString(1, cleanKey)
        }
    }

    fun getApiKey(flow: String = "App -> LipiRxLocalDatabase.getApiKey"): String {
        return getConfigValue(AppConfigKey.GEMINI_API_KEY.key)
    }

    fun saveProviderConfig(
        providerType: String,
        customUrl: String,
        apiKey: String,
        modelName: String,
        flow: String = "App -> LipiRxLocalDatabase.saveProviderConfig"
    ) {
        val cleanKey = apiKey.trim()
        val providerObj = AiProvider.fromId(providerType)
        val providerKey = AppConfigKey.keyForProvider(providerObj)

        Napier.d("[$flow] Saving provider config: provider=${providerObj.id}, keyName=${providerKey.key}, customUrl=$customUrl, model=$modelName", tag = TAG)

        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, AppConfigKey.PROVIDER_TYPE.key)
            bindString(1, providerObj.id)
        }

        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, AppConfigKey.CUSTOM_URL.key)
            bindString(1, customUrl.trim())
        }

        // Save under provider-specific key name
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, providerKey.key)
            bindString(1, cleanKey)
        }

        if (modelName.isNotBlank()) {
            driver.execute(
                identifier = null,
                sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
                parameters = 2
            ) {
                bindString(0, AppConfigKey.LAST_SELECTED_MODEL.key)
                bindString(1, modelName.trim())
            }
        }
    }

    fun getApiKeyForProvider(providerType: String): String {
        val providerObj = AiProvider.fromId(providerType)
        val key = AppConfigKey.keyForProvider(providerObj)
        return getConfigValue(key.key)
    }

    fun getProviderConfig(flow: String = "App -> LipiRxLocalDatabase.getProviderConfig"): ProviderConfigEntity {
        val providerType = getConfigValue(AppConfigKey.PROVIDER_TYPE.key).ifBlank { AiProvider.Gemini.id }
        val customUrl = getConfigValue(AppConfigKey.CUSTOM_URL.key)
        val apiKey = getApiKeyForProvider(providerType)
        val modelName = getConfigValue(AppConfigKey.LAST_SELECTED_MODEL.key)

        val config = ProviderConfigEntity(
            providerType = providerType,
            customUrl = customUrl,
            apiKey = apiKey,
            modelName = modelName
        )
        Napier.d("[$flow -> Result] Provider Config retrieved: $config", tag = TAG)
        return config
    }

    fun saveAgentConfig(
        agentType: RxAgentType,
        provider: AiProvider,
        modelName: String = "",
        flow: String = "App -> LipiRxLocalDatabase.saveAgentConfig"
    ) {
        val providerKey = AppConfigKey.keyForAgentProvider(agentType)
        val modelKey = AppConfigKey.keyForAgentModel(agentType)

        Napier.d("[$flow] Saving agent config: agent=${agentType.id}, provider=${provider.id}, model=$modelName", tag = TAG)

        runCatching {
            driver.execute(
                identifier = null,
                sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
                parameters = 2
            ) {
                bindString(0, providerKey)
                bindString(1, provider.id)
            }

            if (modelName.isNotBlank()) {
                driver.execute(
                    identifier = null,
                    sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
                    parameters = 2
                ) {
                    bindString(0, modelKey)
                    bindString(1, modelName.trim())
                }
            }
        }.onFailure { ex ->
            Napier.w("[$flow -> Warning] Failed to save agent config into SQLite: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun getAgentConfig(agentType: RxAgentType): AgentConfig {
        val providerKey = AppConfigKey.keyForAgentProvider(agentType)
        val modelKey = AppConfigKey.keyForAgentModel(agentType)

        val rawProviderId = getConfigValue(providerKey)
        val provider = if (rawProviderId.isNotBlank()) AiProvider.fromId(rawProviderId) else agentType.defaultProvider
        val modelName = getConfigValue(modelKey).ifBlank { agentType.defaultModel }

        return AgentConfig(
            agentType = agentType,
            provider = provider,
            selectedModel = modelName
        )
    }

    fun getAllAgentConfigs(): Map<RxAgentType, AgentConfig> {
        return RxAgentType.ALL.associateWith { agentType ->
            getAgentConfig(agentType)
        }
    }

    fun saveProviderModels(
        providerType: String,
        models: List<String>,
        flow: String = "App -> LipiRxLocalDatabase.saveProviderModels"
    ) {
        val configKey = "models_list_${providerType.trim().lowercase()}"
        val serializedModels = models.joinToString(separator = "||")
        Napier.d("[$flow] Saving ${models.size} live models for provider '$providerType' into SQLite", tag = TAG)

        runCatching {
            driver.execute(
                identifier = null,
                sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
                parameters = 2
            ) {
                bindString(0, configKey)
                bindString(1, serializedModels)
            }
        }.onFailure { ex ->
            Napier.w("[$flow -> Warning] Failed to save provider models into SQLite: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun getProviderModels(providerType: String): List<String> {
        val configKey = "models_list_${providerType.trim().lowercase()}"
        val rawValue = getConfigValue(configKey)
        if (rawValue.isBlank()) return emptyList()
        return rawValue.split("||").map { it.trim() }.filter { it.isNotBlank() }
    }

    fun getAllProviderModelsMap(): Map<String, List<String>> {
        return mapOf(
            "gemini" to getProviderModels("gemini"),
            "groq" to getProviderModels("groq"),
            "openai" to getProviderModels("openai"),
            "custom" to getProviderModels("custom"),
            "on_device" to getProviderModels("on_device")
        )
    }

    private fun getConfigValue(key: String): String {
        val queryResult = driver.executeQuery(
            identifier = null,
            sql = "SELECT configValue FROM AppConfig WHERE configKey = ?",
            mapper = { cursor ->
                val value = if (cursor.next().value) cursor.getString(0) ?: "" else ""
                QueryResult.Value(value)
            },
            parameters = 1
        ) {
            bindString(0, key)
        }
        return queryResult.value
    }

    companion object {
        private const val TAG = "LipiRxLocalDatabase"
    }
}
