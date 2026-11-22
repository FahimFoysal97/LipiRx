package com.snehonir.lipirx.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.snehonir.lipirx.data.local.LipiRxRoomDatabase
import com.snehonir.lipirx.data.local.LipiRxRoomDatabaseConstructor
import com.snehonir.lipirx.domain.update.AndroidPlatformUpdater
import com.snehonir.lipirx.domain.update.PlatformUpdater
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlatformUpdater> { AndroidPlatformUpdater(context = get(), httpClient = get()) }

    single<RoomDatabase.Builder<LipiRxRoomDatabase>> {
        val context = get<android.content.Context>()
        val dbFile = context.getDatabasePath("lipirx_room.db")
        Room.databaseBuilder<LipiRxRoomDatabase>(
            context = context,
            name = dbFile.absolutePath,
            factory = { LipiRxRoomDatabaseConstructor.initialize() }
        ).setDriver(BundledSQLiteDriver())
    }
}
