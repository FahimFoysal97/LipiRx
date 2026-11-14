package com.snehonir.lipirx.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
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
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE IF NOT EXISTS AgentExecutionTrace (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp INTEGER NOT NULL,
                    turnNumber INTEGER NOT NULL,
                    agentType TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    modelName TEXT NOT NULL,
                    reasoning TEXT NOT NULL,
                    feedbackPrompt TEXT,
                    itemCount INTEGER NOT NULL,
                    executionTimeMs INTEGER NOT NULL,
                    isSuccess INTEGER NOT NULL,
                    errorMessage TEXT
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
