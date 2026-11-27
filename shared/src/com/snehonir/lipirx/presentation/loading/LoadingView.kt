package com.snehonir.lipirx.presentation.loading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.PipelinePhase
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.ScanMedicine
import com.snehonir.lipirx.presentation.common.LipiRxScaffold
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.presentation.loading.components.DetailsDrawer
import com.snehonir.lipirx.presentation.loading.components.FoundChips
import com.snehonir.lipirx.presentation.loading.components.ResultBanner
import com.snehonir.lipirx.presentation.loading.components.ScanHero
import com.snehonir.lipirx.presentation.loading.components.StatusHeader
import com.snehonir.lipirx.presentation.loading.components.StepperCard
import com.snehonir.lipirx.presentation.loading.components.SupervisorCard
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_continue_review
import com.snehonir.lipirx.resources.btn_keep_waiting
import com.snehonir.lipirx.resources.btn_stop
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.btn_view_results
import com.snehonir.lipirx.resources.dialog_stop_message
import com.snehonir.lipirx.resources.dialog_stop_title
import com.snehonir.lipirx.resources.hint_taking_longer
import com.snehonir.lipirx.resources.title_analyzing
import com.snehonir.lipirx.resources.title_complete
import com.snehonir.lipirx.resources.title_review_needed
import com.snehonir.lipirx.ui.BackHandler
import com.snehonir.lipirx.ui.rememberReduceMotion
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private const val FEED_VISIBLE = 5

/**
 * Layout, top to bottom:
 *  1. ScanHero        – the prescription, scanning (or checked off when done)
 *  2. StatusHeader    – the single progress bar + "2 of 4 checks"
 *  3. ResultBanner    – outcome summary, only once finished
 *  4. SupervisorCard  – who is orchestrating, its status, its current decision and who it delegated to
 *  5. StepperCard     – the four steps with a result line each
 *  6. FoundChips      – medicines as they are discovered
 *  7. DetailsDrawer   – the full play-by-play, collapsed by default
 *
 * @param reduceMotion Defaults to the platform's "reduce motion" / zero-animation setting. When true the
 *   scanner, supervisor avatar, agent icons and indeterminate bars stop moving.
 */
