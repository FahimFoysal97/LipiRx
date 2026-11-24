package com.snehonir.lipirx.presentation.loading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.FeedEntry
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.PipelinePhase
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.WorkerProgress
import com.snehonir.lipirx.presentation.common.PaperIllustration
import com.snehonir.lipirx.presentation.common.SecondaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_keep_waiting
import com.snehonir.lipirx.resources.btn_stop
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.cd_hide_details
import com.snehonir.lipirx.resources.cd_show_details
import com.snehonir.lipirx.resources.chip_flagged
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.dialog_stop_message
import com.snehonir.lipirx.resources.dialog_stop_title
import com.snehonir.lipirx.resources.feed_double_check
import com.snehonir.lipirx.resources.feed_entity_done
import com.snehonir.lipirx.resources.feed_finalizing
import com.snehonir.lipirx.resources.feed_found_medicines
import com.snehonir.lipirx.resources.feed_generic_done
import com.snehonir.lipirx.resources.feed_preparing
import com.snehonir.lipirx.resources.feed_safety_done
import com.snehonir.lipirx.resources.feed_safety_failed
import com.snehonir.lipirx.resources.feed_safety_flagged
import com.snehonir.lipirx.resources.feed_start_entity
import com.snehonir.lipirx.resources.feed_start_generic
import com.snehonir.lipirx.resources.feed_start_safety
import com.snehonir.lipirx.resources.feed_start_transcription
import com.snehonir.lipirx.resources.feed_step_failed
import com.snehonir.lipirx.resources.feed_waiting_pharmacist
import com.snehonir.lipirx.resources.hint_taking_longer
import com.snehonir.lipirx.resources.label_analysis_progress
import com.snehonir.lipirx.resources.label_checks_count
import com.snehonir.lipirx.resources.label_required_checks
import com.snehonir.lipirx.resources.label_running_together
import com.snehonir.lipirx.resources.manager_title
import com.snehonir.lipirx.resources.phase_finalizing
import com.snehonir.lipirx.resources.phase_planning
import com.snehonir.lipirx.resources.phase_running
import com.snehonir.lipirx.resources.phase_waiting_pharmacist
import com.snehonir.lipirx.resources.row_entity
import com.snehonir.lipirx.resources.row_generic
import com.snehonir.lipirx.resources.row_safety
import com.snehonir.lipirx.resources.row_transcription
import com.snehonir.lipirx.ui.BackHandler
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/** Swap for a theme token if Lipi has a warning color. */
private val WarningColor = Color(0xFFB26A00)
private const val FEED_VISIBLE = 5
private const val MAX_NAMES_SHOWN = 3

