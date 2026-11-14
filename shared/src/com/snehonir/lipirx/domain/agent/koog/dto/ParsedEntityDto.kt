package com.snehonir.lipirx.domain.agent.koog.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ParsedEntityDto(
    val id: Int? = null,
    val medicineName: String? = null,
    val dosageForm: String? = null,
    val strengthValue: String? = null,
    val strengthUnit: String? = null,
    val strengthNote: String? = null,
    val schedule: String? = null,
    val units: Int? = null,
    val unitType: String? = null
)
