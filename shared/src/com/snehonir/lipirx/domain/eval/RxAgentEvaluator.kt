package com.snehonir.lipirx.domain.eval

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

class RxAgentEvaluator(
    private val supervisorEngine: KoogRxSupervisorEngine
) {
    suspend fun runEvaluationSuite(
        testCases: List<RxEvalTestCase> = getDefaultBenchmarkCases()
    ): EvalSummaryReport {
        Napier.i("[$TAG] Starting Automated Multi-Agent Evaluation Suite (${testCases.size} test cases)...", tag = TAG)
        val metricsList = mutableListOf<EvalMetrics>()

        for (testCase in testCases) {
            val startTime = getPlatformEpochMillis()

            val result = supervisorEngine.runOrchestration(
                input = testCase.input,
                initialItems = testCase.initialItems
            )
            val duration = getPlatformEpochMillis() - startTime

            if (result.isSuccess) {
                val orchestrationResult = result.getOrThrow()
                val extractedItems = orchestrationResult.items
                val turnCount = orchestrationResult.executionHistory.size

                val matchedCount = countMatchedMedicines(extractedItems, testCase.expectedMedicines)
                val accuracy = if (testCase.expectedMedicines.isNotEmpty()) {
                    matchedCount.toFloat() / testCase.expectedMedicines.size.toFloat()
                } else 1.0f

                val passed = accuracy >= AppConstants.EVAL_ACCURACY_THRESHOLD &&
                        turnCount <= testCase.maxAllowedTurns &&
                        (!orchestrationResult.isSuspendedForApproval || testCase.allowPharmacistSuspension)

                metricsList.add(
                    EvalMetrics(
                        testCaseId = testCase.id,
                        passed = passed,
                        extractionAccuracy = accuracy,
                        totalTurns = turnCount,
                        executionTimeMs = duration,
                        failureReason = if (!passed) {
                            if (orchestrationResult.isSuspendedForApproval && !testCase.allowPharmacistSuspension) {
                                "Pipeline suspended for pharmacist review (${orchestrationResult.approvalReason})"
                            } else {
                                "Accuracy below threshold ($accuracy < ${AppConstants.EVAL_ACCURACY_THRESHOLD}) or turn count exceeded ($turnCount > ${testCase.maxAllowedTurns})"
                            }
                        } else null
                    )
                )
            } else {
                metricsList.add(
                    EvalMetrics(
                        testCaseId = testCase.id,
                        passed = false,
                        extractionAccuracy = 0.0f,
                        totalTurns = 0,
                        executionTimeMs = duration,
                        failureReason = result.exceptionOrNull()?.message ?: "Pipeline execution failed"
                    )
                )
            }
        }

        val passedCount = metricsList.count { it.passed }
        val avgAccuracy = if (metricsList.isNotEmpty()) metricsList.map { it.extractionAccuracy }.average().toFloat() else 0f
        val avgLatency = if (metricsList.isNotEmpty()) metricsList.map { it.executionTimeMs }.average().toLong() else 0L

        val report = EvalSummaryReport(
            totalTests = metricsList.size,
            passedCount = passedCount,
            avgAccuracyPercentage = avgAccuracy * 100f,
            avgLatencyMs = avgLatency,
            results = metricsList
        )

        logReportSummary(report)
        return report
    }

    private fun countMatchedMedicines(
        extracted: List<MedicineItem>,
        expected: List<ExpectedMedicine>
    ): Int {
        if (expected.isEmpty()) return 1
        return expected.count { exp ->
            extracted.any { item ->
                item.medicineName.contains(exp.brandName, ignoreCase = true) ||
                        (exp.expectedGeneric.isNotBlank() && item.genericName.contains(exp.expectedGeneric, ignoreCase = true))
            }
        }
    }

    private fun logReportSummary(report: EvalSummaryReport) {
        val summaryText = buildString {
            appendLine()
            appendLine("==========================================================================")
            appendLine("📊 MULTI-AGENT AUTOMATED EVALUATION REPORT")
            appendLine("==========================================================================")
            appendLine("  Total Tests Run : ${report.totalTests}")
            appendLine("  Passed          : ${report.passedCount} / ${report.totalTests}")
            appendLine("  Avg Accuracy    : ${report.avgAccuracyPercentage}%")
            appendLine("  Avg Latency     : ${report.avgLatencyMs} ms")
            appendLine("--------------------------------------------------------------------------")
            report.results.forEach { res ->
                val status = if (res.passed) "✅ PASS" else "❌ FAIL"
                appendLine("  [$status] ${res.testCaseId} -> Accuracy: ${(res.extractionAccuracy * 100).toInt()}% | Turns: ${res.totalTurns} | Time: ${res.executionTimeMs}ms")
                if (res.failureReason != null) {
                    appendLine("      Reason: ${res.failureReason}")
                }
            }
            appendLine("==========================================================================")
        }
        Napier.i(summaryText, tag = TAG)
    }

    companion object {
        private const val TAG = "RxAgentEvaluator"

        fun getDefaultBenchmarkCases(): List<RxEvalTestCase> {
            return listOf(
                RxEvalTestCase(
                    id = "BENCH_001_PARSING_AND_SUBSTITUTES",
                    description = "Benchmark multi-agent entity parsing, generic matching, and safety audit",
                    input = TranscriptionInput(
                        imageByteArray = ByteArray(0),
                        selectedModel = AiProvider.Gemini.defaultModel,
                        provider = AiProvider.Gemini
                    ),
                    initialItems = listOf(
                        MedicineItem(medicineName = "Tab Napa 500mg", schedule = "1+0+1", units = 10, unitType = "tablets"),
                        MedicineItem(medicineName = "Cap Maxpro 20mg", schedule = "1+0+0", units = 14, unitType = "capsules")
                    ),
                    expectedItemsCount = 2,
                    expectedMedicines = listOf(
                        ExpectedMedicine(brandName = "Napa", expectedStrength = "500mg", expectedGeneric = "Paracetamol"),
                        ExpectedMedicine(brandName = "Maxpro", expectedStrength = "20mg", expectedGeneric = "Esomeprazole")
                    ),
                    maxAllowedTurns = 6,
                    allowPharmacistSuspension = true
                )
            )
        }
    }
}
