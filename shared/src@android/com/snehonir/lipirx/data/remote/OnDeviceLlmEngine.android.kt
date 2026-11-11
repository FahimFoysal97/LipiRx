package com.snehonir.lipirx.data.remote

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
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
    private var currentConversation: Conversation? = null

    actual fun isModelInstalled(): Boolean {
        return getInstalledModelPath() != null
    }

    actual fun getInstalledModelPath(): String? {
        val context = androidContextRef ?: return null
        val dirsToScan = listOfNotNull(
            context.filesDir,
            context.getExternalFilesDir(null)
        )

        dirsToScan.forEach { dir ->
            dir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".litertlm", ignoreCase = true) || file.name.endsWith(".tflite", ignoreCase = true))) {
                    return file.absolutePath
                }
            }
        }
        return null
    }

    actual suspend fun runTextPrompt(promptText: String, modelName: String): Result<String> = withContext(dispatcher) {
        val modelPath = getInstalledModelPath()
        if (modelPath == null) {
            Napier.w("[OnDeviceLlmEngine] No local .litertlm model file found on device.", tag = "OnDeviceLlmEngine")
            return@withContext Result.failure(IllegalStateException("No local model file found on device storage."))
        }

        runCatching {
            Napier.d("[OnDeviceLlmEngine] Initializing LiteRT-LM Engine with model at $modelPath...", tag = "OnDeviceLlmEngine")

            if (currentEngine == null) {
                val context = androidContextRef
                val cachePath = context?.cacheDir?.path ?: ""
                Napier.d("[OnDeviceLlmEngine] Loading model weights into memory with cacheDir: $cachePath", tag = "OnDeviceLlmEngine")

                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = cachePath
                )
                val engine = Engine(config)
                engine.initialize()
                currentEngine = engine
                currentConversation = engine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of("You are a medical pharmacy assistant. Respond strictly in valid JSON.")
                    )
                )
            }

            val conv = checkNotNull(currentConversation)
            val responseFlow = conv.sendMessageAsync(promptText)
            val sb = StringBuilder()
            responseFlow.collect { chunk ->
                sb.append(chunk)
            }

            val responseText = sb.toString()
            Napier.d("[OnDeviceLlmEngine] Local LiteRT-LM inference finished (${responseText.length} chars)", tag = "OnDeviceLlmEngine")
            responseText
        }.onFailure { ex ->
            Napier.e("[OnDeviceLlmEngine] LiteRT-LM local inference failed: ${ex.message}", throwable = ex, tag = "OnDeviceLlmEngine")
        }
    }

    actual suspend fun runVisionOcr(imageByteArray: ByteArray, modelName: String): Result<String> = withContext(dispatcher) {
        runTextPrompt("Transcribe all written prescription medicines in this image in JSON.", modelName)
    }
}
