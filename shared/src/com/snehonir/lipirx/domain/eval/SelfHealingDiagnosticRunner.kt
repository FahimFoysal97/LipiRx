package com.snehonir.lipirx.domain.eval

import com.snehonir.lipirx.domain.agent.SelfHealingAgentExecutor
import io.github.aakira.napier.Napier
import kotlinx.serialization.SerializationException

object SelfHealingDiagnosticRunner {
    private const val TAG = "SelfHealingDiagnostic"

    suspend fun runDiagnostic(): Boolean {
        val executor = SelfHealingAgentExecutor(maxRetries = 2)
        var attempts = 0
        var capturedRepairPrompt = ""

        val result = executor.executeWithSelfHealing("diagnostic_parser_agent") { repairPrompt ->
            attempts++
            if (attempts == 1) {
                Result.failure(SerializationException("Unexpected JSON token '{' at offset 18"))
            } else {
                capturedRepairPrompt = repairPrompt
                Result.success("Parsed 2 medicines successfully after repair")
            }
        }

        val passed = result.isSuccess && attempts == 2 && capturedRepairPrompt.contains("CRITICAL JSON REPAIR")

        if (passed) {
            Napier.i("[$TAG] Self-Healing Patching Diagnostic: PASSED ✅ (1 failure -> 1 repair retry -> Success)", tag = TAG)
        } else {
            Napier.e("[$TAG] Self-Healing Patching Diagnostic: FAILED ❌", tag = TAG)
        }

        return passed
    }
}
