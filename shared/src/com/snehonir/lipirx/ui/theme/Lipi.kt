package com.snehonir.lipirx.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalLipiColors = staticCompositionLocalOf { LightLipiColors }

/** Theme-aware color tokens. Use inside @Composable code, e.g. `Lipi.Primary`. */
object Lipi {
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.primary
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.onPrimary
    val PrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.primaryContainer
    val OnPrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.onPrimaryContainer
    val Background: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.surface
    val Outline: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.outline
    val CardBorder: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.cardBorder
    val Ink: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.ink
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.muted
    val AmberContainer: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.amberContainer
    val OnAmber: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.onAmber
    val Red: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.red
    val Error: Color @Composable @ReadOnlyComposable get() = Red
    val Danger: Color @Composable @ReadOnlyComposable get() = Red
    val OnError: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.onError
    val ImageWell: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.imageWell
    val InkLine: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.inkLine
    val Success: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.success
    val ViewerBg: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.viewerBg
    val SwitchOffTrack: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.switchOffTrack
    val SwitchOffThumb: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.switchOffThumb
    val SwitchOffBorder: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.switchOffBorder
    val StatusActiveGreen: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.statusActiveGreen
    val PaperSheet: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.paperSheet
    val PaperBrand: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.paperBrand
    val PaperInkLine: Color @Composable @ReadOnlyComposable get() = LocalLipiColors.current.paperInkLine
}
