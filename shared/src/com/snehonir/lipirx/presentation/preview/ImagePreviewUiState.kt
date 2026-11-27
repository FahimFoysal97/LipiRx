package com.snehonir.lipirx.presentation.preview

import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

data class ImagePreviewUiState(
    val imageByteArray: ByteArray? = null,
    val agentConfigs: ImmutableMap<RxAgentType, AgentConfig> = persistentMapOf(),
    val errorMessage: String? = null,
    val isRetryableError: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImagePreviewUiState) return false

        if (imageByteArray != null) {
            if (other.imageByteArray == null) return false
            if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        } else if (other.imageByteArray != null) return false
        if (agentConfigs != other.agentConfigs) return false
        if (errorMessage != other.errorMessage) return false
        if (isRetryableError != other.isRetryableError) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageByteArray?.contentHashCode() ?: 0
        result = 31 * result + agentConfigs.hashCode()
        result = 31 * result + errorMessage.hashCode()
        result = 31 * result + isRetryableError.hashCode()
        return result
    }
}
