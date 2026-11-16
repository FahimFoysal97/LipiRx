package com.snehonir.lipirx.ui.theme.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.snehonir.lipirx.domain.model.OnDeviceStrategy

/** Cloud first, device as automatic backup: two opposite arrows (switch / fallback). */
val HybridFallbackVectorIcon: ImageVector = ImageVector.Builder(
    name = "StrategyHybridFallback",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        // Right-pointing arrow
        moveTo(3f, 5.5f)
        lineTo(15f, 5.5f)
        lineTo(15f, 3f)
        lineTo(21f, 7.5f)
        lineTo(15f, 12f)
        lineTo(15f, 9.5f)
        lineTo(3f, 9.5f)
        close()
        // Left-pointing arrow
        moveTo(21f, 14.5f)
        lineTo(9f, 14.5f)
        lineTo(9f, 12f)
        lineTo(3f, 16.5f)
        lineTo(9f, 21f)
        lineTo(9f, 18.5f)
        lineTo(21f, 18.5f)
        close()
    }
}.build()

/** Local NPU first: a processor chip. */
val OnDeviceFirstVectorIcon: ImageVector = ImageVector.Builder(
    name = "StrategyOnDeviceFirst",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        // Chip body
        moveTo(7f, 7f)
        lineTo(17f, 7f)
        lineTo(17f, 17f)
        lineTo(7f, 17f)
        close()
        // Core cutout
        moveTo(10f, 10f)
        lineTo(14f, 10f)
        lineTo(14f, 14f)
        lineTo(10f, 14f)
        close()
        // Top pins
        moveTo(9f, 3.5f); lineTo(10.5f, 3.5f); lineTo(10.5f, 7f); lineTo(9f, 7f); close()
        moveTo(13.5f, 3.5f); lineTo(15f, 3.5f); lineTo(15f, 7f); lineTo(13.5f, 7f); close()
        // Bottom pins
        moveTo(9f, 17f); lineTo(10.5f, 17f); lineTo(10.5f, 20.5f); lineTo(9f, 20.5f); close()
        moveTo(13.5f, 17f); lineTo(15f, 17f); lineTo(15f, 20.5f); lineTo(13.5f, 20.5f); close()
        // Left pins
        moveTo(3.5f, 9f); lineTo(7f, 9f); lineTo(7f, 10.5f); lineTo(3.5f, 10.5f); close()
        moveTo(3.5f, 13.5f); lineTo(7f, 13.5f); lineTo(7f, 15f); lineTo(3.5f, 15f); close()
        // Right pins
        moveTo(17f, 9f); lineTo(20.5f, 9f); lineTo(20.5f, 10.5f); lineTo(17f, 10.5f); close()
        moveTo(17f, 13.5f); lineTo(20.5f, 13.5f); lineTo(20.5f, 15f); lineTo(17f, 15f); close()
    }
}.build()

/** Strictly offline / private: a shield with a check mark. */
val OnDeviceOnlyVectorIcon: ImageVector = ImageVector.Builder(
    name = "StrategyOnDeviceOnly",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        // Shield
        moveTo(12f, 2f)
        lineTo(4f, 5f)
        lineTo(4f, 11f)
        curveTo(4f, 16.5f, 7.4f, 20.7f, 12f, 22f)
        curveTo(16.6f, 20.7f, 20f, 16.5f, 20f, 11f)
        lineTo(20f, 5f)
        close()
        // Check mark cutout
        moveTo(8f, 12.4f)
        lineTo(9.4f, 11f)
        lineTo(11f, 12.6f)
        lineTo(14.8f, 8.8f)
        lineTo(16.2f, 10.2f)
        lineTo(11f, 15.4f)
        close()
    }
}.build()

/** Cloud AI only: a cloud. */
val CloudOnlyVectorIcon: ImageVector = ImageVector.Builder(
    name = "StrategyCloudOnly",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 19f)
        curveTo(4.24f, 19f, 2f, 16.76f, 2f, 14f)
        curveTo(2f, 11.5f, 3.9f, 9.4f, 6.3f, 9.05f)
        curveTo(7.2f, 6.2f, 9.8f, 4f, 13f, 4f)
        curveTo(16.6f, 4f, 19.5f, 6.7f, 19.9f, 10.2f)
        curveTo(21.9f, 10.7f, 23f, 12.4f, 23f, 14.4f)
        curveTo(23f, 16.9f, 21f, 19f, 18.5f, 19f)
        close()
    }
}.build()

/** Icon for a strategy. Tint at the call site with the theme color. */
val OnDeviceStrategy.icon: ImageVector
    get() = when (this) {
        OnDeviceStrategy.HYBRID_FALLBACK -> HybridFallbackVectorIcon
        OnDeviceStrategy.ON_DEVICE_FIRST -> OnDeviceFirstVectorIcon
        OnDeviceStrategy.ON_DEVICE_ONLY -> OnDeviceOnlyVectorIcon
        OnDeviceStrategy.CLOUD_ONLY -> CloudOnlyVectorIcon
    }