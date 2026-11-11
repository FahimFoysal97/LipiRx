package com.snehonir.lipirx.domain.model

enum class OnDeviceStrategy(
    val id: String,
    val displayName: String,
    val description: String
) {
    HYBRID_FALLBACK(
        id = "hybrid_fallback",
        displayName = "Hybrid Fallback",
        description = "Cloud First -> Automatic On-Device SLM Fallback on offline/error"
    ),
    ON_DEVICE_FIRST(
        id = "on_device_first",
        displayName = "On-Device First",
        description = "Local NPU First -> Cloud Escalation for complex safety"
    ),
    ON_DEVICE_ONLY(
        id = "on_device_only",
        displayName = "On-Device Only",
        description = "Strict 100% Offline / Local Execution Only"
    ),
    CLOUD_ONLY(
        id = "cloud_only",
        displayName = "Cloud Only",
        description = "Disable On-Device SLM -> Cloud AI Only"
    );

    companion object {
        val ALL: List<OnDeviceStrategy> = entries.toList()

        fun fromId(id: String): OnDeviceStrategy =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: HYBRID_FALLBACK
    }
}
