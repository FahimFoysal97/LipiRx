package com.snehonir.lipirx.ui.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.snehonir.lipirx.ui.theme.Lipi

val CameraVectorIcon = ImageVector.Builder(
    name = "CameraOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(23f, 19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 21f)
        lineTo(3f, 21f)
        arcTo(2f, 2f, 0f, false, true, 1f, 19f)
        lineTo(1f, 8f)
        arcTo(2f, 2f, 0f, false, true, 3f, 6f)
        lineTo(7f, 6f)
        lineTo(9f, 3f)
        lineTo(15f, 3f)
        lineTo(17f, 6f)
        lineTo(21f, 6f)
        arcTo(2f, 2f, 0f, false, true, 23f, 8f)
        close()
    }
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 17f)
        arcTo(3.5f, 3.5f, 0f, true, false, 12f, 10f)
        arcTo(3.5f, 3.5f, 0f, false, false, 12f, 17f)
        close()
    }
}.build()

val GalleryVectorIcon = ImageVector.Builder(
    name = "GalleryOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        arcTo(2f, 2f, 0f, false, false, 3f, 5f)
        lineTo(3f, 19f)
        arcTo(2f, 2f, 0f, false, false, 5f, 21f)
        lineTo(19f, 21f)
        arcTo(2f, 2f, 0f, false, false, 21f, 19f)
        lineTo(21f, 5f)
        arcTo(2f, 2f, 0f, false, false, 19f, 3f)
        close()
    }
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(8.5f, 10f)
        arcTo(1.5f, 1.5f, 0f, true, false, 8.5f, 7f)
        arcTo(1.5f, 1.5f, 0f, false, false, 8.5f, 10f)
        close()
    }
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(21f, 15f)
        lineTo(16f, 10f)
        lineTo(5f, 21f)
    }
}.build()

val MultiAgentPipelineIcon = ImageVector.Builder(
    name = "MultiAgentPipeline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Primary),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        lineTo(20f, 7f)
        lineTo(20f, 17f)
        lineTo(12f, 22f)
        lineTo(4f, 17f)
        lineTo(4f, 7f)
        close()
        moveTo(12f, 6f)
        lineTo(12f, 18f)
        moveTo(6f, 9f)
        lineTo(18f, 15f)
        moveTo(18f, 9f)
        lineTo(6f, 15f)
    }
}.build()

val CopyVectorIcon = ImageVector.Builder(
    name = "CopyOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(16f, 1f)
        lineTo(4f, 1f)
        arcTo(2f, 2f, 0f, false, false, 2f, 3f)
        lineTo(2f, 15f)
        moveTo(8f, 5f)
        lineTo(20f, 5f)
        arcTo(2f, 2f, 0f, false, true, 22f, 7f)
        lineTo(22f, 21f)
        arcTo(2f, 2f, 0f, false, true, 20f, 23f)
        lineTo(8f, 23f)
        arcTo(2f, 2f, 0f, false, true, 6f, 21f)
        lineTo(6f, 7f)
        arcTo(2f, 2f, 0f, false, true, 8f, 5f)
        close()
    }
}.build()

val PasteVectorIcon = ImageVector.Builder(
    name = "ContentPaste",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Primary),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 2f)
        lineTo(16f, 2f)
        arcTo(2f, 2f, 0f, false, false, 12f, 0.5f)
        arcTo(2f, 2f, 0f, false, false, 8f, 2f)
        lineTo(5f, 2f)
        arcTo(2f, 2f, 0f, false, false, 3f, 4f)
        lineTo(3f, 20f)
        arcTo(2f, 2f, 0f, false, false, 5f, 22f)
        lineTo(19f, 22f)
        arcTo(2f, 2f, 0f, false, false, 21f, 20f)
        lineTo(21f, 4f)
        arcTo(2f, 2f, 0f, false, false, 19f, 2f)
        close()
    }
    path(
        fill = SolidColor(Lipi.Primary)
    ) {
        moveTo(14f, 11f)
        lineTo(7f, 11f)
        lineTo(7f, 13f)
        lineTo(14f, 13f)
        close()
        moveTo(17f, 15f)
        lineTo(7f, 15f)
        lineTo(7f, 17f)
        lineTo(17f, 17f)
        close()
    }
}.build()
