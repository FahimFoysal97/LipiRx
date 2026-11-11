package com.snehonir.lipirx.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.snehonir.lipirx.data.remote.ModelInfoDto
import com.snehonir.lipirx.data.remote.initOnDeviceEngineContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var appContextRef: Context? = null

actual fun getPlatformEpochMillis(): Long = System.currentTimeMillis()

actual fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val date = Date(if (epochMillis <= 0) System.currentTimeMillis() else epochMillis)
    val sdf = SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a", Locale.ENGLISH)
    return sdf.format(date)
}

actual fun scanInstalledOnDeviceModels(): List<ModelInfoDto> {
    val foundModels = mutableListOf<ModelInfoDto>()
    val context = appContextRef ?: return emptyList()

    val dirsToScan = listOfNotNull(
        context.filesDir,
        context.getExternalFilesDir(null)
    )

    dirsToScan.forEach { dir ->
        dir.listFiles()?.forEach { file ->
            val name = file.name
            if (file.isFile && (name.endsWith(".litertlm", ignoreCase = true) || name.endsWith(".tflite", ignoreCase = true))) {
                val sizeMb = file.length() / (1024 * 1024)
                foundModels.add(
                    ModelInfoDto(
                        name = name,
                        displayName = "$name ($sizeMb MB)"
                    )
                )
            }
        }
    }

    return foundModels.distinctBy { it.name }
}

actual class DatabaseDriverFactory(private val context: Context) {
    init {
        appContextRef = context.applicationContext
        initOnDeviceEngineContext(context.applicationContext)
    }

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = LipiRxSchema,
            context = context,
            name = "lipirx.db"
        )
    }
}
