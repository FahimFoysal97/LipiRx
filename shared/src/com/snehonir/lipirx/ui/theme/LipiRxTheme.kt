package com.snehonir.lipirx.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.snehonir.lipirx.domain.model.ThemeMode

data class LipiColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val outline: Color,
    val cardBorder: Color,
    val ink: Color,
    val muted: Color,
    val amberContainer: Color,
    val onAmber: Color,
    val red: Color,
    val onError: Color,
    val imageWell: Color,
    val inkLine: Color,
    val success: Color,
    val viewerBg: Color,
    val switchOffTrack: Color,
    val switchOffThumb: Color,
    val switchOffBorder: Color,
    val statusActiveGreen: Color,
    val paperSheet: Color,
    val paperBrand: Color,
    val paperInkLine: Color,
)

/**
 * Senior Designer Palette: Noble Islamic Emerald Green (#0F6B43) Theme.
 * Soft sage mint surfaces, serene warm off-white background, obsidian forest ink.
 */
private val LightLipiColors = LipiColors(
    primary = Color(0xFF0F6B43),           // Noble Emerald Green
    onPrimary = Color(0xFFFFFFFF),         // Crisp White
    primaryContainer = Color(0xFFE4F3EC),  // Soothing Sage Mint
    onPrimaryContainer = Color(0xFF073822),// Deep Forest Emerald
    background = Color(0xFFF4F7F5),        // Serene Warm Off-White
    surface = Color(0xFFFFFFFF),           // Pure White Surface Card
    outline = Color(0xFFC3D8CE),           // Refined Sage Outline
    cardBorder = Color(0xFFDDE8E2),        // Delicate Card Border
    ink = Color(0xFF11221A),               // Obsidian Forest Text
    muted = Color(0xFF52685E),             // Muted Sage Slate Text
    amberContainer = Color(0xFFFFF4E5),    // Illumination Gold/Amber Tint
    onAmber = Color(0xFF7A4508),           // Deep Warm Amber
    red = Color(0xFFB83A32),               // Refined Ruby Red
    onError = Color(0xFFFFFFFF),
    imageWell = Color(0xFFE8F0EC),         // Soft Sage Image Well
    inkLine = Color(0xFF98B3A6),           // Delicate Ink Line
    success = Color(0xFF0E8C54),           // Vibrant Emerald Green
    viewerBg = Color(0xFF0E1613),          // Deep Forest Dark Viewer
    switchOffTrack = Color(0xFFDCE6E1),
    switchOffThumb = Color(0xFF7B9488),
    switchOffBorder = Color(0xFFB2C7BD),
    statusActiveGreen = Color(0xFF0E8C54),
    paperSheet = Color(0xFFFCFDFC),
    paperBrand = Color(0xFF0F6B43),
    paperInkLine = Color(0xFF98B3A6),
)

private val DarkLipiColors = LipiColors(
    primary = Color(0xFF28D388),           // Luminous Emerald Mint
    onPrimary = Color(0xFF042617),         // Dark Forest Text
    primaryContainer = Color(0xFF0C3B25),  // Deep Forest Container
    onPrimaryContainer = Color(0xFFB3F2D2),// Soft Mint Text
    background = Color(0xFF0A1410),        // Deep Obsidian Forest-Black
    surface = Color(0xFF12201A),           // Deep Emerald Surface Card
    outline = Color(0xFF2E4D3E),           // Dark Emerald Outline
    cardBorder = Color(0xFF1E352B),        // Dark Emerald Border
    ink = Color(0xFFE8F2EC),               // Soft Mint White Text
    muted = Color(0xFF8BA89B),             // Muted Sage Text
    amberContainer = Color(0xFF332412),    // Warm Dark Amber Tint
    onAmber = Color(0xFFF5C782),           // Luminous Amber Gold
    red = Color(0xFFE27B73),               // Soft Coral Red
    onError = Color(0xFF381210),
    imageWell = Color(0xFF192B23),         // Dark Emerald Image Well
    inkLine = Color(0xFF5C7D6E),
    success = Color(0xFF32DC92),           // Radiant Mint Success
    viewerBg = Color(0xFF060D0A),
    switchOffTrack = Color(0xFF1E352B),
    switchOffThumb = Color(0xFF8BA89B),
    switchOffBorder = Color(0xFF3B5E4F),
    statusActiveGreen = Color(0xFF32DC92),
    paperSheet = Color(0xFFFCFDFC),
    paperBrand = Color(0xFF0F6B43),
    paperInkLine = Color(0xFF5C7D6E),
)

