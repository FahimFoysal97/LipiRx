package com.snehonir.lipirx.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SparkleVectorIcon: ImageVector = ImageVector.Builder(
    name = "GeminiSparkle",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color(0xFF8E24AA))) {
        moveTo(12f, 2f)
        curveTo(12f, 7.5f, 7.5f, 12f, 2f, 12f)
        curveTo(7.5f, 12f, 12f, 16.5f, 12f, 22f)
        curveTo(12f, 16.5f, 16.5f, 12f, 22f, 12f)
        curveTo(16.5f, 12f, 12f, 7.5f, 12f, 2f)
        close()
    }
}.build()

val LightningVectorIcon: ImageVector = ImageVector.Builder(
    name = "GroqLightning",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color(0xFFF57C00))) {
        moveTo(13f, 2f)
        lineTo(4f, 14f)
        lineTo(11f, 14f)
        lineTo(9f, 22f)
        lineTo(20f, 9f)
        lineTo(13f, 9f)
        close()
    }
}.build()

val OpenAiVectorIcon: ImageVector = ImageVector.Builder(
    name = "OpenAiGlobe",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color(0xFF00A67E))) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(11f, 4.07f)
        curveTo(8f, 5f, 6f, 8f, 6f, 12f)
        curveTo(6f, 16f, 8f, 19f, 11f, 19.93f)
        lineTo(11f, 4.07f)
        close()
        moveTo(13f, 4.07f)
        lineTo(13f, 19.93f)
        curveTo(16f, 19f, 18f, 16f, 18f, 12f)
        curveTo(18f, 8f, 16f, 5f, 13f, 4.07f)
        close()
    }
}.build()

val CustomUrlVectorIcon: ImageVector = ImageVector.Builder(
    name = "CustomGear",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color(0xFF546E7A))) {
        moveTo(19.14f, 12.94f)
        curveTo(19.19f, 12.63f, 19.22f, 12.32f, 19.22f, 12f)
        curveTo(19.22f, 11.68f, 19.19f, 11.37f, 19.14f, 11.06f)
        lineTo(21.16f, 9.48f)
        curveTo(21.34f, 9.34f, 21.39f, 9.08f, 21.28f, 8.87f)
        lineTo(19.38f, 5.58f)
        curveTo(19.26f, 5.37f, 19.01f, 5.29f, 18.79f, 5.37f)
        lineTo(16.41f, 6.33f)
        curveTo(15.92f, 5.95f, 15.38f, 5.64f, 14.8f, 5.4f)
        lineTo(14.44f, 2.87f)
        curveTo(14.4f, 2.64f, 14.21f, 2.47f, 13.98f, 2.47f)
        lineTo(10.18f, 2.47f)
        curveTo(9.95f, 2.47f, 9.76f, 2.64f, 9.72f, 2.87f)
        lineTo(9.36f, 5.4f)
        curveTo(8.78f, 5.64f, 8.24f, 5.96f, 7.75f, 6.33f)
        lineTo(5.37f, 5.37f)
        curveTo(5.15f, 5.29f, 4.9f, 5.37f, 4.78f, 5.58f)
        lineTo(2.88f, 8.87f)
        curveTo(2.77f, 9.08f, 2.82f, 9.34f, 3f, 9.48f)
        lineTo(5.02f, 11.06f)
        curveTo(4.97f, 11.37f, 4.94f, 11.69f, 4.94f, 12f)
        curveTo(4.94f, 12.31f, 4.97f, 12.63f, 5.02f, 12.94f)
        lineTo(3f, 14.52f)
        curveTo(2.82f, 14.66f, 2.77f, 14.92f, 2.88f, 15.13f)
        lineTo(4.78f, 18.42f)
        curveTo(4.9f, 18.63f, 5.15f, 18.71f, 5.37f, 18.63f)
        lineTo(7.75f, 17.67f)
        curveTo(8.24f, 18.05f, 8.78f, 18.36f, 9.36f, 18.6f)
        lineTo(9.72f, 21.13f)
        curveTo(9.76f, 21.36f, 9.95f, 21.53f, 10.18f, 21.53f)
        lineTo(13.98f, 21.53f)
        curveTo(14.21f, 21.53f, 14.4f, 21.36f, 14.44f, 21.13f)
        lineTo(14.8f, 18.6f)
        curveTo(15.38f, 18.36f, 15.92f, 18.05f, 16.41f, 17.67f)
        lineTo(18.79f, 18.63f)
        curveTo(19.01f, 18.71f, 19.26f, 18.63f, 19.38f, 18.42f)
        lineTo(21.28f, 15.13f)
        curveTo(21.39f, 14.92f, 21.34f, 14.66f, 21.16f, 14.52f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(12f, 15.5f)
        curveTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
        curveTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
        curveTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
        curveTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
        close()
    }
}.build()

val OnDeviceVectorIcon: ImageVector = ImageVector.Builder(
    name = "OnDevicePhone",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
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
