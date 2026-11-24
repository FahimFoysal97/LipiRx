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
import com.snehonir.lipirx.domain.agent.PipelinePhase
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.navigation.AppNavKey
import com.snehonir.lipirx.navigation.rememberAppNavigator
import com.snehonir.lipirx.presentation.home.HomeScreen
import com.snehonir.lipirx.presentation.loading.AnalysisLoadingScreen
import com.snehonir.lipirx.presentation.preview.ImagePreviewScreen
import com.snehonir.lipirx.presentation.results.ScanResultsScreen
import com.snehonir.lipirx.presentation.common.FullScreenImagePreviewDialog
import com.snehonir.lipirx.presentation.history.HistoryScreen
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
fun App(
    initialNavKey: AppNavKey = AppNavKey.Home
) {
    val settingsViewModel: SettingsViewModel = koinInject()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    LipiRxTheme(themeMode = settingsUiState.themeMode) {
        LipiRxAppContent(initialNavKey = initialNavKey)
    }
}

@Composable
fun LipiRxAppContent(
    initialNavKey: AppNavKey = AppNavKey.Home,
    flow: String = "App -> LipiRxAppContent"
) {
    Napier.d("[$flow] Rendering Navigation 3 LipiRxAppContent with startDestination=$initialNavKey", tag = TAG)

    val traceViewModel: AgentTraceViewModel = koinInject()
    val updateManager: UpdateManager = koinInject()
    val sessionState: PrescriptionSessionState = koinInject()
    val sessionData by sessionState.session.collectAsState()
    val navigator = rememberAppNavigator(startDestination = initialNavKey)

    LaunchedEffect(Unit) {
        updateManager.checkForUpdates(silent = true)
    }

    LaunchedEffect(initialNavKey) {
        if (initialNavKey != AppNavKey.Home) {
            navigator.goTo(initialNavKey)
        }
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

    LaunchedEffect(sessionData.isLoading, sessionData.medicines.size, sessionData.errorMessage, navigator.currentKey) {
        if (navigator.currentKey == AppNavKey.AnalysisLoading) {
            if (sessionData.errorMessage != null) {
                navigator.replaceTop(AppNavKey.ImagePreview)
            } else if (!sessionData.isLoading && sessionData.progress.phase != PipelinePhase.PLANNING) {
                navigator.replaceTop(AppNavKey.Results)
            }
        }
    }

    var viewerImageBytes by rememberSaveable { mutableStateOf<ByteArray?>(null) }

    // Navigation 3 Back Stack Handler
    BackHandler(enabled = navigator.backStack.size > 1) {
        if (navigator.currentKey == AppNavKey.AnalysisLoading) {
            sessionState.cancelActiveAnalysis()
            navigator.replaceTop(AppNavKey.ImagePreview)
            return@BackHandler
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
                        onCancel = {
                            sessionState.cancelActiveAnalysis()
                            navigator.replaceTop(AppNavKey.ImagePreview)
                        }
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
                    HistoryScreen(
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
                    SettingsScreen(
                        onClose = { navigator.goBack() },
                        onViewTelemetry = { navigator.goTo(AppNavKey.Telemetry) }
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
