package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.data.local.scanInstalledOnDeviceModels
import com.snehonir.lipirx.data.remote.dto.ModelInfoDto
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_local_slm_decode_failed
import com.snehonir.lipirx.resources.error_local_slm_parse_failed
import com.snehonir.lipirx.resources.error_no_local_slm_model
import com.snehonir.lipirx.resources.error_on_device_ocr_failed
import com.snehonir.lipirx.resources.on_device_executing
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

class OnDeviceAiService(
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    private val onDeviceEngine: OnDeviceLlmEngine = OnDeviceLlmEngine(),
    private val repository: PrescriptionRepository? = null
) : LipiRxAiService {

    override val provider: AiProvider = AiProvider.OnDevice

    override suspend fun fetchAvailableModels(
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<List<ModelInfoDto>> {
        val customPath = repository?.getSlmModelDirectory()
        val installedModels = scanInstalledOnDeviceModels(customPath)
        Napier.d("[$flow -> OnDevice Models] Scanned ${installedModels.size} installed local model files from device storage (customPath='$customPath'): ${installedModels.map { it.name }}", tag = TAG)
        return Result.success(installedModels)
    }

    override suspend fun processPrescriptionImage(
        imageByteArray: ByteArray,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        mimeType: String,
        flow: String,
        onProgress: (step: Int, statusText: String) -> Unit
    ): Result<List<MedicineItem>> {
        Napier.d("[$flow -> OnDevice] Executing local On-Device SLM OCR processing (${imageByteArray.size} bytes)...", tag = TAG)
        onProgress(1, getString(Res.string.on_device_executing))

        if (!onDeviceEngine.isModelInstalled()) {
            val errorMsg = formatAiErrorMessage(IllegalStateException(getString(Res.string.error_no_local_slm_model)), provider)
            Napier.w("[$flow -> OnDevice Error] $errorMsg", tag = TAG)
            return Result.failure(IllegalStateException(errorMsg))
        }

        val localResult = onDeviceEngine.runVisionOcr(imageByteArray, selectedModel)
        if (localResult.isFailure) {
            val ex = localResult.exceptionOrNull() ?: IllegalStateException(getString(Res.string.error_on_device_ocr_failed))
            Napier.e("[$flow -> OnDevice Error] Vision OCR failed: ${ex.message}", throwable = ex, tag = TAG)
            return Result.failure(ex)
        }

        val rawText = localResult.getOrThrow()
        val parseResult = parseMedicineItemsFromText(rawText)

        if (parseResult.isFailure) {
            val ex = parseResult.exceptionOrNull() ?: IllegalStateException(getString(Res.string.error_local_slm_parse_failed))
            Napier.w("[$flow -> OnDevice Error] ${ex.message}", tag = TAG)
            return Result.failure(ex)
        }

        val items = parseResult.getOrThrow()
        Napier.d("[$flow -> OnDevice Success] Real local SLM model extracted ${items.size} items.", tag = TAG)
        return Result.success(items)
    }

    override suspend fun processTextPrompt(
        promptText: String,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        flow: String
    ): Result<String> {
        Napier.d("[$flow -> OnDevice] Executing local On-Device SLM text prompt (${promptText.length} chars)...", tag = TAG)

        if (!onDeviceEngine.isModelInstalled()) {
            val errorMsg = formatAiErrorMessage(IllegalStateException(getString(Res.string.error_no_local_slm_model)), provider)
            Napier.w("[$flow -> OnDevice Error] $errorMsg", tag = TAG)
            return Result.failure(IllegalStateException(errorMsg))
        }

        val localResult = onDeviceEngine.runTextPrompt(promptText, selectedModel)
        return localResult.fold(
            onSuccess = { responseText ->
                Napier.d("[$flow -> OnDevice Success] Real local LiteRT-LM model generated ${responseText.length} chars.", tag = TAG)
                Result.success(responseText)
            },
            onFailure = { ex ->
                Napier.e("[$flow -> OnDevice Error] Local SLM text inference failed: ${ex.message}", throwable = ex, tag = TAG)
                Result.failure(ex)
            }
        )
    }

    override suspend fun streamContent(
        promptText: String,
        selectedModel: String,
        providedApiKey: String,
        customUrl: String,
        flow: String,
        onToken: (String) -> Unit
    ): Result<String> {
        val res = processTextPrompt(promptText, selectedModel, providedApiKey, customUrl, flow)
        res.onSuccess { onToken(it) }
        return res
    }

    private suspend fun parseMedicineItemsFromText(rawText: String): Result<List<MedicineItem>> {
        val cleaned = cleanJson(rawText)

        runCatching { jsonParser.decodeFromString<List<MedicineItem>>(cleaned) }
            .getOrNull()
            ?.let { return Result.success(it) }

        runCatching { jsonParser.decodeFromString<MedicineItem>(cleaned) }
            .getOrNull()
            ?.let { return Result.success(listOf(it)) }

        return Result.failure(
            IllegalStateException(getString(Res.string.error_local_slm_decode_failed))
        )
    }

    private fun cleanJson(rawText: String): String {
        val sanitized = rawText.replace(JsonFenceRegex, "").trim()
        val start = sanitized.indexOfFirst { it == '[' || it == '{' }
        val end = sanitized.indexOfLast { it == ']' || it == '}' }
        return if (start in 0 until end) sanitized.substring(start, end + 1) else sanitized
    }

    companion object {
        private const val TAG = "OnDeviceAiService"
        private val JsonFenceRegex = Regex("```(?:json)?", RegexOption.IGNORE_CASE)
    }
}
