package com.snehonir.lipirx.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

actual fun getPlatformEpochMillis(): Long {
    val refSeconds = NSDate().timeIntervalSinceReferenceDate
    return ((refSeconds + 978307200.0) * 1000).toLong()
}

actual fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val epochSeconds = if (epochMillis <= 0) (getPlatformEpochMillis() / 1000.0) else epochMillis / 1000.0
    val refSeconds = epochSeconds - 978307200.0
    val date = NSDate(timeIntervalSinceReferenceDate = refSeconds)
    val formatter = NSDateFormatter().apply {
        dateFormat = AppConstants.DATETIME_FORMAT_FULL_TIMESTAMP
        locale = NSLocale(localeIdentifier = "en_US")
    }
    return formatter.stringFromDate(date)
}

actual fun scanInstalledOnDeviceModels(): List<ModelInfoDto> {
    return emptyList()
}

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = LipiRxSchema,
            name = AppConstants.DATABASE_NAME
        )
    }
}
