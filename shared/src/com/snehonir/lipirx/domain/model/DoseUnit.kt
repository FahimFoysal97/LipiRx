package com.snehonir.lipirx.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DoseUnit(val rawValue: String) {
    @SerialName("tablet") TABLET("tablet"),
    @SerialName("capsule") CAPSULE("capsule"),
    @SerialName("ml") ML("ml"),
    @SerialName("drop") DROP("drop"),
    @SerialName("tsp") TSP("tsp"),
    @SerialName("puff") PUFF("puff"),
    @SerialName("sachet") SACHET("sachet"),
    @SerialName("application") APPLICATION("application"),
    @SerialName("") NONE("");

    companion object {
        fun fromValue(value: String): DoseUnit {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.rawValue == clean } ?: NONE
        }
    }
}
