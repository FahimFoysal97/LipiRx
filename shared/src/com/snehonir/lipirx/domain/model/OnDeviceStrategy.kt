package com.snehonir.lipirx.domain.model

import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.strategy_cloud_only_desc
import com.snehonir.lipirx.resources.strategy_cloud_only_explanation
import com.snehonir.lipirx.resources.strategy_cloud_only_name
import com.snehonir.lipirx.resources.strategy_hybrid_fallback_desc
import com.snehonir.lipirx.resources.strategy_hybrid_fallback_explanation
import com.snehonir.lipirx.resources.strategy_hybrid_fallback_name
import com.snehonir.lipirx.resources.strategy_on_device_first_desc
import com.snehonir.lipirx.resources.strategy_on_device_first_explanation
import com.snehonir.lipirx.resources.strategy_on_device_first_name
import com.snehonir.lipirx.resources.strategy_on_device_only_desc
import com.snehonir.lipirx.resources.strategy_on_device_only_explanation
import com.snehonir.lipirx.resources.strategy_on_device_only_name
import org.jetbrains.compose.resources.StringResource

enum class OnDeviceStrategy(
    val id: String,
    val displayNameRes: StringResource,
    val descriptionRes: StringResource,
    val explanationRes: StringResource,
    @Deprecated("Use displayNameRes with stringResource()") val displayName: String,
    @Deprecated("Use descriptionRes with stringResource()") val description: String
) {
    HYBRID_FALLBACK(
        id = "hybrid_fallback",
        displayNameRes = Res.string.strategy_hybrid_fallback_name,
        descriptionRes = Res.string.strategy_hybrid_fallback_desc,
        explanationRes = Res.string.strategy_hybrid_fallback_explanation,
        displayName = "Hybrid Fallback",
        description = "Cloud First -> Automatic On-Device SLM Fallback on offline/error"
    ),
    ON_DEVICE_FIRST(
        id = "on_device_first",
        displayNameRes = Res.string.strategy_on_device_first_name,
        descriptionRes = Res.string.strategy_on_device_first_desc,
        explanationRes = Res.string.strategy_on_device_first_explanation,
        displayName = "On-Device First",
        description = "Local NPU First -> Cloud Escalation for complex safety"
    ),
    ON_DEVICE_ONLY(
        id = "on_device_only",
        displayNameRes = Res.string.strategy_on_device_only_name,
        descriptionRes = Res.string.strategy_on_device_only_desc,
        explanationRes = Res.string.strategy_on_device_only_explanation,
        displayName = "On-Device Only",
        description = "Strict 100% Offline / Local Execution Only"
    ),
    CLOUD_ONLY(
        id = "cloud_only",
        displayNameRes = Res.string.strategy_cloud_only_name,
        descriptionRes = Res.string.strategy_cloud_only_desc,
        explanationRes = Res.string.strategy_cloud_only_explanation,
        displayName = "Cloud Only",
        description = "Disable On-Device SLM -> Cloud AI Only"
    );

    companion object {
        val ALL: List<OnDeviceStrategy> = entries.toList()

        fun fromId(id: String): OnDeviceStrategy =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: HYBRID_FALLBACK
    }
}
