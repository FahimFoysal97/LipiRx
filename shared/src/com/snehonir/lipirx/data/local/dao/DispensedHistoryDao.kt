package com.snehonir.lipirx.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.snehonir.lipirx.data.local.entity.DispensedHistoryEntity

@Dao
interface DispensedHistoryDao {
    @Query("SELECT dispensedBrand FROM DispensedHistory WHERE genericName LIKE '%' || :generic || '%' ORDER BY id DESC LIMIT 3")
    fun getMatchedBrands(generic: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDispensedHistory(entity: DispensedHistoryEntity)
}
