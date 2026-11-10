package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier

data class TranscriptionInput(
    val imageByteArray: ByteArray,
    val selectedModel: String,
    val provider: AiProvider,
    val providedApiKey: String = "",
    val customUrl: String = "",
    val mimeType: String = "image/jpeg",
    val onProgress: (step: Int, statusText: String) -> Unit = { _, _ -> }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TranscriptionInput) return false
        return imageByteArray.contentEquals(other.imageByteArray) &&
                selectedModel == other.selectedModel &&
                provider == other.provider
    }

    override fun hashCode(): Int {
        var result = imageByteArray.contentHashCode()
        result = 31 * result + selectedModel.hashCode()
        result = 31 * result + provider.hashCode()
        return result
    }
}

class VisionTranscriptionAgent(
    private val aiService: CompositeAiService
) : RxAgent<TranscriptionInput, List<MedicineItem>> {

    override val agentName: String = "Vision Transcription Agent"
    override val description: String = "Encodes prescription images and extracts handwriting using Multimodal Vision AI"

    override suspend fun execute(input: TranscriptionInput): Result<List<MedicineItem>> {
        Napier.d("[$agentName] Executing vision handwriting OCR via ${input.provider.displayName}...", tag = TAG)
        return aiService.processPrescriptionImage(
            imageByteArray = input.imageByteArray,
            selectedModel = input.selectedModel,
            providedApiKey = input.providedApiKey,
            provider = input.provider,
            customUrl = input.customUrl,
            mimeType = input.mimeType,
            flow = "MultiAgent -> $agentName",
            onProgress = input.onProgress
        )
    }

    companion object {
        private const val TAG = "VisionTranscriptionAgent"
    }
}
