package com.snehonir.lipirx.di

import com.snehonir.lipirx.data.local.DatabaseDriverFactory
import com.snehonir.lipirx.domain.update.AndroidPlatformUpdater
import com.snehonir.lipirx.domain.update.PlatformUpdater
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(context = get()) }
    single<PlatformUpdater> { AndroidPlatformUpdater(context = get(), httpClient = get()) }
}
