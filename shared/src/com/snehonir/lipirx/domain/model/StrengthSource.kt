package com.snehonir.lipirx.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StrengthSource(val rawValue: String) {
    @SerialName("written") WRITTEN("written"),
    @SerialName("assumed") ASSUMED("assumed"),
    @SerialName("") NONE("");

    companion object {
        fun fromValue(value: String): StrengthSource {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.rawValue == clean } ?: NONE
        }
    }
}
