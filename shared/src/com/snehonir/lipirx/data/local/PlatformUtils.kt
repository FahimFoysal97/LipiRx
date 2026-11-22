package com.snehonir.lipirx.data.local

import com.snehonir.lipirx.data.remote.dto.ModelInfoDto

expect fun getPlatformEpochMillis(): Long

expect fun formatEpochMillisToDateTime(epochMillis: Long): String

expect fun scanInstalledOnDeviceModels(customDirectoryPath: String? = null): List<ModelInfoDto>
