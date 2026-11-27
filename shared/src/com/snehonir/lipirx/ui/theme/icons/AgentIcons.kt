package com.snehonir.lipirx.ui.theme.icons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.presentation.loading.components.LoadingWarning
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import kotlin.math.abs
import kotlin.math.min

/*
 * Static, tintable agent icons (use with Icon(imageVector = ..., tint = ...)).
 * Same designs as the animated AgentIcon below, glyph only. Everything sits on a 26-unit viewport
 * with a 1-unit margin so round strokes are never clipped at the edge.
 */
private const val VECTOR_VIEWPORT = 26f
private const val ICON_STROKE = 1.8f

private fun agentVector(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VECTOR_VIEWPORT,
        viewportHeight = VECTOR_VIEWPORT
    ).apply(block).build()

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

/** Supervisor: a hub with four satellites. */
val SupervisorAgentVectorIcon: ImageVector = agentVector("SupervisorAgent") {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(13f, 13f); lineTo(5.5f, 5.5f)
        moveTo(13f, 13f); lineTo(20.5f, 5.5f)
        moveTo(13f, 13f); lineTo(20.5f, 20.5f)
        moveTo(13f, 13f); lineTo(5.5f, 20.5f)
    }
    path(fill = SolidColor(Color.Black)) {
        circle(13f, 13f, 3.6f)
        circle(5.5f, 5.5f, 2.6f)
        circle(20.5f, 5.5f, 2.6f)
        circle(20.5f, 20.5f, 2.6f)
        circle(5.5f, 20.5f, 2.6f)
    }
}

/** Transcription (reads the prescription): a document with text lines. */
val VisionOcrAgentVectorIcon: ImageVector = agentVector("VisionOcrAgent") {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = ICON_STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(8f, 2f)
        horizontalLineTo(18f)
        arcTo(3f, 3f, 0f, false, true, 21f, 5f)
        verticalLineTo(21f)
        arcTo(3f, 3f, 0f, false, true, 18f, 24f)
        horizontalLineTo(8f)
        arcTo(3f, 3f, 0f, false, true, 5f, 21f)
        verticalLineTo(5f)
        arcTo(3f, 3f, 0f, false, true, 8f, 2f)
        close()
        moveTo(9f, 8f); horizontalLineTo(17f)
        moveTo(9f, 13f); horizontalLineTo(17f)
        moveTo(9f, 18f); horizontalLineTo(14f)
    }
}

/** Entity parser (identifies medicines): a split capsule inside corner brackets. */
val MedicalParserAgentVectorIcon: ImageVector = agentVector("MedicalParserAgent") {
    group(rotate = -45f, pivotX = 13f, pivotY = 13f) {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = ICON_STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 8.5f)
            horizontalLineTo(18f)
            arcTo(4.5f, 4.5f, 0f, false, true, 18f, 17.5f)
            horizontalLineTo(8f)
            arcTo(4.5f, 4.5f, 0f, false, true, 8f, 8.5f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(13f, 8.5f)
            horizontalLineTo(8f)
            arcTo(4.5f, 4.5f, 0f, false, false, 8f, 17.5f)
            horizontalLineTo(13f)
            close()
        }
    }
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = ICON_STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(1f, 7f); verticalLineTo(4f); quadTo(1f, 1f, 4f, 1f); horizontalLineTo(7f)
        moveTo(25f, 7f); verticalLineTo(4f); quadTo(25f, 1f, 22f, 1f); horizontalLineTo(19f)
        moveTo(1f, 19f); verticalLineTo(22f); quadTo(1f, 25f, 4f, 25f); horizontalLineTo(7f)
        moveTo(25f, 19f); verticalLineTo(22f); quadTo(25f, 25f, 22f, 25f); horizontalLineTo(19f)
    }
}

/** Generic substitute (finds alternatives): two tablets between swap arrows. */
val GenericSubstituteAgentVectorIcon: ImageVector = agentVector("GenericSubstituteAgent") {
    path(fill = SolidColor(Color.Black)) {
        circle(5.5f, 13f, 4.5f)
    }
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = ICON_STROKE,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        circle(20.5f, 13f, 4.5f)
        moveTo(6f, 7f); curveTo(9f, 1f, 17f, 1f, 20f, 7f)
        moveTo(16.9f, 5.3f); lineTo(20f, 7f); lineTo(20.5f, 3.5f)
        moveTo(20f, 19f); curveTo(17f, 25f, 9f, 25f, 6f, 19f)
        moveTo(9.1f, 20.7f); lineTo(6f, 19f); lineTo(5.5f, 22.5f)
    }
}

