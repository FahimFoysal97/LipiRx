package com.snehonir.lipirx.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel(),
    onClose: () -> Unit,
    onSelectRecord: (PrescriptionHistoryEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistoryList()
    }

    HistoryView(
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
