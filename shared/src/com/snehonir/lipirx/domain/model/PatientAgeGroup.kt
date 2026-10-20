package com.snehonir.lipirx.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PatientAgeGroup(val rawValue: String) {
    @SerialName("infant") INFANT("infant"),
    @SerialName("child") CHILD("child"),
    @SerialName("adult") ADULT("adult"),
    @SerialName("") UNKNOWN("");

    companion object {
        fun fromValue(value: String): PatientAgeGroup {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.rawValue == clean } ?: UNKNOWN
        }
    }
}
