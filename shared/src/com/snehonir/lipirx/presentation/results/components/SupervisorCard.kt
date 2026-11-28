package com.snehonir.lipirx.presentation.results.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.PipelinePhase
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.cd_hide_details
import com.snehonir.lipirx.resources.cd_show_details
import com.snehonir.lipirx.resources.chip_recheck
import com.snehonir.lipirx.resources.label_current_decision
import com.snehonir.lipirx.resources.label_delegated_to
import com.snehonir.lipirx.resources.label_turn
import com.snehonir.lipirx.resources.manager_title
import com.snehonir.lipirx.resources.phase_finalizing
import com.snehonir.lipirx.resources.phase_planning
import com.snehonir.lipirx.resources.phase_running
import com.snehonir.lipirx.resources.phase_waiting_pharmacist
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.AgentIcon
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Makes the supervisor visible: who is in charge, what it is doing right now, why it chose that
 * (its reasoning), and which workers it has handed work to.
 */
@Composable
internal fun SupervisorCard(
    progress: PipelineProgress,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    val phase = progress.phase
    val waitingForPharmacist = phase == PipelinePhase.WAITING_FOR_PHARMACIST
    val isDeciding = phase == PipelinePhase.PLANNING
    val isActive = phase == PipelinePhase.PLANNING || phase == PipelinePhase.RUNNING

    val running = progress.running
    val runningNames = running.map { stringResource(rowLabelRes(it)) }
    val phaseText = when (phase) {
        PipelinePhase.PLANNING -> stringResource(Res.string.phase_planning)
        PipelinePhase.RUNNING -> stringResource(Res.string.phase_running, runningNames.joinToString(", "))
        PipelinePhase.FINALIZING -> stringResource(Res.string.phase_finalizing)
        PipelinePhase.WAITING_FOR_PHARMACIST -> stringResource(Res.string.phase_waiting_pharmacist)
    }

    val container by animateColorAsState(
        targetValue = if (waitingForPharmacist) LoadingWarningContainer else Lipi.PrimaryContainer,
        animationSpec = tween(400),
        label = "supervisorContainer",
    )
    val accent = if (waitingForPharmacist) LoadingWarning else Lipi.Primary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = container,
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(tween(250, easing = FastOutSlowInEasing))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SupervisorAvatar(active = isActive && !reduceMotion, accent = accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.manager_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.OnPrimaryContainer,
                    )
                    Crossfade(
                        targetState = phaseText,
                        animationSpec = tween(300),
                        label = "supervisorPhase",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    ) { text ->
                        Text(
                            text = text,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Ink,
                        )
                    }
                }
                if (progress.turn > 0) {
                    SmallChip(
                        text = stringResource(Res.string.label_turn, progress.turn),
                        container = Lipi.Surface,
                        content = Lipi.OnPrimaryContainer,
                    )
                }
            }

            AnimatedVisibility(visible = isDeciding, enter = fadeIn(), exit = fadeOut()) {
                val barModifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                if (reduceMotion) {
                    LinearProgressIndicator(
                        progress = { 0.4f },
                        modifier = barModifier,
                        color = accent,
                        trackColor = Lipi.Surface,
                        strokeCap = StrokeCap.Round,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = barModifier,
                        color = accent,
                        trackColor = Lipi.Surface,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            if (progress.reasoning.isNotBlank()) {
                DecisionBubble(reasoning = progress.reasoning)
            }

            AnimatedVisibility(
                visible = running.isNotEmpty(),
                enter = fadeIn(tween(250)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.label_delegated_to),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.OnPrimaryContainer,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        running.forEach { agent ->
                            DelegatedChip(
                                agent = agent,
                                reduceMotion = reduceMotion,
                                name = stringResource(rowLabelRes(agent)),
                                isRecheck = progress.worker(agent).runs > 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The supervisor's own words about its current decision. Tap to expand long reasoning. */
@Composable
private fun DecisionBubble(reasoning: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val toggleLabel = stringResource(
        if (expanded) Res.string.cd_hide_details else Res.string.cd_show_details
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Lipi.Surface,
    ) {
        Column(
            modifier = Modifier
                .clickable(role = Role.Button, onClickLabel = toggleLabel) { expanded = !expanded }
                .animateContentSize(tween(250, easing = FastOutSlowInEasing))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.label_current_decision),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Muted,
            )
            Crossfade(
                targetState = reasoning,
                animationSpec = tween(300),
                label = "supervisorReasoning",
            ) { text ->
                Text(
                    text = text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Lipi.Ink,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DelegatedChip(agent: RxAgentType, name: String, isRecheck: Boolean, reduceMotion: Boolean) {
    Surface(shape = RoundedCornerShape(999.dp), color = Lipi.Surface) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AgentIcon(
                agent = agent,
                status = AgentStatus.RUNNING,
                size = 20.dp,
                showDoneBadge = false,
                reduceMotion = reduceMotion,
            )
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Ink,
            )
            if (isRecheck) {
                Text(
                    text = stringResource(Res.string.chip_recheck),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Primary,
                )
            }
        }
    }
}

@Composable
private fun SmallChip(text: String, container: Color, content: Color) {
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

/**
 * A small "hub" mark: a core with four satellites. While the supervisor is deciding or has work
 * in flight the satellites orbit and a ring ripples out; once it is done it settles.
 */
@Composable
private fun SupervisorAvatar(active: Boolean, accent: Color) {
    val transition = rememberInfiniteTransition(label = "supervisorAvatar")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )

    Canvas(modifier = Modifier.size(44.dp)) {
        val c = center
        val r = 18.dp.toPx()

        if (active) {
            drawCircle(
                color = accent.copy(alpha = (1f - ring) * 0.45f),
                radius = r + 4.dp.toPx() * ring,
                center = c,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        drawCircle(color = accent, radius = r, center = c)
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = c)

        for (i in 0 until 4) {
            val degrees = i * 90f + 45f + if (active) spin else 0f
            val a = degrees * (PI / 180.0).toFloat()
            val p = c + Offset(cos(a) * 11.dp.toPx(), sin(a) * 11.dp.toPx())
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = c,
                end = p,
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = p)
        }
    }
}