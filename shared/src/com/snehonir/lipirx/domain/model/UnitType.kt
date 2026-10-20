package com.snehonir.lipirx.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UnitType(val rawValue: String) {
    @SerialName("pcs") PCS("pcs"),
    @SerialName("tablets") TABLETS("tablets"),
    @SerialName("capsules") CAPSULES("capsules"),
    @SerialName("tube") TUBE("tube"),
    @SerialName("bottle") BOTTLE("bottle"),
    @SerialName("sachet") SACHET("sachet"),
    @SerialName("vial") VIAL("vial"),
    @SerialName("soap") SOAP("soap");

    companion object {
        fun fromValue(value: String): UnitType {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.rawValue == clean } ?: PCS
        }
    }
}
