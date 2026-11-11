package com.snehonir.lipirx.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSinceReferenceDate
import platform.Foundation.timeIntervalSinceReferenceDate

actual fun getPlatformEpochMillis(): Long {
    val refSeconds = NSDate().timeIntervalSinceReferenceDate
    return ((refSeconds + 978307200.0) * 1000).toLong()
}

actual fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val epochSeconds = if (epochMillis <= 0) (getPlatformEpochMillis() / 1000.0) else epochMillis / 1000.0
    val refSeconds = epochSeconds - 978307200.0
    val date = NSDate.dateWithTimeIntervalSinceReferenceDate(refSeconds) as? NSDate ?: NSDate()
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMM dd, yyyy, hh:mm:ss a"
        locale = NSLocale(localeIdentifier = "en_US")
    }
    return formatter.stringFromDate(date)
}

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = LipiRxSchema,
            name = "lipirx.db"
        )
    }
}
