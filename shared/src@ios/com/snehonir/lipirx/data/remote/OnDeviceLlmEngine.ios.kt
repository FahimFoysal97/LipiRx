package com.snehonir.lipirx.data.remote

actual class OnDeviceLlmEngine actual constructor() {
    actual fun isModelInstalled(): Boolean = false
    actual fun getInstalledModelPath(): String? = null
    actual suspend fun runTextPrompt(promptText: String, modelName: String): Result<String> = Result.failure(IllegalStateException("On-device SLM not configured on iOS."))
    actual suspend fun runVisionOcr(imageByteArray: ByteArray, modelName: String): Result<String> = Result.failure(IllegalStateException("On-device SLM not configured on iOS."))
}
