package com.snehonir.lipirx.presentation.scan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.snehonir.lipirx.presentation.scan.components.EmptyStateView
import com.snehonir.lipirx.presentation.scan.components.FullScreenImagePreviewDialog
import com.snehonir.lipirx.presentation.scan.components.HistoryScreen
import com.snehonir.lipirx.presentation.scan.components.ImagePreviewAndProcessView
import com.snehonir.lipirx.presentation.scan.components.LoadingView
import com.snehonir.lipirx.presentation.scan.components.MedicineListView
import com.snehonir.lipirx.presentation.scan.components.SettingsScreen
import com.snehonir.lipirx.presentation.telemetry.AgentTelemetryScreen
import com.snehonir.lipirx.presentation.telemetry.AgentTraceViewModel
import com.snehonir.lipirx.ui.BackHandler
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

private const val TAG = "ScanResultScreen"

private sealed interface ScanScreenMode {
    data object Empty : ScanScreenMode
    data class ImageSelected(val byteArray: ByteArray) : ScanScreenMode {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ImageSelected) return false
            return byteArray.contentEquals(other.byteArray)
        }

        override fun hashCode(): Int = byteArray.contentHashCode()
    }
    data object Loading : ScanScreenMode
    data object Results : ScanScreenMode
    data object History : ScanScreenMode
    data object Telemetry : ScanScreenMode
    data object Settings : ScanScreenMode
}

