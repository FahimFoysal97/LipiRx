package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.presentation.common.PaperIllustration
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.cd_scan_complete
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

/**
 * The prescription "hero". While [isScanning] it shows the sweeping scan line, corner brackets
 * and a breathing glow. When the pipeline is done the scanner stops and a check overlay fades in,
 * so the picture itself tells the user the work is finished.
 */
@Composable
internal fun ScanHero(
    imageByteArray: ByteArray?,
    scan: State<Float>,
    pulse: State<Float>,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    val primaryColor = Lipi.Primary
    val shape = RoundedCornerShape(22.dp)
    val borderColor by animateColorAsState(
        targetValue = if (isScanning) primaryColor.copy(alpha = 0.55f) else primaryColor,
        animationSpec = tween(400),
        label = "heroBorder",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isScanning) {
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
        }

        Box(
            modifier = Modifier
                .size(200.dp, 240.dp)
                .shadow(
                    elevation = if (isScanning) 20.dp else 6.dp,
                    shape = shape,
                    ambientColor = primaryColor.copy(alpha = if (isScanning) 0.25f else 0.08f),
                    spotColor = primaryColor.copy(alpha = if (isScanning) 0.35f else 0.12f),
                )
                .clip(shape)
                .background(Color.White)
                .border(1.5.dp, borderColor, shape),
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

            if (isScanning) {
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
                    drawCornerBrackets(primaryColor, inset = 12.dp.toPx(), len = 22.dp.toPx(), sw = 3.dp.toPx())
                }
            }

            AnimatedVisibility(
                visible = !isScanning,
                enter = fadeIn(tween(350)),
                exit = fadeOut(tween(150)),
                modifier = Modifier.matchParentSize(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                        //.background(Lipi.PrimaryContainer.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    AnimatedVisibility(
                        visible = !isScanning,
                        enter = scaleIn(tween(350), initialScale = 0.6f) + fadeIn(tween(350)),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Lipi.Primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.cd_scan_complete),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCornerBrackets(color: Color, inset: Float, len: Float, sw: Float) {
    for (sx in intArrayOf(1, -1)) {
        for (sy in intArrayOf(1, -1)) {
            val origin = Offset(
                x = if (sx > 0) inset else size.width - inset,
                y = if (sy > 0) inset else size.height - inset,
            )
            drawLine(color, origin, origin + Offset(len * sx, 0f), sw, StrokeCap.Round)
            drawLine(color, origin, origin + Offset(0f, len * sy), sw, StrokeCap.Round)
        }
    }
}