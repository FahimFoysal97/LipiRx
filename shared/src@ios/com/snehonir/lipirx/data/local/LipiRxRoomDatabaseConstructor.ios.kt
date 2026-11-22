package com.snehonir.lipirx.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(): RoomDatabase.Builder<LipiRxRoomDatabase> {
    val docDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dbPath = requireNotNull(docDir?.path) + "/lipirx.db"
    return Room.databaseBuilder<LipiRxRoomDatabase>(name = dbPath)
}