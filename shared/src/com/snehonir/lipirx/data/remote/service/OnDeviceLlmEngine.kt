package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.data.remote.prompt.SINGLE_CALL_PROMPT

expect class OnDeviceLlmEngine() {
    fun isModelInstalled(): Boolean
    fun getInstalledModelPath(): String?
    suspend fun runTextPrompt(promptText: String, modelName: String): Result<String>
    suspend fun runVisionOcr(imageByteArray: ByteArray, modelName: String): Result<String>
}