/** Safety auditor (safety check): a shield with the check cut out of it. */
val SafetyAuditorAgentVectorIcon: ImageVector = agentVector("SafetyAuditorAgent") {
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(13f, 1.5f)
        lineTo(22.5f, 5f)
        lineTo(22.5f, 12.5f)
        curveTo(22.5f, 18.5f, 18.5f, 22.5f, 13f, 25f)
        curveTo(7.5f, 22.5f, 3.5f, 18.5f, 3.5f, 12.5f)
        lineTo(3.5f, 5f)
        close()
        moveTo(7.734f, 13.79f)
        lineTo(11.85f, 17.781f)
        lineTo(18.614f, 10.34f)
        lineTo(16.986f, 8.86f)
        lineTo(11.75f, 14.619f)
        lineTo(9.266f, 12.21f)
        close()
    }
}

private const val ICON_UNITS = 44f
private const val STROKE = 1.8f
private val DoneGreen = Color(0xFF2E7D32)
private val CENTER = Offset(22f, 22f)

/**
 * One icon per worker, drawn on a 44-unit grid so it stays crisp from 20dp (chips) to 72dp.
 * Same visual language as the supervisor mark: a filled disc with a white glyph.
 *
 *  - TRANSCRIPTION      document with a scan line that sweeps while it runs
 *  - ENTITY_PARSER      split capsule inside corner brackets that pulse while it runs
 *  - GENERIC_SUBSTITUTE two tablets that swap places between swap arrows
 *  - SAFETY_AUDITOR     shield whose check draws itself; becomes "!" when flagged or failed
 *
 * The disc color follows [status] (waiting is muted, running and done are primary, flagged is amber,
 * failed is red). Animation only runs for RUNNING and is skipped when [reduceMotion] is set.
 */
