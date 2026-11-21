package com.snehonir.lipirx.navigation

import kotlinx.serialization.Serializable

sealed interface NavKey

@Serializable
sealed interface AppNavKey : NavKey {
    fun toRouteString(): String = when (this) {
        Home -> "home"
        ImagePreview -> "image_preview"
        AnalysisLoading -> "analysis_loading"
        Results -> "results"
        History -> "history"
        Telemetry -> "telemetry"
        Settings -> "settings"
    }

    companion object {
        fun fromRouteString(route: String): AppNavKey = when (route) {
            "image_preview" -> ImagePreview
            "analysis_loading" -> AnalysisLoading
            "results" -> Results
            "history" -> History
            "telemetry" -> Telemetry
            "settings" -> Settings
            else -> Home
        }
    }

    @Serializable
    data object Home : AppNavKey

    @Serializable
    data object ImagePreview : AppNavKey

    @Serializable
    data object AnalysisLoading : AppNavKey

    @Serializable
    data object Results : AppNavKey

    @Serializable
    data object History : AppNavKey

    @Serializable
    data object Telemetry : AppNavKey

    @Serializable
    data object Settings : AppNavKey
}
