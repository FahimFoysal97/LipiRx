package com.snehonir.lipirx.domain.agent

sealed class PipelineFailureReason(val displayName: String, val isRetryable: Boolean) {
    data class NetworkTimeout(val details: String) : PipelineFailureReason("Network Timeout", isRetryable = true)
    data class RateLimitExceeded(val details: String) : PipelineFailureReason("Rate Limit Exceeded (429)", isRetryable = true)
    data class InvalidCredentials(val details: String) : PipelineFailureReason("Invalid API Key / Credentials", isRetryable = false)
    data class SchemaParsingFailed(val details: String) : PipelineFailureReason("Output Schema Formatting Error", isRetryable = true)
    data class ServiceUnavailable(val details: String) : PipelineFailureReason("Service Unavailable (503)", isRetryable = true)
    data class GenericError(val details: String) : PipelineFailureReason("Execution Error", isRetryable = false)

    companion object {
        fun fromThrowable(throwable: Throwable): PipelineFailureReason {
            val msg = throwable.message ?: ""
            return when {
                msg.contains("429") || msg.contains("quota", ignoreCase = true) || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ->
                    RateLimitExceeded(msg)
                msg.contains("401") || msg.contains("403") || msg.contains("API key", ignoreCase = true) ->
                    InvalidCredentials(msg)
                msg.contains("503") || msg.contains("overloaded", ignoreCase = true) || msg.contains("UNAVAILABLE", ignoreCase = true) ->
                    ServiceUnavailable(msg)
                msg.contains("timeout", ignoreCase = true) || msg.contains("UnknownHostException", ignoreCase = true) ->
                    NetworkTimeout(msg)
                msg.contains("JSON", ignoreCase = true) || msg.contains("parse", ignoreCase = true) || msg.contains("decode", ignoreCase = true) ->
                    SchemaParsingFailed(msg)
                else -> GenericError(msg)
            }
        }
    }
}

class AiOutputFormatException(
    val rawOutput: String,
    cause: Throwable
) : Exception("AI output did not match expected prescription schema", cause)
