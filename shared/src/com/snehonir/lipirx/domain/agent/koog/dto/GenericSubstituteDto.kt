package com.snehonir.lipirx.domain.agent.koog.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class GenericSubstituteDto(
    val id: Int? = null,
    val genericName: String? = null,
    val similarBrands: List<String>? = null
)
