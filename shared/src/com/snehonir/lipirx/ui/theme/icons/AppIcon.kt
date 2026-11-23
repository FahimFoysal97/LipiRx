package com.snehonir.lipirx.ui.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * LipiRx v2 Noble Prophet Emerald Green App Icon Vector.
 */
val LipiRxAppVectorIcon: ImageVector = ImageVector.Builder(
    name = "LipiRxAppIcon",
    defaultWidth = 108.dp,
    defaultHeight = 108.dp,
    viewportWidth = 108f,
    viewportHeight = 108f
).apply {
    // Background Squircle
    path(fill = SolidColor(Color(0xFF0F6B43))) {
        moveTo(0f, 0f)
        lineTo(108f, 0f)
        lineTo(108f, 108f)
        lineTo(0f, 108f)
        close()
    }
    // Paper Sheet
    path(fill = SolidColor(Color(0xFFFCFDFC))) {
        moveTo(36f, 22f)
        lineTo(62f, 22f)
        lineTo(77f, 37f)
        lineTo(77f, 79f)
        curveTo(77f, 81.76f, 74.76f, 84f, 72f, 84f)
        lineTo(36f, 84f)
        curveTo(33.24f, 84f, 31f, 81.76f, 31f, 79f)
        lineTo(31f, 27f)
        curveTo(31f, 24.24f, 33.24f, 22f, 36f, 22f)
        close()
    }
    // Folded Corner
    path(fill = SolidColor(Color(0xFFC3D8CE))) {
        moveTo(62f, 22f)
        lineTo(62f, 32f)
        curveTo(62f, 34.76f, 64.24f, 37f, 67f, 37f)
        lineTo(77f, 37f)
        close()
    }
    // Rx Symbol
    path(
        stroke = SolidColor(Color(0xFF0F6B43)),
        strokeLineWidth = 4.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(39f, 57f)
        lineTo(39f, 37f)
        lineTo(47f, 37f)
        curveTo(49.76f, 37f, 52f, 39.24f, 52f, 42f)
        curveTo(52f, 44.76f, 49.76f, 47f, 47f, 47f)
        lineTo(39f, 47f)
        moveTo(45f, 47f)
        lineTo(54f, 58f)
        moveTo(49f, 58f)
        lineTo(57f, 50f)
    }
    // Ink Lines
    path(
        stroke = SolidColor(Color(0xFF98B3A6)),
        strokeLineWidth = 2.6f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(39f, 67f)
        lineTo(62f, 67f)
        moveTo(39f, 74f)
        lineTo(54f, 74f)
    }
    // AI Sparkle Star
    path(
        fill = SolidColor(Color(0xFFF5C782)),
        stroke = SolidColor(Color(0xFF073822)),
        strokeLineWidth = 1.6f,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(74f, 64f)
        lineTo(76.4f, 71.6f)
        lineTo(84f, 74f)
        lineTo(76.4f, 76.4f)
        lineTo(74f, 84f)
        lineTo(71.6f, 76.4f)
        lineTo(64f, 74f)
        lineTo(71.6f, 71.6f)
        close()
    }
}.build()
