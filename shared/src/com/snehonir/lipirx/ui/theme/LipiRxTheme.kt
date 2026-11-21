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
)

private val LightLipiColors = LipiColors(
    primary = Color(0xFF267A70),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F1EE),
    onPrimaryContainer = Color(0xFF1F5F57),
    background = Color(0xFFF3F5F2),
    surface = Color(0xFFFBFCFB),
    outline = Color(0xFFD9E1DE),
    cardBorder = Color(0xFFE5EBE8),
    ink = Color(0xFF24343A),
    muted = Color(0xFF66777B),
    amberContainer = Color(0xFFFBEFDA),
    onAmber = Color(0xFF7A5420),
    red = Color(0xFFB8463F),
    onError = Color(0xFFFFFFFF),
    imageWell = Color(0xFFECF0EE),
    inkLine = Color(0xFFA9B7C6),
    success = Color(0xFF3FAE7E),
    viewerBg = Color(0xFF161C1F),
    switchOffTrack = Color(0xFFE0E4E3),
    switchOffThumb = Color(0xFF8A999C),
    switchOffBorder = Color(0xFFB5C1C4),
    statusActiveGreen = Color(0xFF4C8F52),
)

private val DarkLipiColors = LipiColors(
    primary = Color(0xFF5FB8AA),
    onPrimary = Color(0xFF0F2E2A),
    primaryContainer = Color(0xFF1F3B38),
    onPrimaryContainer = Color(0xFFBFE3DC),
    background = Color(0xFF141A1C),
    surface = Color(0xFF1B2326),
    outline = Color(0xFF3A4749),
    cardBorder = Color(0xFF263033),
    ink = Color(0xFFDDE5E3),
    muted = Color(0xFF9AA8A6),
    amberContainer = Color(0xFF35291A),
    onAmber = Color(0xFFE6C48A),
    red = Color(0xFFE59A94),
    onError = Color(0xFF3A1412),
    imageWell = Color(0xFF222C2F),
    inkLine = Color(0xFF6E7F83),
    success = Color(0xFF6FCBA0),
    viewerBg = Color(0xFF0E1315),
    switchOffTrack = Color(0xFF263033),
    switchOffThumb = Color(0xFF9AA8A6),
    switchOffBorder = Color(0xFF4A585B),
    statusActiveGreen = Color(0xFF86C98F),
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
