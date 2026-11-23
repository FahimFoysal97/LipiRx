package com.snehonir.lipirx.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Maps [LipiColors] onto a Material 3 [ColorScheme].
 *
 * The mapping is identical for light and dark; only the base scheme (which supplies
 * defaults for roles we don't override) differs, so there is a single copy of it.
 */
internal fun LipiColors.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        tertiary = success,
        onTertiary = onPrimary,
        tertiaryContainer = amberContainer,
        onTertiaryContainer = onAmber,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = imageWell,
        onSurfaceVariant = muted,
        surfaceContainerLowest = background,
        surfaceContainerLow = background,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = imageWell,
        outline = outline,
        outlineVariant = cardBorder,
        error = red,
        onError = onError,
        errorContainer = amberContainer,
        onErrorContainer = onAmber,
        surfaceBright = surface,
        surfaceDim = background,
        inverseSurface = ink,
        inverseOnSurface = surface,
        inversePrimary = primary,
        scrim = viewerBg,
    )
}
