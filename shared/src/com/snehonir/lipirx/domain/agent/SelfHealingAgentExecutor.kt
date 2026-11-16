package com.snehonir.lipirx.domain.agent

import io.github.aakira.napier.Napier
import kotlinx.serialization.SerializationException

/**
 * Executes agent tool actions with an autonomous self-healing retry loop.
 * Catching serialization and schema failures, constructing targeted
 * repair prompts, and retrying up to [maxRetries] times.
 */
class SelfHealingAgentExecutor(
    private val maxRetries: Int = 2
) {
    suspend fun <T> executeWithSelfHealing(
        agentName: String,
        action: suspend (repairPrompt: String) -> Result<T>
    ): Result<T> {
        var currentAttempt = 0
        var lastException: Throwable? = null
        var currentRepairPrompt = ""

        while (currentAttempt <= maxRetries) {
            Napier.d("[$TAG] Executing agent '$agentName' (Attempt ${currentAttempt + 1}/${maxRetries + 1})...", tag = TAG)
            val result = action(currentRepairPrompt)
            if (result.isSuccess) {
                if (currentAttempt > 0) {
                    Napier.i(
                        "[$TAG] Agent '$agentName' successfully self-healed on retry attempt $currentAttempt.",
                        tag = TAG
                    )
                }
                return result
            }

            lastException = result.exceptionOrNull() ?: IllegalStateException("Unknown agent execution failure")
            currentAttempt++

            if (currentAttempt <= maxRetries) {
                currentRepairPrompt = buildRepairPrompt(lastException, currentAttempt)
                Napier.w(
                    "[$TAG] Agent '$agentName' failed attempt $currentAttempt/$maxRetries. " +
                            "Error: ${lastException.message}. Triggering self-repair loop with prompt: '$currentRepairPrompt'",
                    tag = TAG
                )
            }
        }

        return Result.failure(
            lastException ?: IllegalStateException("Self-healing exhausted after $maxRetries retries for agent '$agentName'")
        )
    }

    private fun buildRepairPrompt(error: Throwable, attempt: Int): String {
        return when (error) {
            is SerializationException -> {
                "CRITICAL JSON REPAIR (Attempt $attempt): Your previous output failed JSON parsing: '${error.message}'. " +
                        "Output ONLY valid, raw JSON matching the required schema. Do NOT wrap output in markdown code blocks, intros, or explanations."
            }
            is IllegalArgumentException -> {
                "SCHEMA REPAIR (Attempt $attempt): Missing or invalid required fields: '${error.message}'. Please supply valid non-empty data."
            }
            else -> {
                "EXECUTION REPAIR (Attempt $attempt): Previous step failed with error: '${error.message}'. Please re-evaluate input and fix formatting errors."
            }
        }
    }

    companion object {
        private const val TAG = "SelfHealingAgent"
    }
}
