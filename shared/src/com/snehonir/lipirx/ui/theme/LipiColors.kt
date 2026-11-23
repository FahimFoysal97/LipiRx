package com.snehonir.lipirx.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Semantic color tokens for LipiRx.
 *
 * Concrete values live in [LightLipiColors] / [DarkLipiColors] (see LipiPalette.kt).
 * Read them in composables through the [Lipi] accessor object.
 */
@Immutable
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

/** Linearly interpolates every token, used for smooth light/dark transitions. */
internal fun lerp(start: LipiColors, stop: LipiColors, fraction: Float): LipiColors {
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
