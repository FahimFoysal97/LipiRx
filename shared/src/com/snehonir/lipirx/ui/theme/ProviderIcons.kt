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

val SupervisorAgentVectorIcon: ImageVector = ImageVector.Builder(
    name = "SupervisorAgent",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(12f, 2f)
        lineTo(4f, 6f)
        lineTo(4f, 12f)
        curveTo(4f, 16.97f, 7.41f, 21.46f, 12f, 22.5f)
        curveTo(16.59f, 21.46f, 20f, 16.97f, 20f, 12f)
        lineTo(20f, 6f)
        lineTo(12f, 2f)
        close()
        moveTo(12f, 6.5f)
        lineTo(13.62f, 9.78f)
        lineTo(17.24f, 10.31f)
        lineTo(14.62f, 12.87f)
        lineTo(15.24f, 16.49f)
        lineTo(12f, 14.79f)
        lineTo(8.76f, 16.49f)
        lineTo(9.38f, 12.87f)
        lineTo(6.76f, 10.31f)
        lineTo(10.38f, 9.78f)
        lineTo(12f, 6.5f)
        close()
    }
}.build()

val VisionOcrAgentVectorIcon: ImageVector = ImageVector.Builder(
    name = "VisionOcrAgent",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(12f, 4.5f)
        curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
        curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
        curveTo(17f, 19.5f, 21.27f, 16.39f, 23f, 12f)
        curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
        close()
        moveTo(12f, 17f)
        curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
        curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
        curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
        curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
        close()
        moveTo(12f, 9f)
        curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
        curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
        curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
        curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
        close()
    }
}.build()

val MedicalParserAgentVectorIcon: ImageVector = ImageVector.Builder(
    name = "MedicalParserAgent",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(21.41f, 11.58f)
        lineTo(12.41f, 2.58f)
        curveTo(12.05f, 2.22f, 11.55f, 2f, 11f, 2f)
        lineTo(4f, 2f)
        curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
        lineTo(2f, 11f)
        curveTo(2f, 11.55f, 2.22f, 12.05f, 2.59f, 12.42f)
        lineTo(11.59f, 21.42f)
        curveTo(11.95f, 21.78f, 12.45f, 22f, 13f, 22f)
        curveTo(13.55f, 22f, 14.05f, 21.78f, 14.41f, 21.41f)
        lineTo(21.41f, 14.41f)
        curveTo(21.78f, 14.05f, 22f, 13.55f, 22f, 13f)
        curveTo(22f, 12.45f, 21.77f, 11.94f, 21.41f, 11.58f)
        close()
        moveTo(6.5f, 8f)
        curveTo(5.67f, 8f, 5f, 7.33f, 5f, 6.5f)
        curveTo(5f, 5.67f, 5.67f, 5f, 6.5f, 5f)
        curveTo(7.33f, 5f, 8f, 5.67f, 8f, 6.5f)
        curveTo(8f, 7.33f, 7.33f, 8f, 6.5f, 8f)
        close()
    }
}.build()

val GenericSubstituteAgentVectorIcon: ImageVector = ImageVector.Builder(
    name = "GenericSubstituteAgent",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(12f, 4f)
        lineTo(12f, 1f)
        lineTo(8f, 5f)
        lineTo(12f, 9f)
        lineTo(12f, 6f)
        curveTo(15.31f, 6f, 18f, 8.69f, 18f, 12f)
        curveTo(18f, 13.01f, 17.75f, 13.97f, 17.3f, 14.8f)
        lineTo(18.77f, 16.27f)
        curveTo(19.55f, 15.01f, 20f, 13.56f, 20f, 12f)
        curveTo(20f, 7.58f, 16.42f, 4f, 12f, 4f)
        close()
        moveTo(12f, 18f)
        curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f)
        curveTo(6f, 10.99f, 6.25f, 10.03f, 6.7f, 9.2f)
        lineTo(5.23f, 7.73f)
        curveTo(4.45f, 8.99f, 4f, 10.44f, 4f, 12f)
        curveTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
        lineTo(12f, 23f)
        lineTo(16f, 19f)
        lineTo(12f, 15f)
        lineTo(12f, 18f)
        close()
    }
}.build()

val SafetyAuditorAgentVectorIcon: ImageVector = ImageVector.Builder(
    name = "SafetyAuditorAgent",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(12f, 1f)
        lineTo(3f, 5f)
        lineTo(3f, 11f)
        curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
        curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
        lineTo(21f, 5f)
        lineTo(12f, 1f)
        close()
        moveTo(10f, 15.5f)
        lineTo(6.5f, 12f)
        lineTo(7.91f, 10.59f)
        lineTo(10f, 12.67f)
        lineTo(16.09f, 6.58f)
        lineTo(17.5f, 8f)
        lineTo(10f, 15.5f)
        close()
    }
}.build()

val OnDeviceStrategyVectorIcon: ImageVector = ImageVector.Builder(
    name = "OnDeviceStrategy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(12f, 1f)
        lineTo(3f, 5f)
        lineTo(3f, 11f)
        curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
        curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
        lineTo(21f, 5f)
        lineTo(12f, 1f)
        close()
        moveTo(12f, 11.99f)
        lineTo(19f, 11.99f)
        curveTo(18.47f, 16.11f, 15.72f, 19.78f, 12f, 20.93f)
        lineTo(12f, 12f)
        lineTo(5f, 12f)
        lineTo(5f, 6.3f)
        lineTo(12f, 3.19f)
        lineTo(12f, 11.99f)
        close()
    }
}.build()

