package com.snehonir.lipirx.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentExecutionTraceDao {
    @Query("SELECT * FROM AgentExecutionTrace ORDER BY id DESC LIMIT :limit")
    fun getAgentTraces(limit: Int): List<AgentExecutionTraceEntity>

    @Query("SELECT * FROM AgentExecutionTrace ORDER BY id DESC LIMIT :limit")
    fun getAgentTracesFlow(limit: Int): Flow<List<AgentExecutionTraceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrace(entity: AgentExecutionTraceEntity)

    @Query("DELETE FROM AgentExecutionTrace WHERE id IN (:traceIds)")
    fun deleteAgentTraces(traceIds: List<Long>)

    @Query("DELETE FROM AgentExecutionTrace")
    fun clearAgentTraces()
}
