package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.PipelinePhase
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.cd_hide_details
import com.snehonir.lipirx.resources.cd_show_details
import com.snehonir.lipirx.resources.label_checks_count
import com.snehonir.lipirx.resources.label_required_checks
import com.snehonir.lipirx.resources.manager_title
import com.snehonir.lipirx.resources.phase_finalizing
import com.snehonir.lipirx.resources.phase_planning
import com.snehonir.lipirx.resources.phase_running
import com.snehonir.lipirx.resources.phase_waiting_pharmacist
import com.snehonir.lipirx.resources.row_entity
import com.snehonir.lipirx.resources.row_generic
import com.snehonir.lipirx.resources.row_safety
import com.snehonir.lipirx.resources.row_transcription
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ManagerCard(progress: PipelineProgress) {
    val runningNames = progress.running.map { stringResource(rowLabelRes(it)) }
    val phaseText = when (progress.phase) {
        PipelinePhase.PLANNING -> stringResource(Res.string.phase_planning)
        PipelinePhase.RUNNING -> stringResource(
            Res.string.phase_running,
            runningNames.joinToString(", ")
        )
        PipelinePhase.FINALIZING -> stringResource(Res.string.phase_finalizing)
        PipelinePhase.WAITING_FOR_PHARMACIST -> stringResource(Res.string.phase_waiting_pharmacist)
    }
    val checks = PipelineProgress.WORKERS.size
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
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation),
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
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (progress.phase == PipelinePhase.PLANNING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
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
                    text = stringResource(
                        Res.string.label_checks_count,
                        progress.completedChecks,
                        checks
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.OnPrimaryContainer,
                )
            }
            LinearProgressIndicator(
                progress = { animatedChecks },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = Lipi.Primary,
                trackColor = Lipi.Surface,
                strokeCap = StrokeCap.Round,
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
