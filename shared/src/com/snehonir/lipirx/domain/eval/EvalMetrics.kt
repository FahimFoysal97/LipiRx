package com.snehonir.lipirx.domain.eval

import kotlinx.serialization.Serializable

@Serializable
data class EvalMetrics(
    val testCaseId: String,
    val passed: Boolean,
    val extractionAccuracy: Float,
    val totalTurns: Int,
    val executionTimeMs: Long,
    val jsonParseErrorsCount: Int = 0,
    val failureReason: String? = null
)

@Serializable
data class EvalSummaryReport(
    val totalTests: Int,
    val passedCount: Int,
    val avgAccuracyPercentage: Float,
    val avgLatencyMs: Long,
    val results: List<EvalMetrics>
)
