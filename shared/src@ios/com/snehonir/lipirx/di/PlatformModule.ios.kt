package com.snehonir.lipirx.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.snehonir.lipirx.data.local.LipiRxRoomDatabase
import com.snehonir.lipirx.data.local.LipiRxRoomDatabaseConstructor
import com.snehonir.lipirx.domain.update.IosPlatformUpdater
import com.snehonir.lipirx.domain.update.PlatformUpdater
import platform.Foundation.NSHomeDirectory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<PlatformUpdater> { IosPlatformUpdater() }

    single<RoomDatabase.Builder<LipiRxRoomDatabase>> {
        val dbFile = NSHomeDirectory() + "/lipirx_room.db"
        Room.databaseBuilder<LipiRxRoomDatabase>(
            name = dbFile,
            factory = { LipiRxRoomDatabaseConstructor.initialize() }
        )
    }
}
