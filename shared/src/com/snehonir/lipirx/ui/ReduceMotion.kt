package com.snehonir.lipirx.ui

import androidx.compose.runtime.Composable

/**
 * True when the user asked the OS to cut down on motion. Loading animations (scanner, avatar,
 * agent icons, indeterminate bars) stop when this is true.
 *
 * Put this file in commonMain. Add one `actual` per target (Android and iOS are provided; for
 * desktop or web, `actual @Composable fun rememberReduceMotion(): Boolean = false`).
 */
@Composable
expect fun rememberReduceMotion(): Boolean