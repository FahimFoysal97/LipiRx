package com.snehonir.lipirx.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.ui.SystemBarAppearance

private const val THEME_TRANSITION_MILLIS = 280

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

    SystemBarAppearance(darkTheme = darkTheme)

    val themeProgress by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = THEME_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
        label = "themeProgress",
    )

    val lipiColors = remember(themeProgress) {
        when {
            themeProgress <= 0f -> LightLipiColors
            themeProgress >= 1f -> DarkLipiColors
            else -> lerp(LightLipiColors, DarkLipiColors, themeProgress)
        }
    }

    val colorScheme = remember(lipiColors, darkTheme) {
        lipiColors.toMaterialColorScheme(darkTheme)
    }

    CompositionLocalProvider(LocalLipiColors provides lipiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LipiTypography,
            content = content,
        )
    }
}
