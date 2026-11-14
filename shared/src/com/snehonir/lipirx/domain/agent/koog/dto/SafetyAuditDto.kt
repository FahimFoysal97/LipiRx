package com.snehonir.lipirx.domain.agent.koog.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SafetyAuditDto(
    val id: Int? = null,
    val needsReview: Boolean? = null,
    val possibleMisreading: String? = null,
    val confidence: String? = null,
    val enrichmentConfidence: String? = null,
    val auditWarning: String? = null
)
