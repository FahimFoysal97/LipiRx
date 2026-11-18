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

// Soft light: warm off-white surfaces (no pure white), slightly desaturated teal,
// softer ink instead of near-black.
// primary 0xFF267A70: ~5:1 against white, so small bold labels stay readable.
// red 0xFFB8463F: ~5:1 against the surface, for small error text.
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

// Soft dark: lifted charcoal (no near-black), muted pastel accents,
// off-white text instead of bright white to reduce glare.
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

private val LocalLipiColors = staticCompositionLocalOf { LightLipiColors }

/** Theme-aware tokens. Use inside @Composable code, same as before. */
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

    val lipi = if (darkTheme) DarkLipiColors else LightLipiColors

    val colorScheme = if (darkTheme) {
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
            // Dialogs, menus and bottom sheets read these container roles,
            // so they must be set or Material falls back to its default purple-gray.
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

    CompositionLocalProvider(LocalLipiColors provides lipi) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}