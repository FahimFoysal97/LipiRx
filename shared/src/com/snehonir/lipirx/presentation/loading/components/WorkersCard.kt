package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.snehonir.lipirx.resources.chip_flagged
import com.snehonir.lipirx.resources.label_running_together
import com.snehonir.lipirx.resources.manager_title
import com.snehonir.lipirx.resources.row_entity
import com.snehonir.lipirx.resources.row_generic
import com.snehonir.lipirx.resources.row_safety
import com.snehonir.lipirx.resources.row_transcription
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val WarningColor = Color(0xFFB26A00)

@Composable
internal fun WorkersCard(progress: PipelineProgress) {
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
                WorkerRow(
                    RxAgentType.GENERIC_SUBSTITUTE,
                    progress.worker(RxAgentType.GENERIC_SUBSTITUTE)
                )
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
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Lipi.Primary),
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
                Modifier
                    .size(16.dp)
                    .border(1.5.dp, Lipi.CardBorder, CircleShape)
            )

            AgentStatus.FLAGGED -> Icon(
                Icons.Default.Warning,
                null,
                tint = WarningColor,
                modifier = Modifier.size(20.dp)
            )

            AgentStatus.FAILED -> Icon(
                Icons.Default.Warning,
                null,
                tint = Lipi.Error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun rowLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.row_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.row_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.row_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.row_safety
    else -> Res.string.manager_title
}
