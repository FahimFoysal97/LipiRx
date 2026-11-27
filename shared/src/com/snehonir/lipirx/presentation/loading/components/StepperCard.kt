package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.WorkerProgress
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.chip_recheck
import com.snehonir.lipirx.resources.label_running_together
import com.snehonir.lipirx.resources.step_checking_again
import com.snehonir.lipirx.resources.step_failed
import com.snehonir.lipirx.resources.step_reading_percent
import com.snehonir.lipirx.resources.step_result_entity
import com.snehonir.lipirx.resources.step_result_flagged
import com.snehonir.lipirx.resources.step_result_generic
import com.snehonir.lipirx.resources.step_result_medicines
import com.snehonir.lipirx.resources.step_result_safe
import com.snehonir.lipirx.resources.step_running
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.AgentIcon
import org.jetbrains.compose.resources.stringResource

private val RowStartPad = 12.dp
private val RowVerticalPad = 8.dp
private val IconSize = 30.dp

/**
 * Four plain-language steps with a result line under each. Transcription -> Entity parsing is a
 * chain (joined by a connector); Generic substitutes and the Safety check run side by side inside
 * a dashed bracket because the supervisor dispatches them together.
 */
@Composable
internal fun StepperCard(
    progress: PipelineProgress,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    val transcription = progress.worker(RxAgentType.TRANSCRIPTION)
    val entity = progress.worker(RxAgentType.ENTITY_PARSER)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            StepRow(
                agent = RxAgentType.TRANSCRIPTION,
                reduceMotion = reduceMotion,
                worker = transcription,
                hasUp = false,
                hasDown = true,
                upDone = false,
            )
            StepRow(
                agent = RxAgentType.ENTITY_PARSER,
                reduceMotion = reduceMotion,
                worker = entity,
                hasUp = true,
                hasDown = true,
                upDone = transcription.completedOk,
            )
            ParallelGroup(active = progress.isParallel) {
                StepRow(
                    agent = RxAgentType.GENERIC_SUBSTITUTE,
                    reduceMotion = reduceMotion,
                    worker = progress.worker(RxAgentType.GENERIC_SUBSTITUTE),
                    hasUp = true,
                    hasDown = false,
                    upDone = entity.completedOk,
                )
                StepRow(
                    agent = RxAgentType.SAFETY_AUDITOR,
                    reduceMotion = reduceMotion,
                    worker = progress.worker(RxAgentType.SAFETY_AUDITOR),
                    hasUp = false,
                    hasDown = false,
                    upDone = false,
                )
            }
        }
    }
}

@Composable
private fun ParallelGroup(active: Boolean, content: @Composable () -> Unit) {
    val stroke = Lipi.Primary.copy(alpha = 0.5f)
    Box(
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
            },
    ) {
        Column { content() }
        // Floats over the corner instead of taking its own row, so the connector line stays intact.
        AnimatedVisibility(
            visible = active,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 10.dp),
        ) {
            Text(
                text = stringResource(Res.string.label_running_together),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Primary,
            )
        }
    }
}

@Composable
private fun StepRow(
    agent: RxAgentType,
    worker: WorkerProgress,
    hasUp: Boolean,
    hasDown: Boolean,
    upDone: Boolean,
    reduceMotion: Boolean,
) {
    val titleColor by animateColorAsState(
        targetValue = when (worker.status) {
            AgentStatus.RUNNING -> Lipi.Ink
            AgentStatus.WAITING -> Lipi.Muted.copy(alpha = 0.55f)
            else -> Lipi.Ink.copy(alpha = 0.85f)
        },
        animationSpec = tween(300),
        label = "stepTitle",
    )
    val subtitle = stepSubtitle(agent, worker)
    val subtitleColor = when (worker.status) {
        AgentStatus.FLAGGED -> LoadingWarning
        AgentStatus.FAILED -> Lipi.Error
        else -> Lipi.Muted
    }
    val doneLine = Lipi.Primary.copy(alpha = 0.45f)
    val upColor = if (upDone) doneLine else Lipi.CardBorder
    val downColor = if (worker.completedOk) doneLine else Lipi.CardBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = (RowStartPad + IconSize / 2).toPx()
                val iconTop = RowVerticalPad.toPx()
                val iconBottom = iconTop + IconSize.toPx()
                val gap = 3.dp.toPx()
                val width = 2.dp.toPx()
                if (hasUp) {
                    drawLine(upColor, Offset(x, 0f), Offset(x, iconTop - gap), width, StrokeCap.Round)
                }
                if (hasDown) {
                    drawLine(downColor, Offset(x, iconBottom + gap), Offset(x, size.height), width, StrokeCap.Round)
                }
            }
            .padding(start = RowStartPad, end = RowStartPad, top = RowVerticalPad, bottom = RowVerticalPad),
        verticalAlignment = Alignment.Top,
    ) {
        AgentIcon(agent = agent, status = worker.status, size = IconSize, reduceMotion = reduceMotion)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(rowLabelRes(agent)),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = if (worker.status == AgentStatus.RUNNING) FontWeight.Bold else FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Crossfade(targetState = subtitle, animationSpec = tween(250), label = "stepSubtitle") { text ->
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (worker.runs > 1) {
            Spacer(Modifier.width(8.dp))
            if (worker.status == AgentStatus.RUNNING) {
                StepChip(
                    text = stringResource(Res.string.chip_recheck),
                    container = Lipi.PrimaryContainer,
                    content = Lipi.OnPrimaryContainer,
                )
            } else {
                StepChip(
                    text = "×${worker.runs}",
                    container = Lipi.PrimaryContainer,
                    content = Lipi.OnPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun stepSubtitle(agent: RxAgentType, worker: WorkerProgress): String = when (worker.status) {
    AgentStatus.WAITING -> stringResource(stepDescRes(agent))
    AgentStatus.RUNNING -> {
        val percent = worker.percent
        when {
            worker.runs > 1 -> stringResource(Res.string.step_checking_again)
            percent != null -> stringResource(Res.string.step_reading_percent, percent)
            else -> stringResource(Res.string.step_running)
        }
    }
    AgentStatus.DONE -> when (agent) {
        RxAgentType.TRANSCRIPTION -> stringResource(Res.string.step_result_medicines, worker.itemCount)
        RxAgentType.ENTITY_PARSER -> stringResource(Res.string.step_result_entity)
        RxAgentType.GENERIC_SUBSTITUTE -> stringResource(Res.string.step_result_generic, worker.substituteCount)
        else -> stringResource(Res.string.step_result_safe)
    }
    AgentStatus.FLAGGED -> stringResource(Res.string.step_result_flagged, shortList(worker.flaggedNames))
    AgentStatus.FAILED -> stringResource(Res.string.step_failed)
}

@Composable
private fun StepChip(text: String, container: Color, content: Color) {
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