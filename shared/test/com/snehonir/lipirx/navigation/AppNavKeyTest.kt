package com.snehonir.lipirx.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavKeyTest {

    @Test
    fun testRouteStringSerializationAndDeserialization() {
        val keys = listOf(
            AppNavKey.Home,
            AppNavKey.ImagePreview,
            AppNavKey.AnalysisLoading,
            AppNavKey.Results,
            AppNavKey.History,
            AppNavKey.Telemetry,
            AppNavKey.Settings
        )

        for (key in keys) {
            val route = key.toRouteString()
            val restored = AppNavKey.fromRouteString(route)
            assertEquals(key, restored, "Failed to serialize and restore $key via route '$route'")
        }
    }

    @Test
    fun testUnknownRouteDefaultsToHome() {
        val restored = AppNavKey.fromRouteString("unknown_destination_key")
        assertEquals(AppNavKey.Home, restored)
    }
}
