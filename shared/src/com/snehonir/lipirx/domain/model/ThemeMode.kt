package com.snehonir.lipirx.domain.model

import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.theme_dark
import com.snehonir.lipirx.resources.theme_light
import com.snehonir.lipirx.resources.theme_system_default
import org.jetbrains.compose.resources.StringResource

enum class ThemeMode(
    val id: String,
    val displayNameRes: StringResource
) {
    SYSTEM("system", Res.string.theme_system_default),
    DARK("dark", Res.string.theme_dark),
    LIGHT("light", Res.string.theme_light);

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: SYSTEM
        }
    }
}
