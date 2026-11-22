package com.snehonir.lipirx.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<LipiRxRoomDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("lipirx.db")
    return Room.databaseBuilder<LipiRxRoomDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}