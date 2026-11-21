package com.snehonir.lipirx

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.navigation.AppNavKey
import com.snehonir.lipirx.navigation.rememberAppNavigator
import com.snehonir.lipirx.presentation.history.HistoryScreenWrapper
import com.snehonir.lipirx.presentation.home.HomeScreen
import com.snehonir.lipirx.presentation.loading.AnalysisLoadingScreen
import com.snehonir.lipirx.presentation.preview.ImagePreviewScreen
import com.snehonir.lipirx.presentation.results.ScanResultsScreen
import com.snehonir.lipirx.presentation.common.FullScreenImagePreviewDialog
import com.snehonir.lipirx.presentation.settings.SettingsScreen
import com.snehonir.lipirx.presentation.settings.SettingsViewModel
import com.snehonir.lipirx.presentation.telemetry.AgentTelemetryScreen
import com.snehonir.lipirx.presentation.telemetry.AgentTraceViewModel
import com.snehonir.lipirx.presentation.update.UpdateDialog
import com.snehonir.lipirx.ui.BackHandler
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject

private const val TAG = "LipiRxApp"

@Composable
@Preview
fun App() {
    val settingsViewModel: SettingsViewModel = koinInject()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    LipiRxTheme(themeMode = settingsUiState.themeMode) {
        LipiRxAppContent(settingsViewModel = settingsViewModel)
    }
}

