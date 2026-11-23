package com.snehonir.lipirx.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalLipiColors = staticCompositionLocalOf { LightLipiColors }

/** Theme-aware tokens. Use inside @Composable code. */
object Lipi {
    val Primary: Color @Composable get() = LocalLipiColors.current.primary
    val OnPrimary: Color @Composable get() = LocalLipiColors.current.onPrimary
    val PrimaryContainer: Color @Composable get() = LocalLipiColors.current.primaryContainer
    val OnPrimaryContainer: Color @Composable get() = LocalLipiColors.current.onPrimaryContainer
    val Background: Color @Composable get() = LocalLipiColors.current.background
    val Surface: Color @Composable get() = LocalLipiColors.current.surface
    val Outline: Color @Composable get() = LocalLipiColors.current.outline
    val CardBorder: Color @Composable get() = LocalLipiColors.current.cardBorder
    val Ink: Color @Composable get() = LocalLipiColors.current.ink
    val Muted: Color @Composable get() = LocalLipiColors.current.muted
    val AmberContainer: Color @Composable get() = LocalLipiColors.current.amberContainer
    val OnAmber: Color @Composable get() = LocalLipiColors.current.onAmber
    val Red: Color @Composable get() = LocalLipiColors.current.red
    val Error: Color @Composable get() = Red
    val Danger: Color @Composable get() = Red
    val OnError: Color @Composable get() = LocalLipiColors.current.onError
    val ImageWell: Color @Composable get() = LocalLipiColors.current.imageWell
    val InkLine: Color @Composable get() = LocalLipiColors.current.inkLine
    val Success: Color @Composable get() = LocalLipiColors.current.success
    val ViewerBg: Color @Composable get() = LocalLipiColors.current.viewerBg
    val SwitchOffTrack: Color @Composable get() = LocalLipiColors.current.switchOffTrack
    val SwitchOffThumb: Color @Composable get() = LocalLipiColors.current.switchOffThumb
    val SwitchOffBorder: Color @Composable get() = LocalLipiColors.current.switchOffBorder
    val StatusActiveGreen: Color @Composable get() = LocalLipiColors.current.statusActiveGreen
    val PaperSheet: Color @Composable get() = LocalLipiColors.current.paperSheet
    val PaperBrand: Color @Composable get() = LocalLipiColors.current.paperBrand
    val PaperInkLine: Color @Composable get() = LocalLipiColors.current.paperInkLine
}

internal fun LipiColors.toMaterialColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
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
