package com.snehonir.lipirx.presentation.scan

import com.snehonir.lipirx.domain.model.MedicineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ScanUiState(
    val isLoading: Boolean = false,
    val capturedImageByteArray: ByteArray? = null,
    val medicines: ImmutableList<MedicineItem> = persistentListOf(),
    val errorMessage: String? = null,
    val availableModels: ImmutableList<String> = persistentListOf(),
    val selectedModel: String = "",
    val isFetchingModels: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ScanUiState

        if (isLoading != other.isLoading) return false
        if (capturedImageByteArray != null) {
            if (other.capturedImageByteArray == null) return false
            if (!capturedImageByteArray.contentEquals(other.capturedImageByteArray)) return false
        } else if (other.capturedImageByteArray != null) return false
        if (medicines != other.medicines) return false
        if (errorMessage != other.errorMessage) return false
        if (availableModels != other.availableModels) return false
        if (selectedModel != other.selectedModel) return false
        if (isFetchingModels != other.isFetchingModels) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + (capturedImageByteArray?.contentHashCode() ?: 0)
        result = 31 * result + medicines.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + availableModels.hashCode()
        result = 31 * result + selectedModel.hashCode()
        result = 31 * result + isFetchingModels.hashCode()
        return result
    }
}

sealed interface ScanUiEvent {
    data class ShowToast(val message: String) : ScanUiEvent
    data object CopyToClipboardSuccess : ScanUiEvent
}
