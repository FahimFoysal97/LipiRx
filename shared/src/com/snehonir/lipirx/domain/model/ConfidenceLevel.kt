package com.snehonir.lipirx.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ConfidenceLevel(val rawValue: String) {
    @SerialName("high") HIGH("high"),
    @SerialName("medium") MEDIUM("medium"),
    @SerialName("low") LOW("low");

    val isHigh: Boolean get() = this == HIGH
    val isLow: Boolean get() = this == LOW

    companion object {
        fun fromValue(value: String): ConfidenceLevel {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.rawValue == clean } ?: LOW
        }
    }
}
