package com.snehonir.lipirx.presentation.results.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.label_checks_count
import com.snehonir.lipirx.resources.label_running_together
import com.snehonir.lipirx.resources.tap_to_zoom
import com.snehonir.lipirx.resources.title_analyzing
import com.snehonir.lipirx.resources.title_complete
import com.snehonir.lipirx.resources.title_review_needed
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.AgentIcon
import org.jetbrains.compose.resources.stringResource

private val StripIcon = 34.dp

/**
 * Pinned-at-top progress card for the merged screen.
 *
 * Collapsed: thumbnail, status, percent, one bar and the 4-agent strip. Tap the header row to open
 * the supervisor card, the step list and the details log. After the run finishes the thumbnail
 * opens the zoom preview ([onZoom]); while it is running the thumbnail is not tappable.
 */
@Composable
fun AnalysisHeader(
    progress: PipelineProgress,
    imageByteArray: ByteArray?,
    isRunning: Boolean,
    isSuspended: Boolean,
    onZoom: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val fraction by animateFloatAsState(
        targetValue = if (isRunning) progress.overallPercent / 100f else 1f,
        animationSpec = tween(400),
        label = "overall",
    )
    val title = stringResource(
        when {
            isRunning -> Res.string.title_analyzing
            isSuspended || progress.flaggedMedicineNames.isNotEmpty() -> Res.string.title_review_needed
            else -> Res.string.title_complete
        }
    )
    val total = PipelineProgress.WORKERS.size
    val done = if (isRunning) progress.completedChecks else total
    val canZoom = !isRunning && imageByteArray != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (imageByteArray != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp, 60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Lipi.ImageWell)
                            .clickable(enabled = canZoom, role = Role.Button, onClick = onZoom),
                    ) {
                        AsyncImage(
                            model = imageByteArray,
                            contentDescription = stringResource(Res.string.content_desc_prescription),
                            modifier = Modifier.size(48.dp, 60.dp),
                            contentScale = ContentScale.Crop,
                        )
                        if (canZoom) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(Res.string.tap_to_zoom),
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                    .padding(2.dp)
                                    .size(14.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    ) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                        Text(
                            stringResource(Res.string.label_checks_count, done, total),
                            fontSize = 12.sp,
                            color = Lipi.Muted,
                        )
                    }
                    if (isRunning) {
                        Text(
                            "${progress.overallPercent}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Primary,
                        )
                    }
                    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev")
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(if (expanded) Res.string.cd_collapse else Res.string.cd_expand),
                        tint = Lipi.Muted,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = Lipi.Primary,
                trackColor = Lipi.ImageWell,
                strokeCap = StrokeCap.Round,
            )

            PipelineStrip(progress = progress, reduceMotion = reduceMotion)

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SupervisorCard(progress, reduceMotion = reduceMotion)
                    StepperCard(progress, reduceMotion = reduceMotion)
                    DetailsDrawer(progress.feed.takeLast(8))
                }
            }
        }
    }
}

/**
 * Read -> Match -> [Generics | Safety]. The two parallel agents sit in one dashed bracket and the
 * Match agent connects into the bracket, so the fork is visible. The bracket's dashes only show
 * while both run together; afterwards it stays as a faint solid outline so the link remains.
 */
