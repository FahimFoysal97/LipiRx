package com.snehonir.lipirx.presentation.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScanResultsScreen(
    viewModel: ScanResultsViewModel = koinViewModel(),
    onZoom: () -> Unit,
    onDone: () -> Unit,
    onHistoryClick: () -> Unit,
    onBackFromHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    MedicineListView(
        imageByteArray = uiState.imageByteArray,
        medicines = uiState.medicines,
        isSuspendedForApproval = uiState.isSuspendedForApproval,
        approvalReason = uiState.approvalReason,
        selectedHistoryItem = uiState.selectedHistoryItem,
        onZoom = onZoom,
        onCopySummary = {
            val textSummary = viewModel.copyToClipboard()
            if (textSummary.isNotBlank()) {
                clipboardManager.setText(AnnotatedString(textSummary))
            }
        },
        onRescan = {
            viewModel.clearResults()
            onDone()
        },
        onApproveAndConfirm = {
            viewModel.approveAndResumeAnalysis()
        },
        onUpdateMedicineItem = { index, updatedItem ->
            viewModel.onUpdateMedicineItem(index, updatedItem)
        },
        onHistoryClick = onHistoryClick,
        onBackFromHistory = {
            viewModel.clearSelectedHistoryRecord()
            onBackFromHistory()
        }
    )
}
