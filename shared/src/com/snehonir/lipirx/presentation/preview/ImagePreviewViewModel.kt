package com.snehonir.lipirx.presentation.preview

import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.common.LipiRxNoParams
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.GetAgentConfigsUseCase
import com.snehonir.lipirx.domain.usecase.GetProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.ProcessPrescriptionParams
import com.snehonir.lipirx.domain.usecase.ProcessPrescriptionUseCase
import com.snehonir.lipirx.domain.usecase.SaveScanSummaryParams
import com.snehonir.lipirx.domain.usecase.SaveScanSummaryUseCase
import com.snehonir.lipirx.presentation.common.LipiRxViewModel
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_api_key_required
import com.snehonir.lipirx.resources.error_connection_failed
import com.snehonir.lipirx.resources.error_please_select_image
import com.snehonir.lipirx.resources.loading_default_subtitle
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

class ImagePreviewViewModel(
    private val sessionState: PrescriptionSessionState,
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase,
    private val getAgentConfigsUseCase: GetAgentConfigsUseCase,
    private val getProviderConfigUseCase: GetProviderConfigUseCase,
    private val saveScanSummaryUseCase: SaveScanSummaryUseCase,
    private val repository: PrescriptionRepository,
    private val jsonParser: Json
) : LipiRxViewModel<ImagePreviewUiState>(ImagePreviewUiState()) {

    init {
        launchSafe {
            sessionState.session.collect { data ->
                val configs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()

                updateState {
                    copy(
                        imageByteArray = data.imageByteArray,
                        agentConfigs = configs,
                        errorMessage = data.errorMessage
                    )
                }
            }
        }
    }

    fun refreshAgentConfigs() {
        launchSafe {
            val configs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()

            updateState {
                copy(agentConfigs = configs)
            }
        }
    }

    fun onImageSelected(bytes: ByteArray) {
        sessionState.setImageBytes(bytes)
    }

    fun clearSelectedImage() {
        sessionState.setImageBytes(null)
    }

    fun processSelectedImage(
        flow: String = "App -> $TAG.processSelectedImage",
        onStarted: (Boolean) -> Unit = {}
    ) {
        val bytes = sessionState.session.value.imageByteArray
        if (bytes == null || bytes.isEmpty()) {
            launchSafe {
                val err = getString(Res.string.error_please_select_image)
                updateState { copy(errorMessage = err) }
                onStarted(false)
            }
            return
        }

        launchSafe {
            val currentAgentConfigs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()

            updateState { copy(agentConfigs = currentAgentConfigs) }

            val config = getProviderConfigUseCase(LipiRxNoParams).getOrDefault(ProviderConfigEntity())
            val visionConfig = currentAgentConfigs[RxAgentType.TRANSCRIPTION]
            val visionProvider = visionConfig?.provider ?: AiProvider.Gemini
            val visionApiKey = repository.getApiKeyForProvider(visionProvider.id)

            if (visionProvider.requiresApiKey && visionApiKey.isBlank()) {
                val errorMsg = getString(Res.string.error_api_key_required, visionProvider.displayName)
                updateState { copy(errorMessage = errorMsg) }
                sessionState.setResults(medicines = emptyList(), error = errorMsg)
                onStarted(false)
                return@launchSafe
            }

            val analyzeStartTime = getPlatformEpochMillis()
            val customUrl = if (visionProvider == AiProvider.Custom) config.customUrl else ""
            val visionModel = visionConfig?.selectedModel?.ifBlank { visionProvider.defaultModel } ?: visionProvider.defaultModel
            val savedStrategy = repository.getOnDeviceStrategy()

            val fallbackProvider = visionConfig?.fallbackProvider
            val fallbackApiKey = if (fallbackProvider != null) {
                repository.getApiKeyForProvider(fallbackProvider.id)
            } else {
                ""
            }

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

            val initialStatus = getString(Res.string.loading_default_subtitle)

            sessionState.setLoadingState(
                isLoading = true,
                step = 0,
                statusText = initialStatus,
                progressPercent = 10
            )
            onStarted(true)

            sessionState.startPipelineAnalysis {
                try {
                    val result = processPrescriptionUseCase(
                        ProcessPrescriptionParams(
                            input = input,
                            agentConfigs = currentAgentConfigs,
                            onEvent = sessionState::applyProgress
                        )
                    )

                    result.fold(
                        onSuccess = { orchResult ->
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
                                    saveScanSummaryUseCase(
                                        SaveScanSummaryParams(
                                            summaryText = summaryToSave,
                                            timestampMillis = analyzeStartTime,
                                            medicines = orchResult.items,
                                            flow = dbFlow
                                        )
                                    )
                                }
                            }.onFailure { ex ->
                                Napier.w("[$flow -> History Save Exception] ${ex.message}", throwable = ex, tag = TAG)
                            }
                        },
                        onFailure = { error ->
                            if (error is CancellationException) throw error
                            val fallbackFail = getString(Res.string.error_connection_failed)
                            val errorMsg = error.message ?: fallbackFail
                            sessionState.setResults(
                                medicines = emptyList(),
                                error = errorMsg
                            )
                        }
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.e("[$flow -> Pipeline Execution Exception] ${e.message}", throwable = e, tag = TAG)
                    val fallbackFail = getString(Res.string.error_connection_failed)
                    sessionState.setResults(
                        medicines = emptyList(),
                        error = e.message ?: fallbackFail
                    )
                }
            }
        }
    }

    companion object {
        private val TAG = ImagePreviewViewModel::class.simpleName
    }
}
