package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.model.AiProvider

data class TranscriptionInput(
    val imageByteArray: ByteArray,
    val selectedModel: String,
    val provider: AiProvider,
    val providedApiKey: String = "",
    val customUrl: String = "",
    val mimeType: String = AppConstants.MIME_TYPE_JPEG,
    val onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherInput = other as? TranscriptionInput ?: return false
        return imageByteArray.contentEquals(otherInput.imageByteArray) &&
                selectedModel == otherInput.selectedModel &&
                provider == otherInput.provider &&
                providedApiKey == otherInput.providedApiKey &&
                customUrl == otherInput.customUrl &&
                mimeType == otherInput.mimeType
    }

    override fun hashCode(): Int {
        var result = imageByteArray.contentHashCode()
        result = 31 * result + selectedModel.hashCode()
        result = 31 * result + provider.hashCode()
        result = 31 * result + providedApiKey.hashCode()
        result = 31 * result + customUrl.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