@Composable
fun LipiRxAppContent(
    settingsViewModel: SettingsViewModel = koinInject(),
    flow: String = "App -> LipiRxAppContent"
) {
    Napier.d("[$flow] Rendering Navigation 3 LipiRxAppContent", tag = TAG)

    val traceViewModel: AgentTraceViewModel = koinInject()
    val updateManager: UpdateManager = koinInject()
    val sessionState: PrescriptionSessionState = koinInject()
    val sessionData by sessionState.session.collectAsState()
    val navigator = rememberAppNavigator(startDestination = AppNavKey.Home)

    LaunchedEffect(Unit) {
        updateManager.checkForUpdates(silent = true)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateManager.onPermissionResultCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(sessionData.isLoading, sessionData.errorMessage) {
        if (navigator.currentKey == AppNavKey.AnalysisLoading && !sessionData.isLoading) {
            if (sessionData.errorMessage != null) {
                navigator.replaceTop(AppNavKey.ImagePreview)
            } else {
                navigator.replaceTop(AppNavKey.Results)
            }
        }
    }

    var viewerImageBytes by rememberSaveable { mutableStateOf<ByteArray?>(null) }

    // Navigation 3 Back Stack Handler
    BackHandler(enabled = navigator.backStack.size > 1) {
        if (navigator.currentKey == AppNavKey.AnalysisLoading) {
            sessionState.cancelActiveAnalysis()
        }
        if (navigator.backStack.size == 2 && navigator.currentKey != AppNavKey.History && navigator.currentKey != AppNavKey.Settings) {
            sessionState.resetSession()
        }
        navigator.goBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
    ) {
        AnimatedContent(
            targetState = navigator.currentKey,
            contentKey = { it::class },
            transitionSpec = {
                (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 40 }) togetherWith fadeOut(tween(160))
            },
            label = "nav3State",
        ) { key ->
            when (key) {
                is AppNavKey.Home -> {
                    HomeScreen(
                        onOpenSettings = { navigator.goTo(AppNavKey.Settings) },
                        onHistoryClick = { navigator.goTo(AppNavKey.History) },
                        onImageCaptured = { navigator.goTo(AppNavKey.ImagePreview) }
                    )
                }

                is AppNavKey.ImagePreview -> {
                    ImagePreviewScreen(
                        onOpenSettings = { navigator.goTo(AppNavKey.Settings) },
                        onZoom = { viewerImageBytes = sessionData.imageByteArray },
                        onStartAnalysis = { navigator.replaceTop(AppNavKey.AnalysisLoading) },
                        onClear = { navigator.popToHome() }
                    )
                }

                is AppNavKey.AnalysisLoading -> {
                    AnalysisLoadingScreen(
                        onCancel = { navigator.popToHome() }
                    )
                }

                is AppNavKey.Results -> {
                    ScanResultsScreen(
                        onZoom = { viewerImageBytes = sessionData.imageByteArray },
                        onDone = { navigator.popToHome() },
                        onHistoryClick = { navigator.goTo(AppNavKey.History) },
                        onBackFromHistory = { navigator.goBack() }
                    )
                }

                is AppNavKey.History -> {
                    HistoryScreenWrapper(
                        onClose = { navigator.goBack() },
                        onSelectRecord = { navigator.goTo(AppNavKey.Results) }
                    )
                }

                is AppNavKey.Telemetry -> {
                    AgentTelemetryScreen(
                        viewModel = traceViewModel,
                        onClose = { navigator.goBack() }
                    )
                }

                is AppNavKey.Settings -> {
                    val settingsUiState by settingsViewModel.uiState.collectAsState()
                    SettingsScreen(
                        selectedModel = settingsUiState.selectedModel,
                        apiKey = settingsUiState.apiKey,
                        geminiApiKey = settingsUiState.geminiApiKey,
                        groqApiKey = settingsUiState.groqApiKey,
                        openaiApiKey = settingsUiState.openaiApiKey,
                        claudeApiKey = settingsUiState.claudeApiKey,
                        customApiKey = settingsUiState.customApiKey,
                        providerType = settingsUiState.providerType,
                        customUrl = settingsUiState.customUrl,
                        apiKeyError = settingsUiState.apiKeyError,
                        apiKeySuccess = settingsUiState.apiKeySuccess,
                        isTestingApiKey = settingsUiState.isTestingApiKey,
                        onDeviceStrategy = settingsUiState.onDeviceStrategy,
                        isMcpServerEnabled = settingsUiState.isMcpServerEnabled,
                        onToggleMcpServer = { enabled ->
                            settingsViewModel.onMcpServerToggled(enabled)
                        },
                        agentConfigs = settingsUiState.agentConfigs,
                        providerModelsMap = settingsUiState.providerModelsMap,
                        onAgentProviderSelected = { agent, prov ->
                            settingsViewModel.onAgentProviderSelected(agent, prov)
                        },
                        onAgentModelSelected = { agent, model ->
                            settingsViewModel.onAgentModelSelected(agent, model)
                        },
                        onAgentFallbackProviderSelected = { agent, prov ->
                            settingsViewModel.onAgentFallbackProviderSelected(agent, prov)
                        },
                        onAgentFallbackModelSelected = { agent, model ->
                            settingsViewModel.onAgentFallbackModelSelected(agent, model)
                        },
                        onProviderTabSelected = { type ->
                            settingsViewModel.onProviderTabSelected(type)
                        },
                        onSaveProviderConfig = { type, url, key, model ->
                            settingsViewModel.saveAndTestProviderConfig(type, url, key, model)
                        },
                        onSelectOnDeviceStrategy = { strat ->
                            settingsViewModel.onSelectOnDeviceStrategy(strat)
                        },
                        slmModelDirectory = settingsUiState.slmModelDirectory,
                        onSaveSlmDirectoryPath = { path ->
                            settingsViewModel.onSaveSlmDirectoryPath(path)
                        },
                        themeMode = settingsUiState.themeMode,
                        onSelectThemeMode = { mode ->
                            settingsViewModel.updateThemeMode(mode)
                        },
                        onViewTelemetry = { navigator.goTo(AppNavKey.Telemetry) },
                        onClose = { navigator.goBack() }
                    )
                }
            }
        }

        // Fullscreen Zoomable Image Viewer
        if (viewerImageBytes != null) {
            FullScreenImagePreviewDialog(
                imageByteArray = viewerImageBytes!!,
                onDismiss = { viewerImageBytes = null }
            )
        }

        // Self-Update Prompt Dialog
        UpdateDialog(updateManager = updateManager)
    }
}
