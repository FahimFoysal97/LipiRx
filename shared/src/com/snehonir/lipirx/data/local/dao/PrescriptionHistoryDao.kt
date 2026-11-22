package com.snehonir.lipirx.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionHistoryDao {
    @Query("SELECT * FROM PrescriptionHistory ORDER BY id DESC")
    fun getAllHistory(): List<PrescriptionHistoryEntity>

    @Query("SELECT * FROM PrescriptionHistory ORDER BY id DESC")
    fun getAllHistoryFlow(): Flow<List<PrescriptionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(entity: PrescriptionHistoryEntity)

    @Query("DELETE FROM PrescriptionHistory WHERE id = :id")
    fun deleteHistoryRecord(id: Long)

    @Query("DELETE FROM PrescriptionHistory")
    fun clearAllHistory()
}
