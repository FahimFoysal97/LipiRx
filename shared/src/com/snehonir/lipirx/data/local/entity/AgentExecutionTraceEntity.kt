package com.snehonir.lipirx.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.snehonir.lipirx.data.local.formatEpochMillisToDateTime
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "AgentExecutionTrace")
data class AgentExecutionTraceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val turnNumber: Int,
    val agentType: String,
    val provider: String,
    val modelName: String,
    val reasoning: String,
    val feedbackPrompt: String = "",
    val itemCount: Int,
    val executionTimeMs: Long,
    val isSuccess: Boolean,
    val errorMessage: String = ""
) {
    val formattedDateTime: String
        get() = formatEpochMillisToDateTime(timestamp)
}
