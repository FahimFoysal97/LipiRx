package com.snehonir.lipirx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Lipi {
    val Primary = Color(0xFF0B7A6E)
    val PrimaryContainer = Color(0xFFE3F3F0)
    val OnPrimaryContainer = Color(0xFF075E54)
    val Background = Color(0xFFF4F7F6)
    val Surface = Color.White
    val Outline = Color(0xFFD5E0DE)
    val CardBorder = Color(0xFFE1E9E7)
    val Ink = Color(0xFF10242B)
    val Muted = Color(0xFF52666C)
    val AmberContainer = Color(0xFFFFF1DC)
    val OnAmber = Color(0xFF8A4B00)
    val Red = Color(0xFFB3261E)
    val Error = Red
    val Danger = Red
    val ImageWell = Color(0xFFE9EFEE)
    val InkLine = Color(0xFF9FB0C8)
    val Success = Color(0xFF1DB36B)
    val ViewerBg = Color(0xFF070C0F)
    val SwitchOffTrack = Color(0xFFDCDFE0)
    val SwitchOffThumb = Color(0xFF52666C)
    val SwitchOffBorder = Color(0xFF9FB0C8)
    val StatusActiveGreen = Color(0xFF2E7D32)
}

@Composable
fun LipiRxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Lipi.Primary,
            onPrimary = Color.White,
            primaryContainer = Lipi.PrimaryContainer,
            onPrimaryContainer = Lipi.OnPrimaryContainer,
            background = Lipi.Background,
            surface = Lipi.Surface,
            onSurface = Lipi.Ink,
            onSurfaceVariant = Lipi.Muted,
            outline = Lipi.Outline,
            error = Lipi.Error,
        ),
        typography = Typography(),
        content = content,
    )
}