val McpServerVectorIcon: ImageVector = ImageVector.Builder(
    name = "McpServer",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(16f, 7f)
        lineTo(16f, 3f)
        lineTo(14f, 3f)
        lineTo(14f, 7f)
        lineTo(10f, 7f)
        lineTo(10f, 3f)
        lineTo(8f, 3f)
        lineTo(8f, 7f)
        curveTo(6.9f, 7f, 6f, 7.9f, 6f, 9f)
        lineTo(6f, 14f)
        curveTo(6f, 16.21f, 7.79f, 18f, 10f, 18f)
        lineTo(11f, 18f)
        lineTo(11f, 21f)
        lineTo(13f, 21f)
        lineTo(13f, 18f)
        lineTo(14f, 18f)
        curveTo(16.21f, 18f, 18f, 16.21f, 18f, 14f)
        lineTo(18f, 9f)
        curveTo(18f, 7.9f, 17.1f, 7f, 16f, 7f)
        close()
    }
}.build()

val PipelineMatrixVectorIcon: ImageVector = ImageVector.Builder(
    name = "PipelineMatrix",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(4f, 4f)
        lineTo(8f, 4f)
        lineTo(8f, 8f)
        lineTo(4f, 8f)
        close()
        moveTo(10f, 4f)
        lineTo(14f, 4f)
        lineTo(14f, 8f)
        lineTo(10f, 8f)
        close()
        moveTo(16f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 8f)
        lineTo(16f, 8f)
        close()
        moveTo(4f, 10f)
        lineTo(8f, 10f)
        lineTo(8f, 14f)
        lineTo(4f, 14f)
        close()
        moveTo(10f, 10f)
        lineTo(14f, 10f)
        lineTo(14f, 14f)
        lineTo(10f, 14f)
        close()
        moveTo(16f, 10f)
        lineTo(20f, 10f)
        lineTo(20f, 14f)
        lineTo(16f, 14f)
        close()
        moveTo(4f, 16f)
        lineTo(8f, 16f)
        lineTo(8f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(10f, 16f)
        lineTo(14f, 16f)
        lineTo(14f, 20f)
        lineTo(10f, 20f)
        close()
        moveTo(16f, 16f)
        lineTo(20f, 16f)
        lineTo(20f, 20f)
        lineTo(16f, 20f)
        close()
    }
}.build()

val ProviderSettingsVectorIcon: ImageVector = ImageVector.Builder(
    name = "ProviderSettings",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(3f, 17f)
        verticalLineTo(19f)
        horizontalLineTo(9f)
        verticalLineTo(17f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 5f)
        verticalLineTo(7f)
        horizontalLineTo(13f)
        verticalLineTo(5f)
        horizontalLineTo(3f)
        close()
        moveTo(13f, 21f)
        verticalLineTo(19f)
        horizontalLineTo(21f)
        verticalLineTo(17f)
        horizontalLineTo(13f)
        verticalLineTo(15f)
        horizontalLineTo(11f)
        verticalLineTo(21f)
        horizontalLineTo(13f)
        close()
        moveTo(7f, 9f)
        verticalLineTo(11f)
        horizontalLineTo(3f)
        verticalLineTo(13f)
        horizontalLineTo(7f)
        verticalLineTo(15f)
        horizontalLineTo(9f)
        verticalLineTo(9f)
        horizontalLineTo(7f)
        close()
        moveTo(21f, 13f)
        verticalLineTo(11f)
        horizontalLineTo(11f)
        verticalLineTo(13f)
        horizontalLineTo(21f)
        close()
        moveTo(17f, 9f)
        horizontalLineTo(19f)
        verticalLineTo(7f)
        horizontalLineTo(21f)
        verticalLineTo(5f)
        horizontalLineTo(19f)
        verticalLineTo(3f)
        horizontalLineTo(17f)
        verticalLineTo(9f)
        close()
    }
}.build()

val ApiKeyVectorIcon: ImageVector = ImageVector.Builder(
    name = "ApiKey",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(12.65f, 10f)
        curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
        curveTo(3.69f, 6f, 1f, 8.69f, 1f, 12f)
        curveTo(1f, 15.31f, 3.69f, 18f, 7f, 18f)
        curveTo(9.61f, 18f, 11.83f, 16.33f, 12.65f, 14f)
        lineTo(17f, 14f)
        lineTo(17f, 18f)
        lineTo(21f, 18f)
        lineTo(21f, 14f)
        lineTo(23f, 14f)
        lineTo(23f, 10f)
        lineTo(12.65f, 10f)
        close()
        moveTo(7f, 14f)
        curveTo(5.9f, 14f, 5f, 13.1f, 5f, 12f)
        curveTo(5f, 10.9f, 5.9f, 10f, 7f, 10f)
        curveTo(8.1f, 10f, 9f, 10.9f, 9f, 12f)
        curveTo(9f, 13.1f, 8.1f, 14f, 7f, 14f)
        close()
    }
}.build()

val ProviderSelectVectorIcon: ImageVector = ImageVector.Builder(
    name = "ProviderSelect",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Lipi.Primary)) {
        moveTo(11.99f, 18.54f)
        lineTo(4.37f, 12.62f)
        lineTo(1.54f, 14.81f)
        lineTo(11.99f, 22.94f)
        lineTo(22.44f, 14.81f)
        lineTo(19.61f, 12.62f)
        lineTo(11.99f, 18.54f)
        close()
        moveTo(11.99f, 14.18f)
        lineTo(19.61f, 8.26f)
        lineTo(22.44f, 6.07f)
        lineTo(11.99f, 2f)
        lineTo(1.54f, 6.07f)
        lineTo(4.37f, 8.26f)
        lineTo(11.99f, 14.18f)
        close()
        moveTo(11.99f, 4.25f)
        lineTo(16.82f, 8f)
        lineTo(11.99f, 11.75f)
        lineTo(7.16f, 8f)
        lineTo(11.99f, 4.25f)
        close()
    }
}.build()
