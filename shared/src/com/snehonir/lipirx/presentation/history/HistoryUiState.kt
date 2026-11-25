package com.snehonir.lipirx.presentation.history

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class HistoryUiState(
    val historyList: ImmutableList<PrescriptionHistoryEntity> = persistentListOf(),
    val isLoading: Boolean = false
)