@Composable
internal fun AgentIcon(
    agent: RxAgentType,
    status: AgentStatus,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showDoneBadge: Boolean = true,
    reduceMotion: Boolean = false,
) {
    val active = status == AgentStatus.RUNNING && !reduceMotion
    val disc by animateColorAsState(discColor(status), tween(300), label = "agentDisc")
    val glyph by animateColorAsState(glyphColor(status), tween(300), label = "agentGlyph")
    val cutout = if (status == AgentStatus.WAITING) Lipi.Surface else disc
    val badgeRing = Lipi.Surface

    val t = if (active) {
        val transition = rememberInfiniteTransition(label = "agentIcon")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "agentT",
        ).value
    } else {
        0f
    }

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / ICON_UNITS
        withTransform({ scale(s, s, Offset.Zero) }) {
            if (active) {
                drawCircle(
                    color = disc.copy(alpha = (1f - t) * 0.45f),
                    radius = 19f + 3f * t,
                    center = CENTER,
                    style = Stroke(1.6f),
                )
            }
            drawCircle(disc, radius = 19f, center = CENTER)

            when (agent) {
                RxAgentType.TRANSCRIPTION -> drawTranscription(glyph, active, t)
                RxAgentType.ENTITY_PARSER -> drawEntity(glyph, active, t)
                RxAgentType.GENERIC_SUBSTITUTE -> drawGeneric(glyph, active, t)
                RxAgentType.SAFETY_AUDITOR -> drawSafety(
                    fg = glyph,
                    cutout = cutout,
                    alert = status == AgentStatus.FLAGGED || status == AgentStatus.FAILED,
                    progress = if (active) min(1f, t * 1.5f) else 1f,
                )
                else -> Unit
            }

            if (showDoneBadge && status == AgentStatus.DONE) {
                drawCircle(badgeRing, radius = 7.5f, center = Offset(35f, 35f))
                drawCircle(DoneGreen, radius = 6f, center = Offset(35f, 35f))
                val tick = Path().apply {
                    moveTo(32.2f, 35.2f); lineTo(34.3f, 37.2f); lineTo(38f, 33.2f)
                }
                drawPath(tick, Color.White, style = Stroke(1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun discColor(status: AgentStatus): Color = when (status) {
    AgentStatus.WAITING -> Lipi.Muted.copy(alpha = 0.14f)
    AgentStatus.RUNNING, AgentStatus.DONE -> Lipi.Primary
    AgentStatus.FLAGGED -> LoadingWarning
    AgentStatus.FAILED -> Lipi.Error
}

@Composable
private fun glyphColor(status: AgentStatus): Color =
    if (status == AgentStatus.WAITING) Lipi.Muted.copy(alpha = 0.75f) else Color.White

private fun pingPong(t: Float) = 1f - abs(2f * t - 1f)
private fun smooth(x: Float) = x * x * (3f - 2f * x)

private fun DrawScope.drawTranscription(fg: Color, active: Boolean, t: Float) {
    drawRoundRect(fg, Offset(14f, 11f), Size(16f, 22f), CornerRadius(3f), style = Stroke(STROKE))
    drawLine(fg, Offset(18f, 17f), Offset(26f, 17f), STROKE, StrokeCap.Round)
    drawLine(fg, Offset(18f, 22f), Offset(26f, 22f), STROKE, StrokeCap.Round)
    drawLine(fg, Offset(18f, 27f), Offset(23f, 27f), STROKE, StrokeCap.Round)
    if (active) {
        val y = 12f + 20f * smooth(pingPong(t))
        drawLine(fg, Offset(10.5f, y), Offset(33.5f, y), 2.2f, StrokeCap.Round)
    }
}

private fun DrawScope.drawEntity(fg: Color, active: Boolean, t: Float) {
    rotate(-45f, CENTER) {
        drawRoundRect(fg, Offset(12.5f, 17.5f), Size(19f, 9f), CornerRadius(4.5f), style = Stroke(STROKE))
        val half = Path().apply {
            moveTo(22f, 17.5f)
            lineTo(17f, 17.5f)
            arcTo(Rect(12.5f, 17.5f, 21.5f, 26.5f), 270f, -180f, false)
            lineTo(22f, 26.5f)
            close()
        }
        drawPath(half, fg)
    }

    val brackets = Path().apply {
        moveTo(10f, 16f); lineTo(10f, 13f); quadraticTo(10f, 10f, 13f, 10f); lineTo(16f, 10f)
        moveTo(34f, 16f); lineTo(34f, 13f); quadraticTo(34f, 10f, 31f, 10f); lineTo(28f, 10f)
        moveTo(10f, 28f); lineTo(10f, 31f); quadraticTo(10f, 34f, 13f, 34f); lineTo(16f, 34f)
        moveTo(34f, 28f); lineTo(34f, 31f); quadraticTo(34f, 34f, 31f, 34f); lineTo(28f, 34f)
    }
    val k = if (active) 1f - 0.12f * pingPong((t * 1.5f) % 1f) else 1f
    scale(k, CENTER) {
        drawPath(brackets, fg, style = Stroke(STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawGeneric(fg: Color, active: Boolean, t: Float) {
    val e = if (active) smooth(pingPong(t)) else 0f
    drawCircle(fg, radius = 4.5f, center = Offset(14.5f + 15f * e, 22f))
    drawCircle(fg, radius = 4.5f, center = Offset(29.5f - 15f * e, 22f), style = Stroke(STROKE))

    val arrows = Path().apply {
        moveTo(15f, 16f); cubicTo(18f, 10f, 26f, 10f, 29f, 16f)
        moveTo(25.9f, 14.3f); lineTo(29f, 16f); lineTo(29.5f, 12.5f)
        moveTo(29f, 28f); cubicTo(26f, 34f, 18f, 34f, 15f, 28f)
        moveTo(18.1f, 29.7f); lineTo(15f, 28f); lineTo(14.5f, 31.5f)
    }
    drawPath(arrows, fg, style = Stroke(STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawSafety(fg: Color, cutout: Color, alert: Boolean, progress: Float) {
    val shield = Path().apply {
        moveTo(22f, 10.5f)
        lineTo(31.5f, 14f)
        lineTo(31.5f, 21.5f)
        cubicTo(31.5f, 27.5f, 27.5f, 31.5f, 22f, 34f)
        cubicTo(16.5f, 31.5f, 12.5f, 27.5f, 12.5f, 21.5f)
        lineTo(12.5f, 14f)
        close()
    }
    drawPath(shield, fg)

    if (alert) {
        drawLine(cutout, Offset(22f, 17f), Offset(22f, 23.5f), 2.2f, StrokeCap.Round)
        drawCircle(cutout, radius = 1.4f, center = Offset(22f, 27.3f))
    } else {
        val check = Path().apply {
            moveTo(17.5f, 22f); lineTo(20.8f, 25.2f); lineTo(26.8f, 18.6f)
        }
        val measure = PathMeasure().apply { setPath(check, false) }
        val part = Path()
        measure.getSegment(0f, measure.length * progress, part, true)
        drawPath(part, cutout, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Preview(name = "Agent icons")
@Composable
private fun AgentIconPreview() {
    LipiRxTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PipelineProgressAgents.forEach { agent ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AgentStatus.entries.forEach { status ->
                        AgentIcon(agent, status, size = 44.dp)
                    }
                    AgentIcon(agent, AgentStatus.RUNNING, size = 20.dp, showDoneBadge = false)
                }
            }
        }
    }
}

private val PipelineProgressAgents = listOf(
    RxAgentType.TRANSCRIPTION,
    RxAgentType.ENTITY_PARSER,
    RxAgentType.GENERIC_SUBSTITUTE,
    RxAgentType.SAFETY_AUDITOR,
)

@Preview(name = "Static agent vector icons")
@Composable
private fun AgentVectorIconsPreview() {
    LipiRxTheme {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(
                SupervisorAgentVectorIcon,
                VisionOcrAgentVectorIcon,
                MedicalParserAgentVectorIcon,
                GenericSubstituteAgentVectorIcon,
                SafetyAuditorAgentVectorIcon,
            ).forEach { icon ->
                Icon(imageVector = icon, contentDescription = null, tint = Lipi.Ink, modifier = Modifier.size(32.dp))
            }
        }
    }
}