/** Linearly interpolates all color tokens for a single-driver, 60/120 FPS theme transition. */
private fun lerp(start: LipiColors, stop: LipiColors, fraction: Float): LipiColors {
    val f = fraction.coerceIn(0f, 1f)
    return LipiColors(
        primary = lerp(start.primary, stop.primary, f),
        onPrimary = lerp(start.onPrimary, stop.onPrimary, f),
        primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, f),
        onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, f),
        background = lerp(start.background, stop.background, f),
        surface = lerp(start.surface, stop.surface, f),
        outline = lerp(start.outline, stop.outline, f),
        cardBorder = lerp(start.cardBorder, stop.cardBorder, f),
        ink = lerp(start.ink, stop.ink, f),
        muted = lerp(start.muted, stop.muted, f),
        amberContainer = lerp(start.amberContainer, stop.amberContainer, f),
        onAmber = lerp(start.onAmber, stop.onAmber, f),
        red = lerp(start.red, stop.red, f),
        onError = lerp(start.onError, stop.onError, f),
        imageWell = lerp(start.imageWell, stop.imageWell, f),
        inkLine = lerp(start.inkLine, stop.inkLine, f),
        success = lerp(start.success, stop.success, f),
        viewerBg = lerp(start.viewerBg, stop.viewerBg, f),
        switchOffTrack = lerp(start.switchOffTrack, stop.switchOffTrack, f),
        switchOffThumb = lerp(start.switchOffThumb, stop.switchOffThumb, f),
        switchOffBorder = lerp(start.switchOffBorder, stop.switchOffBorder, f),
        statusActiveGreen = lerp(start.statusActiveGreen, stop.statusActiveGreen, f),
        paperSheet = lerp(start.paperSheet, stop.paperSheet, f),
        paperBrand = lerp(start.paperBrand, stop.paperBrand, f),
        paperInkLine = lerp(start.paperInkLine, stop.paperInkLine, f),
    )
}

private val LocalLipiColors = staticCompositionLocalOf { LightLipiColors }

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

@Composable
fun LipiRxTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val themeProgress by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "themeProgress"
    )

    val lipi = remember(themeProgress) {
        if (themeProgress <= 0f) LightLipiColors
        else if (themeProgress >= 1f) DarkLipiColors
        else lerp(LightLipiColors, DarkLipiColors, themeProgress)
    }

    val colorScheme = remember(lipi, darkTheme) {
        if (darkTheme) {
            darkColorScheme(
                primary = lipi.primary,
                onPrimary = lipi.onPrimary,
                primaryContainer = lipi.primaryContainer,
                onPrimaryContainer = lipi.onPrimaryContainer,
                background = lipi.background,
                onBackground = lipi.ink,
                surface = lipi.surface,
                onSurface = lipi.ink,
                surfaceVariant = lipi.imageWell,
                onSurfaceVariant = lipi.muted,
                surfaceContainerLowest = lipi.background,
                surfaceContainerLow = lipi.background,
                surfaceContainer = lipi.surface,
                surfaceContainerHigh = lipi.surface,
                surfaceContainerHighest = lipi.imageWell,
                outline = lipi.outline,
                outlineVariant = lipi.cardBorder,
                error = lipi.red,
                onError = lipi.onError,
            )
        } else {
            lightColorScheme(
                primary = lipi.primary,
                onPrimary = lipi.onPrimary,
                primaryContainer = lipi.primaryContainer,
                onPrimaryContainer = lipi.onPrimaryContainer,
                background = lipi.background,
                onBackground = lipi.ink,
                surface = lipi.surface,
                onSurface = lipi.ink,
                surfaceVariant = lipi.imageWell,
                onSurfaceVariant = lipi.muted,
                surfaceContainerLowest = lipi.background,
                surfaceContainerLow = lipi.background,
                surfaceContainer = lipi.surface,
                surfaceContainerHigh = lipi.surface,
                surfaceContainerHighest = lipi.imageWell,
                outline = lipi.outline,
                outlineVariant = lipi.cardBorder,
                error = lipi.red,
                onError = lipi.onError,
            )
        }
    }

    CompositionLocalProvider(LocalLipiColors provides lipi) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
