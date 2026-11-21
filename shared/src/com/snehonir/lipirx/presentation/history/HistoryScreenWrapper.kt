package com.snehonir.lipirx.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.presentation.history.HistoryScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreenWrapper(
    viewModel: HistoryViewModel = koinViewModel(),
    onClose: () -> Unit,
    onSelectRecord: (PrescriptionHistoryEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreen(
        historyList = uiState.historyList,
        onClose = onClose,
        onSelectRecord = { record ->
            viewModel.openRecord(record)
            onSelectRecord(record)
        },
        onDeleteRecord = { id ->
            viewModel.deleteRecord(id)
        },
        onClearAllHistory = {
            viewModel.clearAllHistory()
        }
    )
}
