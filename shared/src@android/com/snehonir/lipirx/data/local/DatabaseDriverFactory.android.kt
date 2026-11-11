package com.snehonir.lipirx.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getPlatformEpochMillis(): Long = System.currentTimeMillis()

actual fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val date = Date(if (epochMillis <= 0) System.currentTimeMillis() else epochMillis)
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a", Locale.ENGLISH)
    return sdf.format(date)
}

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = LipiRxSchema,
            context = context,
            name = "lipirx.db"
        )
    }
}