@Composable
fun LoadingView(
    imageByteArray: ByteArray? = null,
    progress: PipelineProgress = PipelineProgress(),
    isLoading: Boolean = true,
    reduceMotion: Boolean = rememberReduceMotion(),
    onStopAnalysis: () -> Unit = {},
    onViewResults: () -> Unit = {},
) {
    val primaryColor = Lipi.Primary
    val haptic = LocalHapticFeedback.current

    val isComplete =
        !isLoading && (progress.phase == PipelinePhase.FINALIZING || progress.phase == PipelinePhase.WAITING_FOR_PHARMACIST)
    val isSuspended = isComplete && progress.phase == PipelinePhase.WAITING_FOR_PHARMACIST

    val animatedOverall by animateFloatAsState(
        targetValue = progress.overallPercent / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overall",
    )

    val infinite = rememberInfiniteTransition(label = "loading")
    val animatedScan = infinite.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "scanY",
    )
    val animatedPulse = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )
    val frozenScan = remember { mutableStateOf(0.5f) }
    val frozenPulse = remember { mutableStateOf(0f) }
    val scan: State<Float> = if (reduceMotion) frozenScan else animatedScan
    val pulse: State<Float> = if (reduceMotion) frozenPulse else animatedPulse

    // Haptic tick when a required check completes
    LaunchedEffect(progress.completedChecks) {
        if (progress.completedChecks > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    // A firmer tap when the whole analysis is finished
    LaunchedEffect(isComplete) {
        if (isComplete) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Reassurance if nothing new has happened for a while
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(progress.feed.size, isLoading) {
        slow = false
        if (isLoading) {
            delay(25_000.milliseconds)
            slow = true
        }
    }

    // Stop confirmation (button, top-bar close, and system Back)
    var confirmStop by remember { mutableStateOf(false) }
    BackHandler {
        if (isComplete) {
            onViewResults()
        } else {
            confirmStop = true
        }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(Res.string.dialog_stop_title)) },
            text = { Text(stringResource(Res.string.dialog_stop_message)) },
            confirmButton = {
                TextButton(onClick = { confirmStop = false; onStopAnalysis() }) {
                    Text(stringResource(Res.string.btn_stop), color = Lipi.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) {
                    Text(stringResource(Res.string.btn_keep_waiting))
                }
            },
        )
    }

    LipiRxScaffold(
        showTopBar = false
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            radius = size.width * 0.95f,
                        )
                    )
                },
        ) {
            LoadingTopBar(
                isComplete = isComplete,
                isSuspended = isSuspended,
                onClose = { confirmStop = true },
            )

            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val minHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .heightIn(min = minHeight - 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    ScanHero(
                        imageByteArray = imageByteArray,
                        scan = scan,
                        pulse = pulse,
                        isScanning = !isComplete,
                    )
                    StatusHeader(
                        fraction = animatedOverall,
                        completedChecks = progress.completedChecks,
                        totalChecks = PipelineProgress.WORKERS.size,
                    )
                    ResultBanner(
                        visible = isComplete,
                        progress = progress,
                        isSuspended = isSuspended,
                    )
                    SupervisorCard(progress, reduceMotion = reduceMotion)
                    StepperCard(progress, reduceMotion = reduceMotion)
                    FoundChips(
                        medicines = progress.medicines,
                        flaggedNames = progress.flaggedMedicineNames,
                    )
                    DetailsDrawer(progress.feed.takeLast(FEED_VISIBLE))

                    AnimatedVisibility(visible = slow && isLoading) {
                        Text(
                            text = stringResource(Res.string.hint_taking_longer),
                            fontSize = 12.sp,
                            color = Lipi.Muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (isComplete) {
                PrimaryButton(
                    text = stringResource(
                        if (isSuspended) Res.string.btn_continue_review else Res.string.btn_view_results
                    ),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onViewResults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                TextButton(
                    onClick = { confirmStop = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.btn_stop_analysis),
                        color = Lipi.Error,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingTopBar(
    isComplete: Boolean,
    isSuspended: Boolean,
    onClose: () -> Unit,
) {
    val title = stringResource(
        when {
            isSuspended -> Res.string.title_review_needed
            isComplete -> Res.string.title_complete
            else -> Res.string.title_analyzing
        }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(
            targetState = title,
            animationSpec = tween(300),
            label = "title",
            modifier = Modifier.weight(1f),
        ) { text ->
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
            )
        }
        AnimatedVisibility(visible = !isComplete) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.btn_stop_analysis),
                    tint = Lipi.Muted,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------------------------

private fun previewProgress(vararg events: PipelineEvent): PipelineProgress =
    events.fold(PipelineProgress()) { acc, e -> acc.reduce(e) }

private val previewMedicines = listOf(
    ScanMedicine("Napa Extra"),
    ScanMedicine("Seclo"),
    ScanMedicine("Monas"),
    ScanMedicine("Ace Plus"),
    ScanMedicine("Fexo"),
    ScanMedicine("Maxpro"),
)

private val previewMedicinesWithStrength = listOf(
    ScanMedicine("Napa Extra", "500 mg"),
    ScanMedicine("Seclo", "20 mg"),
    ScanMedicine("Monas", "10 mg"),
    ScanMedicine("Ace Plus", "500 mg"),
    ScanMedicine("Fexo", "120 mg"),
    ScanMedicine("Maxpro", "20 mg"),
)

private fun readAndParse(): Array<PipelineEvent> = arrayOf(
    PipelineEvent.Dispatched(listOf(RxAgentType.TRANSCRIPTION), "Image first, so the rest has text to work with."),
    PipelineEvent.WorkerFinished(
        RxAgentType.TRANSCRIPTION, true, 6, medicines = previewMedicines
    ),
    PipelineEvent.Dispatched(listOf(RxAgentType.ENTITY_PARSER), "Normalize names and strengths."),
    PipelineEvent.WorkerFinished(
        RxAgentType.ENTITY_PARSER, true, 6, medicines = previewMedicinesWithStrength
    ),
)

@Preview(name = "Start")
@Composable
fun LoadingViewStartPreview() {
    LipiRxTheme {
        LoadingView(progress = previewProgress(PipelineEvent.Planning("Looking at the photo first.")))
    }
}

@Preview(name = "Reading")
@Composable
fun LoadingViewReadingPreview() {
    LipiRxTheme {
        LoadingView(
            progress = previewProgress(
                PipelineEvent.Dispatched(listOf(RxAgentType.TRANSCRIPTION), "Image first."),
                PipelineEvent.WorkerPercent(RxAgentType.TRANSCRIPTION, 55),
            )
        )
    }
}

@Preview(name = "Parallel")
@Composable
fun LoadingViewParallelPreview() {
    LipiRxTheme {
        LoadingView(
            progress = previewProgress(
                *readAndParse(),
                PipelineEvent.Dispatched(
                    listOf(RxAgentType.GENERIC_SUBSTITUTE, RxAgentType.SAFETY_AUDITOR),
                    "Substitutes and the audit are independent, so I'm running them together."
                ),
            )
        )
    }
}

@Preview(name = "Rework")
@Composable
fun LoadingViewReworkPreview() {
    LipiRxTheme {
        LoadingView(
            progress = previewProgress(
                *readAndParse(),
                PipelineEvent.Dispatched(listOf(RxAgentType.SAFETY_AUDITOR), "Audit."),
                PipelineEvent.WorkerFinished(
                    RxAgentType.SAFETY_AUDITOR,
                    true,
                    6,
                    listOf("Napa Extra")
                ),
                PipelineEvent.Dispatched(
                    listOf(RxAgentType.ENTITY_PARSER),
                    "One item looks wrong. Sending it back."
                ),
            )
        )
    }
}

@Preview(name = "Complete")
@Composable
fun LoadingViewCompletePreview() {
    LipiRxTheme {
        LoadingView(
            isLoading = false,
            progress = previewProgress(
                *readAndParse(),
                PipelineEvent.Dispatched(
                    listOf(RxAgentType.GENERIC_SUBSTITUTE, RxAgentType.SAFETY_AUDITOR),
                    "Running both together."
                ),
                PipelineEvent.WorkerFinished(RxAgentType.GENERIC_SUBSTITUTE, true, 6, substituteCount = 4),
                PipelineEvent.WorkerFinished(RxAgentType.SAFETY_AUDITOR, true, 6),
                PipelineEvent.Finalizing("All checks passed."),
            )
        )
    }
}

@Preview(name = "Needs pharmacist")
@Composable
fun LoadingViewPharmacistPreview() {
    LipiRxTheme {
        LoadingView(
            isLoading = false,
            progress = previewProgress(
                *readAndParse(),
                PipelineEvent.Dispatched(
                    listOf(RxAgentType.GENERIC_SUBSTITUTE, RxAgentType.SAFETY_AUDITOR),
                    "Running both together."
                ),
                PipelineEvent.WorkerFinished(RxAgentType.GENERIC_SUBSTITUTE, true, 6, substituteCount = 4),
                PipelineEvent.WorkerFinished(
                    RxAgentType.SAFETY_AUDITOR, true, 6, listOf("Napa Extra", "Ace Plus")
                ),
                PipelineEvent.Suspended("Two items need a pharmacist before I can continue."),
            )
        )
    }
}