@Composable
fun ScanResultScreen(
    viewModel: ScanViewModel,
    flow: String = "App -> ScanResultScreen"
) {
    Napier.d("[$flow] Rendering ScanResultScreen composable", tag = TAG)

    val uiState by viewModel.uiState.collectAsState()
    val traceViewModel: AgentTraceViewModel = koinInject()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val imagePicker = rememberImagePicker { imageBytes ->
        val pickerFlow = "$flow -> ImagePicker.onImagePicked"
        Napier.d("[$pickerFlow] Prescription image received: ${imageBytes.size} bytes. Storing preview.", tag = TAG)
        viewModel.onImageSelected(imageBytes, flow = pickerFlow)
    }

    var showViewer by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showTelemetryScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }

    val isBackHandlerEnabled = showSettingsScreen || showTelemetryScreen || showHistoryScreen || uiState.selectedHistoryItem != null || uiState.medicines.isNotEmpty() || uiState.capturedImageByteArray != null

    BackHandler(enabled = isBackHandlerEnabled) {
        when {
            showSettingsScreen -> {
                showSettingsScreen = false
            }
            showTelemetryScreen -> {
                showTelemetryScreen = false
            }
            uiState.selectedHistoryItem != null -> {
                viewModel.clearSelectedHistoryRecord(flow = "$flow -> BackHandler.clearSelectedHistoryRecord")
                showHistoryScreen = true
            }
            showHistoryScreen -> {
                showHistoryScreen = false
            }
            uiState.medicines.isNotEmpty() -> {
                viewModel.clearResults(flow = "$flow -> BackHandler.clearResults")
            }
            uiState.capturedImageByteArray != null -> {
                viewModel.clearSelectedImage(flow = "$flow -> BackHandler.clearSelectedImage")
            }
        }
    }

    val currentScreenMode: ScanScreenMode = when {
        showSettingsScreen -> ScanScreenMode.Settings
        showTelemetryScreen -> ScanScreenMode.Telemetry
        showHistoryScreen -> ScanScreenMode.History
        uiState.isLoading -> ScanScreenMode.Loading
        uiState.selectedHistoryItem != null || uiState.medicines.isNotEmpty() -> ScanScreenMode.Results
        uiState.capturedImageByteArray != null -> ScanScreenMode.ImageSelected(uiState.capturedImageByteArray!!)
        else -> ScanScreenMode.Empty
    }

    LipiRxTheme(themeMode = uiState.themeMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Lipi.Background)
        ) {
            AnimatedContent(
                targetState = currentScreenMode,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 40 }) togetherWith fadeOut(tween(160))
                },
                label = "scanState",
            ) { mode ->
                when (mode) {
                    is ScanScreenMode.Settings -> {
                        SettingsScreen(
                            selectedModel = uiState.selectedModel,
                            apiKey = uiState.apiKey,
                            geminiApiKey = uiState.geminiApiKey,
                            groqApiKey = uiState.groqApiKey,
                            openaiApiKey = uiState.openaiApiKey,
                            customApiKey = uiState.customApiKey,
                            providerType = uiState.providerType,
                            customUrl = uiState.customUrl,
                            apiKeyError = uiState.apiKeyError,
                            apiKeySuccess = uiState.apiKeySuccess,
                            isTestingApiKey = uiState.isTestingApiKey,
                            onDeviceStrategy = uiState.onDeviceStrategy,
                            isMcpServerEnabled = uiState.isMcpServerEnabled,
                            onToggleMcpServer = { enabled ->
                                viewModel.onMcpServerToggled(enabled, flow = "$flow -> ScanResultScreen.onToggleMcpServer")
                            },
                            agentConfigs = uiState.agentConfigs,
                            providerModelsMap = uiState.providerModelsMap,
                            onAgentProviderSelected = { agent, prov ->
                                viewModel.onAgentProviderSelected(agent, prov, flow = "$flow -> ScanResultScreen.onAgentProviderSelected")
                            },
                            onAgentModelSelected = { agent, model ->
                                viewModel.onAgentModelSelected(agent, model, flow = "$flow -> ScanResultScreen.onAgentModelSelected")
                            },
                            onProviderTabSelected = { type ->
                                viewModel.onProviderTabSelected(type, flow = "$flow -> ScanResultScreen.onProviderTabSelected")
                            },
                            onSaveProviderConfig = { type, url, key, model ->
                                viewModel.saveAndTestProviderConfig(type, url, key, model, flow = "$flow -> ScanResultScreen.saveAndTestProviderConfig")
                            },
                            onSelectOnDeviceStrategy = { strat ->
                                viewModel.onSelectOnDeviceStrategy(strat, flow = "$flow -> ScanResultScreen.onSelectOnDeviceStrategy")
                            },
                            themeMode = uiState.themeMode,
                            onSelectThemeMode = { mode ->
                                viewModel.updateThemeMode(mode, flow = "$flow -> ScanResultScreen.onSelectThemeMode")
                            },
                            onViewTelemetry = {
                                traceViewModel.loadAgentTraces(flow = "$flow -> ScanResultScreen.onViewTelemetry")
                                showSettingsScreen = false
                                showTelemetryScreen = true
                            },
                            onClose = { showSettingsScreen = false }
                        )
                    }

                    is ScanScreenMode.Telemetry -> {
                        AgentTelemetryScreen(
                            viewModel = traceViewModel,
                            onClose = { showTelemetryScreen = false }
                        )
                    }

                    is ScanScreenMode.History -> {
                        HistoryScreen(
                            historyList = uiState.historyList,
                            onClose = { showHistoryScreen = false },
                            onSelectRecord = { record ->
                                viewModel.openHistoryRecord(record, flow = "$flow -> ScanResultScreen.openHistoryRecord")
                                showHistoryScreen = false
                            },
                            onDeleteRecord = { id ->
                                viewModel.deleteHistoryRecord(id, flow = "$flow -> ScanResultScreen.onDeleteRecord")
                            },
                            onClearAllHistory = {
                                viewModel.clearAllHistory(flow = "$flow -> ScanResultScreen.onClearAllHistory")
                            }
                        )
                    }

                    is ScanScreenMode.Empty -> {
                        EmptyStateView(
                            onOpenSettings = { showSettingsScreen = true },
                            onTakePhoto = { imagePicker.launchCamera() },
                            onChooseGallery = { imagePicker.launchGallery() },
                            onHistoryClick = {
                                viewModel.loadHistoryList(flow = "$flow -> ScanResultScreen.onHistoryClick")
                                showHistoryScreen = true
                            }
                        )
                    }

                    is ScanScreenMode.ImageSelected -> {
                        ImagePreviewAndProcessView(
                            imageByteArray = mode.byteArray,
                            errorMessage = uiState.errorMessage,
                            agentConfigs = uiState.agentConfigs,
                            onOpenSettings = { showSettingsScreen = true },
                            onZoom = { showViewer = true },
                            onAnalyze = {
                                viewModel.processSelectedImage(flow = "$flow -> ScanResultScreen.onProcessClick")
                            },
                            onRetake = { imagePicker.launchCamera() },
                            onChange = { imagePicker.launchGallery() },
                            onClear = {
                                viewModel.clearSelectedImage(flow = "$flow -> ScanResultScreen.onClearClick")
                            }
                        )
                    }

                    is ScanScreenMode.Loading -> {
                        LoadingView(
                            imageByteArray = uiState.capturedImageByteArray,
                            activeStep = uiState.loadingStep,
                            statusText = uiState.loadingStatusText,
                            agentProgressPercent = uiState.activeAgentProgressPercent,
                            onStopAnalysis = {
                                viewModel.stopAnalysis(flow = "$flow -> ScanResultScreen.onStopAnalysis")
                            }
                        )
                    }

                    is ScanScreenMode.Results -> {
                        MedicineListView(
                            imageByteArray = uiState.capturedImageByteArray,
                            medicines = uiState.medicines,
                            isSuspendedForApproval = uiState.isSuspendedForApproval,
                            approvalReason = uiState.approvalReason,
                            selectedHistoryItem = uiState.selectedHistoryItem,
                            onZoom = { showViewer = true },
                            onCopySummary = {
                                val textSummary = viewModel.copyToClipboard(flow = "$flow -> ScanResultScreen.onCopyClick")
                                if (textSummary.isNotEmpty()) {
                                    clipboardManager.setText(AnnotatedString(textSummary))
                                }
                            },
                            onRescan = {
                                viewModel.clearResults(flow = "$flow -> ScanResultScreen.onRescanClick")
                            },
                            onApproveAndConfirm = {
                                viewModel.approveAndResumeAnalysis(flow = "$flow -> ScanResultScreen.onApproveAndConfirm")
                            },
                            onHistoryClick = {
                                viewModel.loadHistoryList(flow = "$flow -> ScanResultScreen.onHistoryClick")
                                showHistoryScreen = true
                            },
                            onBackFromHistory = {
                                viewModel.clearSelectedHistoryRecord(flow = "$flow -> ScanResultScreen.onBackFromHistory")
                                showHistoryScreen = true
                            }
                        )
                    }
                }
            }

            // Fullscreen Zoomable Image Viewer
            if (showViewer && uiState.capturedImageByteArray != null) {
                FullScreenImagePreviewDialog(
                    imageByteArray = uiState.capturedImageByteArray!!,
                    onDismiss = { showViewer = false }
                )
            }
        }
    }
}
