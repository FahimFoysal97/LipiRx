package com.snehonir.lipirx.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.snehonir.lipirx.data.local.dao.AgentExecutionTraceDao
import com.snehonir.lipirx.data.local.dao.AppConfigDao
import com.snehonir.lipirx.data.local.dao.DispensedHistoryDao
import com.snehonir.lipirx.data.local.dao.PrescriptionHistoryDao
import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.data.local.entity.AppConfigEntity
import com.snehonir.lipirx.data.local.entity.DispensedHistoryEntity
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity

@Database(
    entities = [
        PrescriptionHistoryEntity::class,
        AppConfigEntity::class,
        DispensedHistoryEntity::class,
        AgentExecutionTraceEntity::class,
        ProviderConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
@ConstructedBy(LipiRxRoomDatabaseConstructor::class)
abstract class LipiRxRoomDatabase : RoomDatabase() {
    abstract fun prescriptionHistoryDao(): PrescriptionHistoryDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun dispensedHistoryDao(): DispensedHistoryDao
    abstract fun agentExecutionTraceDao(): AgentExecutionTraceDao
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object LipiRxRoomDatabaseConstructor : RoomDatabaseConstructor<LipiRxRoomDatabase> {
    override fun initialize(): LipiRxRoomDatabase
}
