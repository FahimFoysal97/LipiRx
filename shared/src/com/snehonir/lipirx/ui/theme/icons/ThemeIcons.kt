package com.snehonir.lipirx.ui.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SystemThemeVectorIcon: ImageVector = ImageVector.Builder(
    name = "SystemTheme",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(17f, 1.01f)
        lineTo(7f, 1f)
        curveTo(5.9f, 1f, 5f, 1.9f, 5f, 3f)
        lineTo(5f, 21f)
        curveTo(5f, 22.1f, 5.9f, 23f, 7f, 23f)
        lineTo(17f, 23f)
        curveTo(18.1f, 23f, 19f, 22.1f, 19f, 21f)
        lineTo(19f, 3f)
        curveTo(19f, 1.9f, 18.1f, 1.01f, 17f, 1.01f)
        close()
        moveTo(17f, 19f)
        lineTo(7f, 19f)
        lineTo(7f, 5f)
        lineTo(17f, 5f)
        lineTo(17f, 19f)
        close()
    }
}.build()

val DarkThemeVectorIcon: ImageVector = ImageVector.Builder(
    name = "DarkTheme",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12.3f, 2f)
        curveTo(6.58f, 2f, 2f, 6.58f, 2f, 12.3f)
        curveTo(2f, 18.02f, 6.58f, 22.6f, 12.3f, 22.6f)
        curveTo(17.52f, 22.6f, 21.79f, 18.77f, 22.5f, 13.7f)
        curveTo(20.91f, 14.54f, 19.1f, 15f, 17.18f, 15f)
        curveTo(11.56f, 15f, 7f, 10.44f, 7f, 4.82f)
        curveTo(7f, 3.84f, 7.14f, 2.89f, 7.4f, 2f)
        curveTo(9.02f, 2f, 10.66f, 2f, 12.3f, 2f)
        close()
    }
}.build()

val LightThemeVectorIcon: ImageVector = ImageVector.Builder(
    name = "LightTheme",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 7f)
        arcTo(5f, 5f, 0f, true, true, 12f, 17f)
        arcTo(5f, 5f, 0f, false, true, 12f, 7f)
        close()
        moveTo(12f, 1f)
        lineTo(12f, 3f)
        moveTo(12f, 21f)
        lineTo(12f, 23f)
        moveTo(4.22f, 4.22f)
        lineTo(5.64f, 5.64f)
        moveTo(18.36f, 18.36f)
        lineTo(19.78f, 19.78f)
        moveTo(1f, 12f)
        lineTo(3f, 12f)
        moveTo(21f, 12f)
        lineTo(23f, 12f)
        moveTo(4.22f, 19.78f)
        lineTo(5.64f, 18.36f)
        moveTo(18.36f, 5.64f)
        lineTo(19.78f, 4.22f)
    }
}.build()
