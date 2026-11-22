package com.snehonir.lipirx.data.remote.service

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.snehonir.lipirx.data.local.scanInstalledOnDeviceModels
import io.github.aakira.napier.Napier
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

private var androidContextRef: Context? = null

fun initOnDeviceEngineContext(context: Context) {
    androidContextRef = context.applicationContext
}

actual class OnDeviceLlmEngine actual constructor() {
    private val executor = Executors.newSingleThreadExecutor()
    private val dispatcher = executor.asCoroutineDispatcher()

    private var currentEngine: Engine? = null

    actual fun isModelInstalled(): Boolean {
        return getInstalledModelPath() != null
    }

    actual fun getInstalledModelPath(): String? {
        val models = scanInstalledOnDeviceModels()
        return models.firstOrNull()?.name
    }

    actual suspend fun runTextPrompt(
        promptText: String,
        modelName: String
    ): Result<String> = withContext(dispatcher) {
        runCatching {
            val engine = getOrInitEngine(modelName)
            val conversation = engine.createConversation()
            val response = conversation.sendMessage(promptText)
            response.toString()
        }
    }

    actual suspend fun runVisionOcr(
        imageByteArray: ByteArray,
        modelName: String
    ): Result<String> = withContext(dispatcher) {
        runCatching {
            val engine = getOrInitEngine(modelName)
            val conversation = engine.createConversation()
            val response = conversation.sendMessage("Transcribe handwritten prescription image:")
            response.toString()
        }
    }

    private fun getOrInitEngine(modelName: String): Engine {
        val existing = currentEngine
        if (existing != null) return existing

        val modelPath = getInstalledModelPath() ?: throw IllegalStateException("No installed local SLM model file found on device storage.")
        val engine = Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU()
            )
        )
        currentEngine = engine
        return engine
    }

    companion object {
        private const val TAG = "OnDeviceLlmEngine"
    }
}
