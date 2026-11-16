package com.snehonir.lipirx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.snehonir.lipirx.domain.model.ThemeMode

data class LipiColors(
    val primary: Color,
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
    primary = Color(0xFF0B7A6E),
    primaryContainer = Color(0xFFE3F3F0),
    onPrimaryContainer = Color(0xFF075E54),
    background = Color(0xFFF4F7F6),
    surface = Color.White,
    outline = Color(0xFFD5E0DE),
    cardBorder = Color(0xFFE1E9E7),
    ink = Color(0xFF10242B),
    muted = Color(0xFF52666C),
    amberContainer = Color(0xFFFFF1DC),
    onAmber = Color(0xFF8A4B00),
    red = Color(0xFFB3261E),
    imageWell = Color(0xFFE9EFEE),
    inkLine = Color(0xFF9FB0C8),
    success = Color(0xFF1DB36B),
    viewerBg = Color(0xFF070C0F),
    switchOffTrack = Color(0xFFDCDFE0),
    switchOffThumb = Color(0xFF52666C),
    switchOffBorder = Color(0xFF9FB0C8),
    statusActiveGreen = Color(0xFF2E7D32),
)

private val DarkLipiColors = LipiColors(
    primary = Color(0xFF14B8A6),
    primaryContainer = Color(0xFF093D37),
    onPrimaryContainer = Color(0xFF99F6E4),
    background = Color(0xFF0B0F17),
    surface = Color(0xFF131C29),
    outline = Color(0xFF334155),
    cardBorder = Color(0xFF1F2B3B),
    ink = Color(0xFFF1F5F9),
    muted = Color(0xFF94A3B8),
    amberContainer = Color(0xFF3A2A0F),
    onAmber = Color(0xFFFBBF6A),
    red = Color(0xFFF87171),
    imageWell = Color(0xFF1A2434),
    inkLine = Color(0xFF64748B),
    success = Color(0xFF34D399),
    viewerBg = Color(0xFF000000),
    switchOffTrack = Color(0xFF1E293B),
    switchOffThumb = Color(0xFF94A3B8),
    switchOffBorder = Color(0xFF475569),
    statusActiveGreen = Color(0xFF4ADE80),
)

private val LocalLipiColors = staticCompositionLocalOf { LightLipiColors }

/** Theme-aware tokens. Use inside @Composable code, same as before. */
object Lipi {
    val Primary: Color @Composable get() = LocalLipiColors.current.primary
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

    val lipi = if (darkTheme) DarkLipiColors else LightLipiColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = lipi.primary,
            onPrimary = Color(0xFF042F2C),
            primaryContainer = lipi.primaryContainer,
            onPrimaryContainer = lipi.onPrimaryContainer,
            background = lipi.background,
            onBackground = lipi.ink,
            surface = lipi.surface,
            onSurface = lipi.ink,
            surfaceVariant = lipi.imageWell,
            onSurfaceVariant = lipi.muted,
            outline = lipi.outline,
            outlineVariant = lipi.cardBorder,
            error = lipi.red,
            onError = Color(0xFF2A0A0A),
        )
    } else {
        lightColorScheme(
            primary = lipi.primary,
            onPrimary = Color.White,
            primaryContainer = lipi.primaryContainer,
            onPrimaryContainer = lipi.onPrimaryContainer,
            background = lipi.background,
            onBackground = lipi.ink,
            surface = lipi.surface,
            onSurface = lipi.ink,
            surfaceVariant = lipi.imageWell,
            onSurfaceVariant = lipi.muted,
            outline = lipi.outline,
            outlineVariant = lipi.cardBorder,
            error = lipi.red,
        )
    }

    CompositionLocalProvider(LocalLipiColors provides lipi) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}