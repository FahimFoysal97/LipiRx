package com.snehonir.lipirx.data.remote

expect class OnDeviceLlmEngine() {
    fun isModelInstalled(): Boolean
    fun getInstalledModelPath(): String?
    suspend fun runTextPrompt(promptText: String, modelName: String): Result<String>
    suspend fun runVisionOcr(imageByteArray: ByteArray, modelName: String): Result<String>
}
