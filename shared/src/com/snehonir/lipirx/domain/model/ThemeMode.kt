package com.snehonir.lipirx.domain.model

enum class ThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromId(id: String?): ThemeMode {
            return entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: SYSTEM
        }
    }
}
