package com.snehonir.lipirx.presentation.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import com.snehonir.lipirx.domain.usecase.ManageProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.ProcessPrescriptionUseCase
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

data class ImagePreviewUiState(
    val imageByteArray: ByteArray? = null,
    val agentConfigs: ImmutableMap<RxAgentType, AgentConfig> = persistentMapOf(),
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImagePreviewUiState) return false

        if (imageByteArray != null) {
            if (other.imageByteArray == null) return false
            if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        } else if (other.imageByteArray != null) return false
        if (agentConfigs != other.agentConfigs) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageByteArray?.contentHashCode() ?: 0
        result = 31 * result + agentConfigs.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}

class ImagePreviewViewModel(
    private val sessionState: PrescriptionSessionState,
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase,
    private val manageProviderConfigUseCase: ManageProviderConfigUseCase,
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val jsonParser: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImagePreviewUiState())
    val uiState: StateFlow<ImagePreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionState.session.collect { data ->
                val configs = runCatching {
                    manageProviderConfigUseCase.getAllAgentConfigs()
                }.getOrDefault(emptyMap()).toImmutableMap()

                _uiState.update {
                    it.copy(
                        imageByteArray = data.imageByteArray,
                        agentConfigs = configs,
                        errorMessage = data.errorMessage
                    )
                }
            }
        }
    }

    fun onImageSelected(bytes: ByteArray) {
        sessionState.setImageBytes(bytes)
    }

    fun clearSelectedImage() {
        sessionState.setImageBytes(null)
    }

    fun processSelectedImage(flow: String = "com.snehonir.lipirx.App -> ImagePreviewViewModel.processSelectedImage"): Boolean {
        val bytes = sessionState.session.value.imageByteArray
        if (bytes == null || bytes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select an image first.") }
            return false
        }

        val config = manageProviderConfigUseCase.getProviderConfig()
        val visionConfig = _uiState.value.agentConfigs[RxAgentType.TRANSCRIPTION]
        val visionProvider = visionConfig?.provider ?: AiProvider.Gemini
        val visionApiKey = manageProviderConfigUseCase.getApiKeyForProvider(visionProvider.id)

        if (visionProvider.requiresApiKey && visionApiKey.isBlank()) {
            val errorMsg = "${visionProvider.displayName} API key is required. Please set your API key in Model Settings."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            sessionState.setResults(medicines = emptyList(), error = errorMsg)
            return false
        }

        val analyzeStartTime = getPlatformEpochMillis()
        val customUrl = if (visionProvider == AiProvider.Custom) config.customUrl else ""
        val visionModel = visionConfig?.selectedModel?.ifBlank { visionProvider.defaultModel } ?: visionProvider.defaultModel
        val savedStrategy = manageProviderConfigUseCase.getOnDeviceStrategy()

        val fallbackProvider = visionConfig?.fallbackProvider
        val fallbackApiKey = if (fallbackProvider != null) manageProviderConfigUseCase.getApiKeyForProvider(fallbackProvider.id) else ""

        val input = TranscriptionInput(
            imageByteArray = bytes,
            selectedModel = visionModel,
            provider = visionProvider,
            providedApiKey = visionApiKey,
            customUrl = customUrl,
            strategy = savedStrategy,
            fallbackProvider = fallbackProvider,
            fallbackModel = visionConfig?.fallbackModel ?: "",
            fallbackApiKey = fallbackApiKey
        )

        sessionState.setLoadingState(
            isLoading = true,
            step = 0,
            statusText = "Initiating Vision AI Analysis...",
            progressPercent = 10,
            agent = RxAgentType.SUPERVISOR
        )

        sessionState.startPipelineAnalysis {
            val result = processPrescriptionUseCase.executePipeline(
                input = input,
                agentConfigs = _uiState.value.agentConfigs,
                onProgress = { agentType, reasoning ->
                    sessionState.setLoadingState(
                        isLoading = true,
                        step = agentType.ordinal,
                        statusText = reasoning.ifBlank { "Agent active..." },
                        progressPercent = 100,
                        agent = agentType
                    )
                }
            )

            result.fold(
                onSuccess = { orchResult ->
                    delay(800.milliseconds)
                    sessionState.setResults(
                        medicines = orchResult.items,
                        isSuspended = orchResult.isSuspendedForApproval,
                        approvalReason = orchResult.approvalReason.ifBlank { null }
                    )

                    runCatching {
                        val dbFlow = "$flow -> saveSummary"
                        val summaryJson = runCatching { jsonParser.encodeToString(orchResult.items) }.getOrDefault("")
                        val summaryToSave = summaryJson.ifBlank {
                            orchResult.items.joinToString("; ") { "${it.formattedFullName} (${it.genericName}) [${it.schedule}] Qty: ${it.formattedQuantity}" }
                        }
                        if (summaryToSave.isNotBlank() && orchResult.items.isNotEmpty()) {
                            getScanHistoryUseCase.saveSummary(summaryToSave, timestampMillis = analyzeStartTime, flow = dbFlow)
                            getScanHistoryUseCase.saveDispensedHistory(orchResult.items, flow = dbFlow)
                        }
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    val errorMsg = error.message ?: "Analysis failed."
                    sessionState.setResults(
                        medicines = emptyList(),
                        error = errorMsg
                    )
                }
            )
        }
        return true
    }

    companion object {
        private const val TAG = "ImagePreviewViewModel"
    }
}
