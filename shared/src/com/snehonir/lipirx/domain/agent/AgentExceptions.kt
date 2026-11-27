package com.snehonir.lipirx.domain.agent

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

class AiRateLimitException(message: String, cause: Throwable? = null) : Exception(message, cause)
class AiAuthenticationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NetworkUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)
class AiModelOverloadedException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun Throwable.isCancellation(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        if (cause is CancellationException) return true
        cause = cause.cause
    }
    return false
}

fun toTypedAiException(ex: Throwable): Throwable {
    if (ex.isCancellation() ||
        ex is AiRateLimitException ||
        ex is AiAuthenticationException ||
        ex is NetworkUnavailableException ||
        ex is AiModelOverloadedException ||
        ex is AiOutputFormatException ||
        ex is SerializationException
    ) {
        return ex
    }
    val msg = ex.message ?: ""
    return when {
        msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("quota", ignoreCase = true) || msg.contains("rate_limit", ignoreCase = true) ->
            AiRateLimitException(msg, ex)
        msg.contains("401") || msg.contains("403") || msg.contains("API key", ignoreCase = true) || msg.contains("UNAUTHENTICATED", ignoreCase = true) || msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
            AiAuthenticationException(msg, ex)
        msg.contains("503") || msg.contains("529") || msg.contains("overloaded", ignoreCase = true) || msg.contains("UNAVAILABLE", ignoreCase = true) ->
            AiModelOverloadedException(msg, ex)
        msg.contains("UnknownHostException", ignoreCase = true) || msg.contains("Unable to resolve host", ignoreCase = true) || msg.contains("No address associated with hostname", ignoreCase = true) || msg.contains("ConnectException", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
            NetworkUnavailableException(msg, ex)
        else -> ex
    }
}

sealed class PipelineFailureReason(val displayName: String, val isRetryable: Boolean) {
    data class NetworkTimeout(val details: String) : PipelineFailureReason("Network Timeout", isRetryable = true)
    data class RateLimitExceeded(val details: String) : PipelineFailureReason("Rate Limit Exceeded (429)", isRetryable = true)
    data class InvalidCredentials(val details: String) : PipelineFailureReason("Invalid API Key / Credentials", isRetryable = false)
    data class SchemaParsingFailed(val details: String) : PipelineFailureReason("Output Schema Formatting Error", isRetryable = true)
    data class ServiceUnavailable(val details: String) : PipelineFailureReason("Service Unavailable (503)", isRetryable = true)
    data class GenericError(val details: String) : PipelineFailureReason("Execution Error", isRetryable = false)

    companion object {
        fun fromThrowable(throwable: Throwable): PipelineFailureReason {
            return when (val typed = toTypedAiException(throwable)) {
                is AiRateLimitException -> RateLimitExceeded(typed.message ?: "")
                is AiAuthenticationException -> InvalidCredentials(typed.message ?: "")
                is AiModelOverloadedException -> ServiceUnavailable(typed.message ?: "")
                is NetworkUnavailableException -> NetworkTimeout(typed.message ?: "")
                is AiOutputFormatException, is SerializationException -> SchemaParsingFailed(typed.message ?: "")
                else -> GenericError(typed.message ?: "")
            }
        }
    }
}

class AiOutputFormatException(
    val rawOutput: String,
    cause: Throwable
) : Exception("AI output did not match expected prescription schema", cause)
