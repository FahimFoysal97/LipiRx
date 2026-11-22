package com.snehonir.lipirx.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.snehonir.lipirx.data.local.entity.AppConfigEntity

@Dao
interface AppConfigDao {
    @Query("SELECT configValue FROM AppConfig WHERE configKey = :key LIMIT 1")
    fun getConfigValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveConfigValue(entity: AppConfigEntity)
}
