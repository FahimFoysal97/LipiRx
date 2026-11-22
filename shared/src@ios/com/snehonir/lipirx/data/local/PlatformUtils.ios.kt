package com.snehonir.lipirx.data.local

import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

actual fun getPlatformEpochMillis(): Long {
    return (NSDate().timeIntervalSince1970() * 1000).toLong()
}

actual fun formatEpochMillisToDateTime(epochMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMM dd, yyyy, hh:mm:ss a"
        locale = NSLocale.currentLocale()
    }
    return formatter.stringFromDate(date)
}

actual fun scanInstalledOnDeviceModels(customDirectoryPath: String?): List<ModelInfoDto> {
    return emptyList()
}
