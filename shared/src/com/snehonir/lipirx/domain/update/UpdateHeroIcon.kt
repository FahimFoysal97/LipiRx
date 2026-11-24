package com.snehonir.lipirx.domain.update

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private enum class HeroKind { Checking, UpToDate, Available, Downloading, Permission, Ready, Error }

private fun UpdateState.heroKind() = when (this) {
    is UpdateState.Checking -> HeroKind.Checking
    is UpdateState.UpToDate -> HeroKind.UpToDate
    is UpdateState.Available -> HeroKind.Available
    is UpdateState.Downloading -> HeroKind.Downloading
    is UpdateState.NeedsInstallPermission -> HeroKind.Permission
    is UpdateState.ReadyToInstall -> HeroKind.Ready
    is UpdateState.Error -> HeroKind.Error
    else -> HeroKind.Checking
}

@Composable
fun UpdateHeroIcon(state: UpdateState, modifier: Modifier = Modifier) {
    val kind = state.heroKind()
    val downloading = state as? UpdateState.Downloading
    val sizeKnown = (downloading?.totalBytes ?: 0L) > 0L
    val downloadProgress = downloading?.progress ?: 0f
    val drawsCheck = kind == HeroKind.UpToDate || kind == HeroKind.Ready

    val pop = remember { Animatable(0.6f) }   // entrance spring
    val check = remember { Animatable(0f) }   // ring + check stroke trim
    val shake = remember { Animatable(0f) }   // error shake / permission wobble
    val checkFade = remember { Animatable(1f) }  // check fades out (Ready only)
    val boxIn = remember { Animatable(0f) }      // box icon fades and scales in (Ready only)

    LaunchedEffect(kind) {
        pop.snapTo(0.6f)
        pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    LaunchedEffect(kind) {
        check.snapTo(0f)
        checkFade.snapTo(1f)
        boxIn.snapTo(0f)
        if (drawsCheck) {
            check.animateTo(1f, tween(600, delayMillis = 100, easing = FastOutSlowInEasing))
            if (kind == HeroKind.Ready) {
                delay(350.milliseconds)                                   // let the check be seen
                checkFade.animateTo(0f, tween(180))
                boxIn.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
            }
        }
    }
    LaunchedEffect(kind) {
        shake.snapTo(0f)
        if (kind == HeroKind.Error || kind == HeroKind.Permission) {
            repeat(3) {
                shake.animateTo(1f, tween(60))
                shake.animateTo(-1f, tween(60))
            }
            shake.animateTo(0f, tween(60))
        }
    }

    val infinite = rememberInfiniteTransition(label = "hero")
    val spin by infinite.animateFloat(
        0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin"
    )
    val pulse by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulse"
    )
    val ringProgress by animateFloatAsState(downloadProgress.coerceIn(0f, 1f), tween(300), label = "ring")

    val accent = when (kind) {
        HeroKind.Error -> Lipi.Error
        HeroKind.Permission -> Lipi.OnAmber
        else -> Lipi.Primary
    }
    val container = when (kind) {
        HeroKind.Error -> Lipi.Error.copy(alpha = 0.12f)
        HeroKind.Permission -> Lipi.AmberContainer
        else -> Lipi.PrimaryContainer
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                translationX = if (kind == HeroKind.Error) shake.value * 8.dp.toPx() else 0f
                rotationZ = if (kind == HeroKind.Permission) shake.value * 10f else 0f
            }
            .drawBehind {
                // Pulsing glow (checking) or expanding ripple ring (available)
                if (kind == HeroKind.Checking || kind == HeroKind.Available) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        color = accent.copy(alpha = (1f - pulse) * 0.35f),
                        radius = r * (0.8f + pulse * 0.4f),
                        style = if (kind == HeroKind.Available) Stroke(2.dp.toPx()) else Fill
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(container))

        Canvas(Modifier.size(72.dp)) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            when (kind) {
                HeroKind.Checking -> drawArc(
                    color = accent, startAngle = spin, sweepAngle = 100f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )

                HeroKind.Downloading -> {
                    drawArc(
                        accent.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize,
                        style = Stroke(stroke)
                    )
                    if (sizeKnown) {
                        drawArc(
                            accent, -90f, 360f * ringProgress, false, topLeft, arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    } else {
                        // Unknown total size: spin instead of a stuck empty ring
                        drawArc(
                            accent, spin, 100f, false, topLeft, arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                HeroKind.UpToDate, HeroKind.Ready -> {
                    // Ring draws itself, then the check strokes in
                    drawArc(
                        accent, -90f, 360f * check.value.coerceAtLeast(0.001f), false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.30f, h * 0.52f)
                        lineTo(w * 0.45f, h * 0.66f)
                        lineTo(w * 0.72f, h * 0.36f)
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val out = Path()
                    measure.getSegment(0f, measure.length * check.value, out, true)
                    drawPath(
                        out, accent.copy(alpha = checkFade.value),
                        style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                else -> Unit
            }
        }

        // Center glyphs for the states that aren't fully drawn on the canvas
        when (kind) {
            HeroKind.Checking -> CloudGlyph(accent, Modifier.size(30.dp))
            HeroKind.Available -> DownloadGlyph(accent, Modifier.size(30.dp))
            HeroKind.Downloading -> DownloadGlyph(accent, Modifier.size(28.dp))
            HeroKind.Permission -> Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(28.dp))
            HeroKind.Error -> Icon(Icons.Default.Warning, null, tint = accent, modifier = Modifier.size(30.dp))
            HeroKind.Ready -> PackageGlyph(
                tint = accent,
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        alpha = boxIn.value
                        val s = 0.6f + 0.4f * boxIn.value
                        scaleX = s
                        scaleY = s
                    }
            )
            else -> Unit
        }
    }
}

/** Small cloud that floats gently while checking. */
@Composable
private fun CloudGlyph(tint: Color, modifier: Modifier = Modifier) {
    val floatY by rememberInfiniteTransition(label = "cloud").animateFloat(
        -1.5f, 1.5f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cloud_y"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        translate(top = floatY.dp.toPx()) {
            drawCircle(tint, w * 0.18f, Offset(w * 0.30f, h * 0.62f))
            drawCircle(tint, w * 0.25f, Offset(w * 0.52f, h * 0.48f))
            drawCircle(tint, w * 0.18f, Offset(w * 0.73f, h * 0.62f))
            drawRect(tint, Offset(w * 0.30f, h * 0.62f), Size(w * 0.43f, h * 0.18f))
        }
    }
}

/** Arrow that drops into a tray, on a loop. */
@Composable
private fun DownloadGlyph(tint: Color, modifier: Modifier = Modifier) {
    val t by rememberInfiniteTransition(label = "dl").animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing)),
        label = "dl_t"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.6.dp.toPx()
        val cx = w / 2f

        val tray = Path().apply {
            moveTo(w * 0.18f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.82f, h * 0.82f)
            lineTo(w * 0.82f, h * 0.62f)
        }
        drawPath(tray, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val alpha = when {
            t < 0.2f -> t / 0.2f
            t > 0.75f -> (1f - t) / 0.25f
            else -> 1f
        }
        val arrowColor = tint.copy(alpha = alpha.coerceIn(0f, 1f))
        translate(top = h * 0.14f * t) {
            drawLine(arrowColor, Offset(cx, h * 0.12f), Offset(cx, h * 0.52f), sw, StrokeCap.Round)
            drawLine(arrowColor, Offset(cx - w * 0.17f, h * 0.37f), Offset(cx, h * 0.54f), sw, StrokeCap.Round)
            drawLine(arrowColor, Offset(cx + w * 0.17f, h * 0.37f), Offset(cx, h * 0.54f), sw, StrokeCap.Round)
        }
    }
}

/** Simple package / box icon: cube outline with the inner edges. */
@Composable
private fun PackageGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = 2.4.dp.toPx()

        val outline = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            lineTo(w * 0.88f, h * 0.30f)
            lineTo(w * 0.88f, h * 0.70f)
            lineTo(w * 0.50f, h * 0.90f)
            lineTo(w * 0.12f, h * 0.70f)
            lineTo(w * 0.12f, h * 0.30f)
            close()
        }
        drawPath(outline, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val inner = Path().apply {
            moveTo(w * 0.12f, h * 0.30f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.88f, h * 0.30f)
            moveTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.90f)
        }
        drawPath(inner, tint, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}