package com.snehonir.lipirx.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DosageForm(val rawValue: String) {
    @SerialName("tablet") TABLET("tablet"),
    @SerialName("capsule") CAPSULE("capsule"),
    @SerialName("syrup") SYRUP("syrup"),
    @SerialName("suspension") SUSPENSION("suspension"),
    @SerialName("drops") DROPS("drops"),
    @SerialName("cream") CREAM("cream"),
    @SerialName("ointment") OINTMENT("ointment"),
    @SerialName("gel") GEL("gel"),
    @SerialName("lotion") LOTION("lotion"),
    @SerialName("soap") SOAP("soap"),
    @SerialName("injection") INJECTION("injection"),
    @SerialName("sachet") SACHET("sachet"),
    @SerialName("inhaler") INHALER("inhaler"),
    @SerialName("other") OTHER("other"),
    @SerialName("unknown") UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): DosageForm {
            val clean = value.trim().lowercase()
            return entries.firstOrNull { it.rawValue == clean } ?: UNKNOWN
        }
    }
}
