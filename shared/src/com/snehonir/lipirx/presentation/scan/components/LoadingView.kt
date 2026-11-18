package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.loading_default_subtitle
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Segmented layout: scanner, one segmented progress bar (one segment per agent),
 * a single "now" card, and a Stop button pinned to the bottom so it is always reachable.
 * Same parameters as before, so call sites don't change.
 */
@Composable
fun LoadingView(
    imageByteArray: ByteArray? = null,
    activeStep: Int = 1,
    statusText: String = "",
    agentProgressPercent: Int = 0,
    currentAgent: RxAgentType = RxAgentType.entries.getOrNull(activeStep) ?: RxAgentType.SUPERVISOR,
    onStopAnalysis: () -> Unit = {},
) {
    val total = RxAgentType.ALL.size
    val percent = agentProgressPercent.coerceIn(0, 100)
    // Read theme colours in composable scope, then use them inside draw lambdas.
    val primaryColor = Lipi.Primary

    val infinite = rememberInfiniteTransition(label = "segmentedLoading")
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
    val breathe = infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )

    val animatedPercent by animateIntAsState(
        targetValue = percent,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "percentText",
    )

    val defaultSubtitle = stringResource(Res.string.loading_default_subtitle)
    val status = statusText.ifBlank { defaultSubtitle }
    val currentAgentName = stringResource(currentAgent.displayNameRes)
    val stepNumber = (activeStep + 1).coerceIn(1, total)
    val overallProgress = ((activeStep.coerceIn(0, total - 1) + percent / 100f) / total).coerceIn(0f, 1f)

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
        // ── Content (vertically centred; scrolls on very small screens) ─────
        BoxWithConstraints(
            modifier = Modifier
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
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                SegScannerCard(imageByteArray = imageByteArray, scan = scan, pulse = pulse)

                // One segment per agent: done = solid, active = filling, waiting = empty.
                // Exposed to screen readers as a single progress bar.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Analysis progress"
                            progressBarRangeInfo = ProgressBarRangeInfo(overallProgress, 0f..1f)
                        },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(total) { i ->
                        SegmentBar(
                            modifier = Modifier.weight(1f),
                            target = when {
                                i < activeStep -> 1f
                                i == activeStep -> percent / 100f
                                else -> 0f
                            },
                            isReached = i <= activeStep,
                        )
                    }
                }

                // The single "now" card. Live region: TalkBack announces status changes.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    shape = RoundedCornerShape(20.dp),
                    color = Lipi.Surface,
                    border = BorderStroke(1.dp, Lipi.CardBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .drawBehind { drawCircle(primaryColor.copy(alpha = breathe.value)) }
                            )
                            Spacer(Modifier.width(10.dp))
                            Crossfade(
                                targetState = currentAgentName,
                                modifier = Modifier.weight(1f),
                                animationSpec = tween(300),
                                label = "agentName",
                            ) { name ->
                                Text(
                                    text = name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Lipi.Ink,
                                    maxLines = 1,
                                )
                            }
                            Surface(shape = CircleShape, color = Lipi.PrimaryContainer) {
                                Text(
                                    text = "$animatedPercent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Lipi.OnPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                        }

                        Crossfade(
                            targetState = status,
                            animationSpec = tween(300),
                            label = "status",
                        ) { text ->
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = Lipi.Muted,
                            )
                        }

                        Crossfade(
                            targetState = "Step $stepNumber of $total",
                            animationSpec = tween(300),
                            label = "stepCount",
                        ) { label ->
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.Muted,
                            )
                        }
                    }
                }
            }
        }

        // ── Stop (pinned, always reachable, quiet red outline) ──────────────
        SecondaryButton(
            text = stringResource(Res.string.btn_stop_analysis),
            icon = Icons.Default.Close,
            onClick = onStopAnalysis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            height = 52.dp,
            contentColor = Lipi.Error,
            fontSize = 15,
        )
    }
}

/** One step of the progress bar. Its fill and track color animate whenever the state changes. */
@Composable
private fun SegmentBar(
    modifier: Modifier,
    target: Float,
    isReached: Boolean,
) {
    val primaryColor = Lipi.Primary
    val fill = animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "segmentFill",
    )
    val track by animateColorAsState(
        targetValue = if (isReached) Lipi.PrimaryContainer else Lipi.CardBorder,
        animationSpec = tween(300),
        label = "segmentTrack",
    )

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(track)
            .drawBehind {
                drawRect(
                    color = primaryColor,
                    size = Size(size.width * fill.value, size.height),
                )
            }
    )
}

/** The prescription "paper" with a viewfinder frame and a sweeping scan beam. */
@Composable
private fun SegScannerCard(
    imageByteArray: ByteArray?,
    scan: State<Float>,
    pulse: State<Float>,
) {
    val primaryColor = Lipi.Primary
    val shape = RoundedCornerShape(22.dp)

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(230.dp, 280.dp)) {
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
                .size(200.dp, 270.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = primaryColor.copy(alpha = 0.25f),
                    spotColor = primaryColor.copy(alpha = 0.35f),
                )
                .clip(shape)
                // Paper stays white in both themes on purpose.
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (imageByteArray != null && imageByteArray.isNotEmpty()) {
                AsyncImage(
                    model = imageByteArray,
                    contentDescription = stringResource(Res.string.content_desc_prescription),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
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

@Preview(name = "Step 0 - Manager Supervisor")
@Composable
fun LoadingViewSupervisorPreview() {
    LipiRxTheme {
        LoadingView(
            activeStep = 0,
            statusText = "Manager Agent evaluating workspace state...",
            agentProgressPercent = 25,
            currentAgent = RxAgentType.SUPERVISOR
        )
    }
}

@Preview(name = "Step 1 - Vision OCR")
@Composable
fun LoadingViewVisionOcrPreview() {
    LipiRxTheme {
        LoadingView(
            activeStep = 1,
            statusText = "Reading handwriting & Vision OCR...",
            agentProgressPercent = 65,
            currentAgent = RxAgentType.TRANSCRIPTION
        )
    }
}

@Preview(name = "Step 2 - Entity Parser")
@Composable
fun LoadingViewEntityParserPreview() {
    LipiRxTheme {
        LoadingView(
            activeStep = 2,
            statusText = "Parsing brand names, dosages & schedules...",
            agentProgressPercent = 45,
            currentAgent = RxAgentType.ENTITY_PARSER
        )
    }
}

@Preview(name = "Step 3 - Generic Substitute Finder")
@Composable
fun LoadingViewSubstitutePreview() {
    LipiRxTheme {
        LoadingView(
            activeStep = 3,
            statusText = "Matching DGDA composition & BD brand substitutes...",
            agentProgressPercent = 80,
            currentAgent = RxAgentType.GENERIC_SUBSTITUTE
        )
    }
}

@Preview(name = "Step 4 - Safety Auditor")
@Composable
fun LoadingViewSafetyAuditorPreview() {
    LipiRxTheme {
        LoadingView(
            activeStep = 4,
            statusText = "Auditing clinical dosage safety & interaction risks...",
            agentProgressPercent = 95,
            currentAgent = RxAgentType.SAFETY_AUDITOR
        )
    }
}

@Preview(name = "Dark - Step 2")
@Composable
fun LoadingViewDarkPreview() {
    LipiRxTheme(themeMode = ThemeMode.DARK) {
        LoadingView(
            activeStep = 2,
            statusText = "Parsing brand names, dosages & schedules...",
            agentProgressPercent = 45,
            currentAgent = RxAgentType.ENTITY_PARSER
        )
    }
}