@Composable
fun LoadingView(
    imageByteArray: ByteArray? = null,
    progress: PipelineProgress = PipelineProgress(),
    onStopAnalysis: () -> Unit = {},
) {
    val primaryColor = Lipi.Primary
    val haptic = LocalHapticFeedback.current

    val animatedOverall by animateFloatAsState(
        targetValue = progress.overallPercent / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overall",
    )

    val infinite = rememberInfiniteTransition(label = "loading")
    val scan = infinite.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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
    LaunchedEffect(progress.feed.size) {
        slow = false
        delay(25_000.milliseconds)
        slow = true
    }

    // Stop confirmation (button + system Back)
    var confirmStop by remember { mutableStateOf(false) }
    BackHandler { confirmStop = true }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.95f,
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
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

                AnimatedVisibility(visible = slow) {
                    Text(
                        text = stringResource(Res.string.hint_taking_longer),
                        fontSize = 12.sp,
                        color = Lipi.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

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
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = Lipi.Primary,
            trackColor = Lipi.PrimaryContainer,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun ManagerCard(progress: PipelineProgress) {
    val runningNames = progress.running.map { stringResource(rowLabelRes(it)) }
    val phaseText = when (progress.phase) {
        PipelinePhase.PLANNING -> stringResource(Res.string.phase_planning)
        PipelinePhase.RUNNING -> stringResource(Res.string.phase_running, runningNames.joinToString(", "))
        PipelinePhase.FINALIZING -> stringResource(Res.string.phase_finalizing)
        PipelinePhase.WAITING_FOR_PHARMACIST -> stringResource(Res.string.phase_waiting_pharmacist)
    }
    val checks = PipelineProgress.CHECKS.size
    val animatedChecks by animateFloatAsState(
        targetValue = progress.completedChecks / checks.toFloat(),
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "checks",
    )

    val hasReasoning = progress.reasoning.isNotBlank()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "chevron",
    )
    val toggleLabel = stringResource(
        if (expanded) Res.string.cd_hide_details else Res.string.cd_show_details
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.PrimaryContainer,
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (hasReasoning) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClickLabel = toggleLabel,
                        ) { expanded = !expanded }
                    } else {
                        Modifier
                    }
                )
                .animateContentSize(tween(250, easing = FastOutSlowInEasing))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.manager_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.OnPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                if (hasReasoning) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Lipi.OnPrimaryContainer,
                        modifier = Modifier.size(20.dp).rotate(chevronRotation),
                    )
                }
            }
            Crossfade(
                targetState = phaseText,
                animationSpec = tween(300),
                label = "phase",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) { text ->
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink,
                )
            }
            if (hasReasoning) {
                Text(
                    text = progress.reasoning,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Lipi.Muted,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (progress.phase == PipelinePhase.PLANNING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                    color = Lipi.Primary,
                    trackColor = Lipi.Surface,
                    strokeCap = StrokeCap.Round,
                )
            }
            Row {
                Text(
                    text = stringResource(Res.string.label_required_checks),
                    fontSize = 12.sp,
                    color = Lipi.OnPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(Res.string.label_checks_count, progress.completedChecks, checks),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.OnPrimaryContainer,
                )
            }
            LinearProgressIndicator(
                progress = { animatedChecks },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = Lipi.Primary,
                trackColor = Lipi.Surface,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WorkersCard(progress: PipelineProgress) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            WorkerRow(RxAgentType.TRANSCRIPTION, progress.worker(RxAgentType.TRANSCRIPTION))
            WorkerRow(RxAgentType.ENTITY_PARSER, progress.worker(RxAgentType.ENTITY_PARSER))
            ParallelGroup(active = progress.isParallel) {
                WorkerRow(RxAgentType.GENERIC_SUBSTITUTE, progress.worker(RxAgentType.GENERIC_SUBSTITUTE))
                WorkerRow(RxAgentType.SAFETY_AUDITOR, progress.worker(RxAgentType.SAFETY_AUDITOR))
            }
        }
    }
}

@Composable
private fun ParallelGroup(active: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val stroke = Lipi.Primary.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (active) {
                    drawRoundRect(
                        color = stroke,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                        ),
                    )
                }
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        AnimatedVisibility(visible = active) {
            Text(
                text = stringResource(Res.string.label_running_together),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Primary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        content()
    }
}

@Composable
private fun WorkerRow(agent: RxAgentType, worker: WorkerProgress) {
    val textColor by animateColorAsState(
        targetValue = when (worker.status) {
            AgentStatus.RUNNING -> Lipi.Ink
            AgentStatus.WAITING -> Lipi.Muted.copy(alpha = 0.55f)
            else -> Lipi.Muted
        },
        animationSpec = tween(300),
        label = "rowColor",
    )
    val animatedPercent by animateIntAsState(
        targetValue = worker.percent ?: 0,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "rowPercent",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIcon(worker.status)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(rowLabelRes(agent)),
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = textColor,
            fontWeight = if (worker.status == AgentStatus.RUNNING) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (worker.runs > 1) {
            Spacer(Modifier.width(8.dp))
            Chip(text = "×${worker.runs}")
        }
        if (worker.status == AgentStatus.FLAGGED) {
            Spacer(Modifier.width(8.dp))
            Chip(
                text = stringResource(Res.string.chip_flagged),
                container = WarningColor.copy(alpha = 0.15f),
                content = WarningColor,
            )
        }
        if (worker.status == AgentStatus.RUNNING && worker.percent != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$animatedPercent%",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Lipi.Primary,
            )
        }
    }
}

