package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.content_desc_stop_analysis
import com.snehonir.lipirx.resources.loading_default_subtitle
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingView(
    imageByteArray: ByteArray? = null,
    activeStep: Int = 1,
    statusText: String = "",
    agentProgressPercent: Int = 0,
    onStopAnalysis: () -> Unit = {}
) {
    val agents = RxAgentType.ALL
    val percent = agentProgressPercent.coerceIn(0, 100)

    // One shared transition drives every looping animation. The values are passed as State
    // and only read in draw scopes, so these loops never trigger recomposition.
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

    val animatedProgress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "agentProgress",
    )

    val defaultSubtitle = stringResource(Res.string.loading_default_subtitle)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            // soft brand glow at the top of the screen
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Lipi.Primary.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.95f,
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val minHeight = maxHeight
            // Scrolls on small screens / large font sizes, centered when there's room.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .heightIn(min = minHeight - 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
            ) {
                ScannerCard(imageByteArray = imageByteArray, scan = scan, pulse = pulse)

                // Timeline stepper
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Lipi.CardBorder),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        agents.forEachIndexed { i, agent ->
                            val isCompleted = i < activeStep
                            val isActive = i == activeStep
                            val isLast = i == agents.lastIndex

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // Node + connector
                                Column(
                                    modifier = Modifier.width(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    StepNode(
                                        index = i,
                                        isCompleted = isCompleted,
                                        isActive = isActive,
                                        pulse = pulse,
                                    )
                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .width(2.dp)
                                                .weight(1f)
                                                .clip(CircleShape)
                                                .background(if (isCompleted) Lipi.Primary else Lipi.CardBorder)
                                        )
                                    }
                                }

                                // Label + trailing state
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (isLast) 0.dp else 14.dp),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 26.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(agent.displayNameRes),
                                            modifier = Modifier.weight(1f),
                                            fontSize = 14.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isActive) Lipi.Ink else Lipi.Muted,
                                        )
                                        when {
                                            isActive -> Surface(shape = CircleShape, color = Lipi.PrimaryContainer) {
                                                Text(
                                                    text = "$percent%",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Lipi.OnPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                                )
                                            }
                                            isCompleted -> Text(
                                                text = "Done",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Lipi.Primary.copy(alpha = 0.8f),
                                            )
                                        }
                                    }
                                    if (isActive) {
                                        Crossfade(
                                            targetState = statusText.ifBlank { defaultSubtitle },
                                            animationSpec = tween(300),
                                            label = "status",
                                        ) { text ->
                                            Text(
                                                text = text,
                                                fontSize = 12.sp,
                                                lineHeight = 17.sp,
                                                color = Lipi.Muted,
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(CircleShape),
                                            color = Lipi.Primary,
                                            trackColor = Lipi.PrimaryContainer,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Stop
                OutlinedButton(
                    onClick = onStopAnalysis,
                    modifier = Modifier.height(44.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Lipi.Error.copy(alpha = 0.35f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Lipi.Error.copy(alpha = 0.08f),
                        contentColor = Lipi.Error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.content_desc_stop_analysis),
                        modifier = Modifier.size(16.dp),
                        tint = Lipi.Error,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.btn_stop_analysis),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Error,
                    )
                }
            }
        }
    }
}

/** The prescription "paper" with a viewfinder frame and a sweeping scan beam. */
@Composable
private fun ScannerCard(
    imageByteArray: ByteArray?,
    scan: State<Float>,
    pulse: State<Float>,
) {
    val shape = RoundedCornerShape(22.dp)

    Box(contentAlignment = Alignment.Center) {
        // breathing halo behind the card
        Canvas(modifier = Modifier.size(240.dp, 290.dp)) {
            val strength = 0.14f + 0.10f * (1f - pulse.value)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(Lipi.Primary.copy(alpha = strength), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.62f,
                )
            )
        }

        Box(
            modifier = Modifier
                .size(168.dp, 224.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = Lipi.Primary.copy(alpha = 0.25f),
                    spotColor = Lipi.Primary.copy(alpha = 0.35f),
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
                    contentScale = ContentScale.Crop,
                )
            } else {
                PaperIllustration(128.dp, 172.dp)
            }

            // scan beam + viewfinder corners
            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * scan.value
                val glow = 44.dp.toPx()

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Lipi.Primary.copy(alpha = 0.32f),
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
                        listOf(Color.Transparent, Lipi.Primary, Lipi.Primary, Color.Transparent)
                    ),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                val inset = 12.dp.toPx()
                val len = 22.dp.toPx()
                val sw = 3.dp.toPx()
                val c = Lipi.Primary
                val l = inset
                val t = inset
                val r = size.width - inset
                val b = size.height - inset

                // top-left
                drawLine(c, Offset(l, t), Offset(l + len, t), sw, StrokeCap.Round)
                drawLine(c, Offset(l, t), Offset(l, t + len), sw, StrokeCap.Round)
                // top-right
                drawLine(c, Offset(r, t), Offset(r - len, t), sw, StrokeCap.Round)
                drawLine(c, Offset(r, t), Offset(r, t + len), sw, StrokeCap.Round)
                // bottom-left
                drawLine(c, Offset(l, b), Offset(l + len, b), sw, StrokeCap.Round)
                drawLine(c, Offset(l, b), Offset(l, b - len), sw, StrokeCap.Round)
                // bottom-right
                drawLine(c, Offset(r, b), Offset(r - len, b), sw, StrokeCap.Round)
                drawLine(c, Offset(r, b), Offset(r, b - len), sw, StrokeCap.Round)
            }
        }
    }
}

/** Completed = check, active = pulsing dot, pending = step number. */
@Composable
private fun StepNode(
    index: Int,
    isCompleted: Boolean,
    isActive: Boolean,
    pulse: State<Float>,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .drawBehind {
                if (isActive) {
                    drawCircle(
                        color = Lipi.Primary.copy(alpha = 0.30f * (1f - pulse.value)),
                        radius = size.minDimension / 2f * (1f + 0.7f * pulse.value),
                    )
                }
            }
            .clip(CircleShape)
            .background(
                when {
                    isCompleted -> Lipi.Primary
                    isActive -> Lipi.PrimaryContainer
                    else -> Lipi.Background
                }
            )
            .then(
                if (!isCompleted && !isActive) Modifier.border(1.dp, Lipi.CardBorder, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isCompleted -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = Color.White,
            )
            isActive -> Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(Lipi.Primary)
            )
            else -> Text(
                text = "${index + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Muted,
            )
        }
    }
}

@Preview
@Composable
fun LoadingViewVisionOcrPreview() {
    LipiRxTheme {
        LoadingView(
            activeStep = 3,
            statusText = "Reading handwriting & Vision OCR...",
            agentProgressPercent = 65
        )
    }
}