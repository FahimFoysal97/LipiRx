package com.snehonir.lipirx.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import io.github.aakira.napier.Napier

val LipiRxSchema = object : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        Napier.d("Creating PrescriptionHistory table in SQLite database", tag = "LipiRxSchema")
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

class LipiRxLocalDatabase(private val driver: SqlDriver) {
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

    companion object {
        private const val TAG = "LipiRxLocalDatabase"
    }
}
