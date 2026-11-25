package com.snehonir.lipirx.presentation.loading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.PipelinePhase
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.presentation.common.LipiRxScaffold
import com.snehonir.lipirx.presentation.common.PaperIllustration
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.presentation.common.SecondaryButton
import com.snehonir.lipirx.presentation.loading.components.FeedCard
import com.snehonir.lipirx.presentation.loading.components.ManagerCard
import com.snehonir.lipirx.presentation.loading.components.WorkersCard
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_keep_waiting
import com.snehonir.lipirx.resources.btn_stop
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.btn_view_results
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.dialog_stop_message
import com.snehonir.lipirx.resources.dialog_stop_title
import com.snehonir.lipirx.resources.hint_taking_longer
import com.snehonir.lipirx.resources.label_analysis_progress
import com.snehonir.lipirx.ui.BackHandler
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

private const val FEED_VISIBLE = 5

@Composable
fun LoadingView(
    imageByteArray: ByteArray? = null,
    progress: PipelineProgress = PipelineProgress(),
    isLoading: Boolean = true,
    onStopAnalysis: () -> Unit = {},
    onViewResults: () -> Unit = {},
) {
    val primaryColor = Lipi.Primary
    val haptic = LocalHapticFeedback.current

    val isComplete =
        !isLoading && (progress.phase == PipelinePhase.FINALIZING || progress.phase == PipelinePhase.WAITING_FOR_PHARMACIST)

    val animatedOverall by animateFloatAsState(
        targetValue = progress.overallPercent / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overall",
    )

    val infinite = rememberInfiniteTransition(label = "loading")
    val scan = infinite.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "scanY",
    )
    val pulse = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse",
    )

    // Haptic tick when a required check completes
    LaunchedEffect(progress.completedChecks) {
        if (progress.completedChecks > 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

    // Stop confirmation (button + system Back)
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .heightIn(min = minHeight - 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    SegScannerCard(imageByteArray = imageByteArray, scan = scan, pulse = pulse)
                    OverallHeader(animatedOverall)
                    ManagerCard(progress)
                    WorkersCard(progress)
                    FeedCard(progress.feed.takeLast(FEED_VISIBLE))

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
                    text = stringResource(Res.string.btn_view_results),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onViewResults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            } else {
                SecondaryButton(
                    text = stringResource(Res.string.btn_stop_analysis),
                    icon = Icons.Default.Close,
                    onClick = { confirmStop = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    height = 52.dp,
                    contentColor = Lipi.Error,
                    fontSize = 15,
                )
            }
        }
    }
}

@Composable
private fun OverallHeader(fraction: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(Res.string.label_analysis_progress),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(fraction * 100).toInt()}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Lipi.Primary,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = Lipi.Primary,
            trackColor = Lipi.PrimaryContainer,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SegScannerCard(
    imageByteArray: ByteArray?,
    scan: State<Float>,
    pulse: State<Float>,
) {
    val primaryColor = Lipi.Primary
    val shape = RoundedCornerShape(22.dp)

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(230.dp, 260.dp)) {
            val strength = 0.14f + 0.10f * (1f - pulse.value)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = strength), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.62f,
                )
            )
        }

        Box(
            modifier = Modifier
                .size(200.dp, 240.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = primaryColor.copy(alpha = 0.25f),
                    spotColor = primaryColor.copy(alpha = 0.35f),
                )
                .clip(shape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (imageByteArray != null && imageByteArray.isNotEmpty()) {
                AsyncImage(
                    model = imageByteArray,
                    contentDescription = stringResource(Res.string.content_desc_prescription),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                PaperIllustration(124.dp, 166.dp)
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * scan.value
                val glow = 44.dp.toPx()

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.32f),
                            Color.Transparent,
                        ),
                        startY = y - glow,
                        endY = y + glow,
                    ),
                    topLeft = Offset(0f, y - glow),
                    size = Size(size.width, glow * 2f),
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, primaryColor, primaryColor, Color.Transparent)
                    ),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                val inset = 12.dp.toPx()
                val len = 22.dp.toPx()
                val sw = 3.dp.toPx()
                val r = size.width - inset
                val b = size.height - inset

                drawLine(
                    primaryColor,
                    Offset(inset, inset),
                    Offset(inset + len, inset),
                    sw,
                    StrokeCap.Round
                )
                drawLine(
                    primaryColor,
                    Offset(inset, inset),
                    Offset(inset, inset + len),
                    sw,
                    StrokeCap.Round
                )
                drawLine(
                    primaryColor,
                    Offset(r, inset),
                    Offset(r - len, inset),
                    sw,
                    StrokeCap.Round
                )
                drawLine(
                    primaryColor,
                    Offset(r, inset),
                    Offset(r, inset + len),
                    sw,
                    StrokeCap.Round
                )
                drawLine(
                    primaryColor,
                    Offset(inset, b),
                    Offset(inset + len, b),
                    sw,
                    StrokeCap.Round
                )
                drawLine(
                    primaryColor,
                    Offset(inset, b),
                    Offset(inset, b - len),
                    sw,
                    StrokeCap.Round
                )
                drawLine(primaryColor, Offset(r, b), Offset(r - len, b), sw, StrokeCap.Round)
                drawLine(primaryColor, Offset(r, b), Offset(r, b - len), sw, StrokeCap.Round)
            }
        }
    }
}

private fun previewProgress(vararg events: PipelineEvent): PipelineProgress =
    events.fold(PipelineProgress()) { acc, e -> acc.reduce(e) }

@Preview(name = "Start")
@Composable
fun LoadingViewStartPreview() {
    LipiRxTheme {
        LoadingView(progress = previewProgress(PipelineEvent.Planning()))
    }
}

@Preview(name = "Parallel")
@Composable
fun LoadingViewParallelPreview() {
    LipiRxTheme {
        LoadingView(
            progress = previewProgress(
                PipelineEvent.Dispatched(listOf(RxAgentType.TRANSCRIPTION), "Image first."),
                PipelineEvent.WorkerFinished(RxAgentType.TRANSCRIPTION, true, 6),
                PipelineEvent.Dispatched(listOf(RxAgentType.ENTITY_PARSER), "Normalize the rows."),
                PipelineEvent.WorkerFinished(RxAgentType.ENTITY_PARSER, true, 6),
                PipelineEvent.Dispatched(
                    listOf(RxAgentType.GENERIC_SUBSTITUTE, RxAgentType.SAFETY_AUDITOR),
                    "Substitutes and audit are independent."
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
                PipelineEvent.Dispatched(listOf(RxAgentType.ENTITY_PARSER), "Normalize."),
                PipelineEvent.WorkerFinished(RxAgentType.ENTITY_PARSER, true, 6),
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
            progress = previewProgress(PipelineEvent.Finalizing())
        )
    }
}