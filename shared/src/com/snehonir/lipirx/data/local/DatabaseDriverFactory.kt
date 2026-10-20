package com.snehonir.lipirx.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import io.github.aakira.napier.Napier

val LipiRxSchema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        Napier.d("Creating PrescriptionHistory and AppConfig tables in SQLite database", tag = "LipiRxSchema")
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
)

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
            Napier.d("Guaranteed AppConfig table exists in SQLite database", tag = TAG)
        }.onFailure { ex ->
            Napier.w("Failed to ensure AppConfig table exists: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun saveSummary(summaryText: String, flow: String = "App -> LipiRxLocalDatabase.saveSummary") {
        val currentTime = 1700000000000L
        Napier.d("[$flow] Executing SQL INSERT into PrescriptionHistory: summaryLength=${summaryText.length}", tag = TAG)
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

    fun saveSelectedModel(modelName: String, flow: String = "App -> LipiRxLocalDatabase.saveSelectedModel") {
        Napier.d("[$flow] Executing SQL INSERT OR REPLACE INTO AppConfig: last_selected_model='$modelName'", tag = TAG)
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES ('last_selected_model', ?)",
            parameters = 1
        ) {
            bindString(0, modelName)
        }
    }

    fun getLastSelectedModel(flow: String = "App -> LipiRxLocalDatabase.getLastSelectedModel"): String {
        return getConfigValue("last_selected_model")
    }

    fun saveApiKey(apiKey: String, flow: String = "App -> LipiRxLocalDatabase.saveApiKey") {
        val cleanKey = apiKey.trim()
        Napier.d("[$flow] Executing SQL INSERT OR REPLACE INTO AppConfig: gemini_api_key (length=${cleanKey.length})", tag = TAG)
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES ('gemini_api_key', ?)",
            parameters = 1
        ) {
            bindString(0, cleanKey)
        }
    }

    fun getApiKey(flow: String = "App -> LipiRxLocalDatabase.getApiKey"): String {
        return getConfigValue("gemini_api_key")
    }

    fun saveProviderConfig(
        providerType: String,
        customUrl: String,
        apiKey: String,
        modelName: String,
        flow: String = "App -> LipiRxLocalDatabase.saveProviderConfig"
    ) {
        val cleanKey = apiKey.trim()
        val providerKeyName = when (providerType) {
            "groq" -> "groq_api_key"
            "openai" -> "openai_api_key"
            "custom" -> "custom_api_key"
            else -> "gemini_api_key"
        }

        Napier.d("[$flow] Saving provider config: type=$providerType, keyName=$providerKeyName, customUrl=$customUrl, model=$modelName", tag = TAG)

        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES ('provider_type', ?)",
            parameters = 1
        ) { bindString(0, providerType.trim()) }

        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES ('custom_url', ?)",
            parameters = 1
        ) { bindString(0, customUrl.trim()) }

        // Save under provider-specific key name
        driver.execute(
            identifier = null,
            sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES (?, ?)",
            parameters = 2
        ) {
            bindString(0, providerKeyName)
            bindString(1, cleanKey)
        }

        if (modelName.isNotBlank()) {
            driver.execute(
                identifier = null,
                sql = "INSERT OR REPLACE INTO AppConfig (configKey, configValue) VALUES ('last_selected_model', ?)",
                parameters = 1
            ) { bindString(0, modelName.trim()) }
        }
    }

    fun getApiKeyForProvider(providerType: String): String {
        val keyName = when (providerType) {
            "groq" -> "groq_api_key"
            "openai" -> "openai_api_key"
            "custom" -> "custom_api_key"
            else -> "gemini_api_key"
        }
        return getConfigValue(keyName)
    }

    fun getProviderConfig(flow: String = "App -> LipiRxLocalDatabase.getProviderConfig"): ProviderConfigEntity {
        val providerType = getConfigValue("provider_type").ifBlank { "gemini" }
        val customUrl = getConfigValue("custom_url")
        val apiKey = getApiKeyForProvider(providerType)
        val modelName = getConfigValue("last_selected_model")

        val config = ProviderConfigEntity(
            providerType = providerType,
            customUrl = customUrl,
            apiKey = apiKey,
            modelName = modelName
        )
        Napier.d("[$flow -> Result] Provider Config retrieved: $config", tag = TAG)
        return config
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
