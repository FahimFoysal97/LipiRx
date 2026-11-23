package com.snehonir.lipirx.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.snehonir.lipirx.domain.model.ThemeMode

/**
 * Entry point for the LipiRx Design System theme.
 * Handles automatic dark/light theme progress interpolation and Material 3 composition.
 */
@Composable
fun LipiRxTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val themeProgress by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "themeProgress",
    )

    val lipi = remember(themeProgress) {
        when {
            themeProgress <= 0f -> LightLipiColors
            themeProgress >= 1f -> DarkLipiColors
            else -> lerpLipiColors(LightLipiColors, DarkLipiColors, themeProgress)
        }
    }

    val colorScheme = remember(lipi, darkTheme) {
        lipi.toMaterialColorScheme(darkTheme)
    }

    CompositionLocalProvider(LocalLipiColors provides lipi) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