@Composable
private fun PipelineStrip(progress: PipelineProgress, reduceMotion: Boolean) {
    val t = progress.worker(RxAgentType.TRANSCRIPTION)
    val e = progress.worker(RxAgentType.ENTITY_PARSER)
    val g = progress.worker(RxAgentType.GENERIC_SUBSTITUTE)
    val s = progress.worker(RxAgentType.SAFETY_AUDITOR)
    val done = Lipi.Primary.copy(alpha = 0.55f)
    val idle = Lipi.CardBorder
    val bracketColor = if (progress.isParallel) Lipi.Primary.copy(alpha = 0.6f) else if (e.completedOk) done else idle

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        StripNode(RxAgentType.TRANSCRIPTION, t.status, reduceMotion, Modifier.width(56.dp).padding(top = 4.dp))
        Connector(color = if (t.completedOk) done else idle, modifier = Modifier.weight(1f))
        StripNode(RxAgentType.ENTITY_PARSER, e.status, reduceMotion, Modifier.width(56.dp).padding(top = 4.dp))
        Connector(color = if (e.completedOk) done else idle, modifier = Modifier.weight(1f))
        // Fork target: both parallel agents live inside the same bracket.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .drawBehind {
                        drawRoundRect(
                            color = bracketColor,
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = if (progress.isParallel) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null,
                            ),
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                StripNode(RxAgentType.GENERIC_SUBSTITUTE, g.status, reduceMotion, Modifier.width(56.dp))
                // Short link between the two siblings so they read as one group.
                Box(
                    modifier = Modifier
                        .padding(top = StripIcon / 2 + 4.dp)
                        .width(8.dp)
                        .height(2.dp)
                        .background(bracketColor, RoundedCornerShape(50))
                )
                StripNode(RxAgentType.SAFETY_AUDITOR, s.status, reduceMotion, Modifier.width(56.dp))
            }
            AnimatedVisibility(visible = progress.isParallel) {
                Text(
                    stringResource(Res.string.label_running_together),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.Primary,
                )
            }
        }
    }
}

@Composable
private fun StripNode(
    agent: RxAgentType,
    status: AgentStatus,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Top padding matches the bracket padding so every icon sits on one baseline.
        AgentIcon(agent = agent, status = status, size = StripIcon, reduceMotion = reduceMotion)
        Text(
            text = stringResource(rowLabelRes(agent)),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (status == AgentStatus.RUNNING) FontWeight.Bold else FontWeight.Medium,
            color = if (status == AgentStatus.WAITING) Lipi.Muted.copy(alpha = 0.6f) else Lipi.Ink,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontal line with a tiny arrow head, vertically centred on the icon row. */
@Composable
private fun Connector(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(StripIcon + 8.dp)
            .padding(horizontal = 2.dp)
            .drawBehind {
                val y = center.y
                val w = 2.dp.toPx()
                val head = 4.dp.toPx()
                drawLine(color, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round)
                drawLine(color, Offset(size.width - head, y - head), Offset(size.width, y), w, StrokeCap.Round)
                drawLine(color, Offset(size.width - head, y + head), Offset(size.width, y), w, StrokeCap.Round)
            }
    )
}

/**
 * Maps the shared pipeline state onto one card. Every agent works on the whole list, so all cards
 * share the same phases. While an agent re-runs (rework) the card keeps its old values and only
 * shows a "re-reading" badge, so data never blinks out.
 */
internal fun PipelineProgress.toCardProgress(isRunning: Boolean): MedicineCardProgress {
    val safety = worker(RxAgentType.SAFETY_AUDITOR)
    val safetyChecked = safety.status != AgentStatus.FAILED
    if (!isRunning) return MedicineCardProgress(safetyChecked = safetyChecked)

    fun phase(agent: RxAgentType): FieldPhase {
        val w = worker(agent)
        return when {
            w.runs > 1 -> FieldPhase.READY
            w.status == AgentStatus.WAITING -> FieldPhase.PENDING
            w.status == AgentStatus.RUNNING -> FieldPhase.RUNNING
            else -> FieldPhase.READY
        }
    }
    val entity = worker(RxAgentType.ENTITY_PARSER)
    return MedicineCardProgress(
        dose = phase(RxAgentType.ENTITY_PARSER),
        generic = phase(RxAgentType.GENERIC_SUBSTITUTE),
        safety = phase(RxAgentType.SAFETY_AUDITOR),
        rereadingDose = entity.runs > 1 && entity.status == AgentStatus.RUNNING,
        safetyChecked = safetyChecked,
    )
}