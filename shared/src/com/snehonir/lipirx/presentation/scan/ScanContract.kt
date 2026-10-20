package com.snehonir.lipirx.presentation.scan

import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ScanUiState(
    val isLoading: Boolean = false,
    val loadingStep: Int = 0,
    val loadingStatusText: String = "",
    val capturedImageByteArray: ByteArray? = null,
    val medicines: ImmutableList<MedicineItem> = persistentListOf(),
    val errorMessage: String? = null,
    val availableModels: ImmutableList<String> = persistentListOf(),
    val selectedModel: String = "",
    val isFetchingModels: Boolean = false,
    val provider: AiProvider = AiProvider.Gemini,
    val customUrl: String = "",
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val openaiApiKey: String = "",
    val customApiKey: String = "",
    val apiKeyError: String? = null,
    val isTestingApiKey: Boolean = false
) {
    val apiKey: String
        get() = when (provider) {
            AiProvider.Groq -> groqApiKey
            AiProvider.OpenAi -> openaiApiKey
            AiProvider.Custom -> customApiKey
            AiProvider.Gemini -> geminiApiKey
        }

    val providerType: String
        get() = provider.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScanUiState

        if (isLoading != other.isLoading) return false
        if (loadingStep != other.loadingStep) return false
        if (loadingStatusText != other.loadingStatusText) return false
        if (capturedImageByteArray != null) {
            if (other.capturedImageByteArray == null) return false
            if (!capturedImageByteArray.contentEquals(other.capturedImageByteArray)) return false
        } else if (other.capturedImageByteArray != null) return false
        if (medicines != other.medicines) return false
        if (errorMessage != other.errorMessage) return false
        if (availableModels != other.availableModels) return false
        if (selectedModel != other.selectedModel) return false
        if (isFetchingModels != other.isFetchingModels) return false
        if (provider != other.provider) return false
        if (customUrl != other.customUrl) return false
        if (geminiApiKey != other.geminiApiKey) return false
        if (groqApiKey != other.groqApiKey) return false
        if (openaiApiKey != other.openaiApiKey) return false
        if (customApiKey != other.customApiKey) return false
        if (apiKeyError != other.apiKeyError) return false
        if (isTestingApiKey != other.isTestingApiKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + loadingStep.hashCode()
        result = 31 * result + loadingStatusText.hashCode()
        result = 31 * result + (capturedImageByteArray?.contentHashCode() ?: 0)
        result = 31 * result + medicines.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + availableModels.hashCode()
        result = 31 * result + selectedModel.hashCode()
        result = 31 * result + isFetchingModels.hashCode()
        result = 31 * result + provider.hashCode()
        result = 31 * result + customUrl.hashCode()
        result = 31 * result + geminiApiKey.hashCode()
        result = 31 * result + groqApiKey.hashCode()
        result = 31 * result + openaiApiKey.hashCode()
        result = 31 * result + customApiKey.hashCode()
        result = 31 * result + (apiKeyError?.hashCode() ?: 0)
        result = 31 * result + isTestingApiKey.hashCode()
        return result
    }
}

sealed interface ScanUiEvent {
    data class ShowToast(val message: String) : ScanUiEvent
    data object CopyToClipboardSuccess : ScanUiEvent
}