@Composable
private fun Chip(
    text: String,
    container: Color = Lipi.PrimaryContainer,
    content: Color = Lipi.OnPrimaryContainer,
) {
    Surface(shape = RoundedCornerShape(8.dp), color = container) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun StatusIcon(status: AgentStatus) {
    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
        when (status) {
            AgentStatus.DONE -> Box(
                Modifier.fillMaxSize().clip(CircleShape).background(Lipi.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            AgentStatus.RUNNING -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.5.dp,
                color = Lipi.Primary,
            )
            AgentStatus.WAITING -> Box(
                Modifier.size(16.dp).border(1.5.dp, Lipi.CardBorder, CircleShape)
            )
            AgentStatus.FLAGGED -> Icon(Icons.Default.Warning, null, tint = WarningColor, modifier = Modifier.size(20.dp))
            AgentStatus.FAILED -> Icon(Icons.Default.Warning, null, tint = Lipi.Error, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FeedCard(entries: List<FeedEntry>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                val latest = index == entries.lastIndex
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (latest) Lipi.Primary else Lipi.CardBorder)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = feedText(entry),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (latest) Lipi.Ink else Lipi.Muted,
                        fontWeight = if (latest) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun feedText(entry: FeedEntry): String = when (entry) {
    FeedEntry.Preparing -> stringResource(Res.string.feed_preparing)
    FeedEntry.Finalizing -> stringResource(Res.string.feed_finalizing)
    FeedEntry.WaitingForPharmacist -> stringResource(Res.string.feed_waiting_pharmacist)
    is FeedEntry.Started ->
        if (entry.rerun) {
            stringResource(Res.string.feed_double_check, stringResource(rowLabelRes(entry.agent)))
        } else {
            stringResource(startLabelRes(entry.agent))
        }
    is FeedEntry.Finished -> when {
        !entry.success && entry.agent == RxAgentType.SAFETY_AUDITOR ->
            stringResource(Res.string.feed_safety_failed)
        !entry.success ->
            stringResource(Res.string.feed_step_failed, stringResource(rowLabelRes(entry.agent)))
        entry.agent == RxAgentType.TRANSCRIPTION ->
            stringResource(Res.string.feed_found_medicines, entry.itemCount)
        entry.agent == RxAgentType.ENTITY_PARSER ->
            stringResource(Res.string.feed_entity_done)
        entry.agent == RxAgentType.GENERIC_SUBSTITUTE ->
            stringResource(Res.string.feed_generic_done)
        entry.flaggedNames.isNotEmpty() ->
            stringResource(Res.string.feed_safety_flagged, shortList(entry.flaggedNames))
        else -> stringResource(Res.string.feed_safety_done)
    }
}

private fun shortList(names: List<String>): String {
    val shown = names.take(MAX_NAMES_SHOWN).joinToString(", ")
    val extra = names.size - MAX_NAMES_SHOWN
    return if (extra > 0) "$shown +$extra" else shown
}

private fun rowLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.row_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.row_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.row_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.row_safety
    else -> Res.string.manager_title
}

private fun startLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.feed_start_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.feed_start_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.feed_start_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.feed_start_safety
    else -> Res.string.feed_preparing
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

                drawLine(primaryColor, Offset(inset, inset), Offset(inset + len, inset), sw, StrokeCap.Round)
                drawLine(primaryColor, Offset(inset, inset), Offset(inset, inset + len), sw, StrokeCap.Round)
                drawLine(primaryColor, Offset(r, inset), Offset(r - len, inset), sw, StrokeCap.Round)
                drawLine(primaryColor, Offset(r, inset), Offset(r, inset + len), sw, StrokeCap.Round)
                drawLine(primaryColor, Offset(inset, b), Offset(inset + len, b), sw, StrokeCap.Round)
                drawLine(primaryColor, Offset(inset, b), Offset(inset, b - len), sw, StrokeCap.Round)
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
        LoadingView(progress = previewProgress(PipelineEvent.Planning))
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
                PipelineEvent.WorkerFinished(RxAgentType.SAFETY_AUDITOR, true, 6, listOf("Napa Extra")),
                PipelineEvent.Dispatched(listOf(RxAgentType.ENTITY_PARSER), "One item looks wrong. Sending it back."),
            )
        )
    }
}