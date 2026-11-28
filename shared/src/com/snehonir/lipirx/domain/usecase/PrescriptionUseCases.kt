package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity
import com.snehonir.lipirx.data.local.getPlatformEpochMillis
import com.snehonir.lipirx.data.remote.service.CompositeAiService
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.OrchestrationResult
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.common.LipiRxNoParams
import com.snehonir.lipirx.domain.common.LipiRxSuspendUseCase
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.error_api_key_required
import com.snehonir.lipirx.resources.error_connection_failed
import com.snehonir.lipirx.resources.loading_default_subtitle
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString

data class ProcessPrescriptionParams(
    val input: TranscriptionInput,
    val agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    val onEvent: (PipelineEvent) -> Unit = {}
)

class ProcessPrescriptionUseCase(
    private val koogSupervisorEngine: KoogRxSupervisorEngine
) : LipiRxSuspendUseCase<ProcessPrescriptionParams, OrchestrationResult> {

    override suspend fun invoke(params: ProcessPrescriptionParams): Result<OrchestrationResult> {
        return koogSupervisorEngine.runOrchestration(
            input = params.input,
            agentConfigs = params.agentConfigs,
            onEvent = params.onEvent
        )
    }
}

data class LaunchPipelineParams(
    val imageByteArray: ByteArray,
    val flow: String = "LaunchPrescriptionPipelineUseCase",
    val tag: String = "PipelineRunner",
    val onStarted: (Boolean) -> Unit = {}
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LaunchPipelineParams) return false

        if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        if (flow != other.flow) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageByteArray.contentHashCode()
        result = 31 * result + flow.hashCode()
        result = 31 * result + tag.hashCode()
        return result
    }
}

class LaunchPrescriptionPipelineUseCase(
    private val sessionState: PrescriptionSessionState,
    private val processPrescriptionUseCase: ProcessPrescriptionUseCase,
    private val getAgentConfigsUseCase: GetAgentConfigsUseCase,
    private val getProviderConfigUseCase: GetProviderConfigUseCase,
    private val saveScanSummaryUseCase: SaveScanSummaryUseCase,
    private val repository: PrescriptionRepository,
    private val jsonParser: Json
) {
    suspend operator fun invoke(params: LaunchPipelineParams) {
        val bytes = params.imageByteArray
        val flow = params.flow
        val tag = params.tag
        val onStarted = params.onStarted

        val currentAgentConfigs = getAgentConfigsUseCase(LipiRxNoParams).getOrDefault(emptyMap()).toImmutableMap()
        val config = getProviderConfigUseCase(LipiRxNoParams).getOrDefault(ProviderConfigEntity())
        val visionConfig = currentAgentConfigs[RxAgentType.TRANSCRIPTION]
        val visionProvider = visionConfig?.provider ?: AiProvider.Gemini
        val visionApiKey = repository.getApiKeyForProvider(visionProvider.id)

        if (visionProvider.requiresApiKey && visionApiKey.isBlank()) {
            val errorMsg = getString(Res.string.error_api_key_required, visionProvider.displayName)
            sessionState.setResults(medicines = emptyList(), error = errorMsg)
            onStarted(false)
            return
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

        onStarted(true)

        sessionState.startPipelineAnalysis {
            sessionState.setLoadingState(
                isLoading = true,
                step = 0,
                statusText = initialStatus,
                progressPercent = 10
            )

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
                            Napier.w("[$flow -> History Save Exception] ${ex.message}", throwable = ex, tag = tag)
                        }
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        val fallbackFail = getString(Res.string.error_connection_failed)
                        sessionState.setResults(
                            medicines = emptyList(),
                            error = error.message ?: fallbackFail
                        )
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Napier.e("[$flow -> Pipeline Execution Exception] ${e.message}", throwable = e, tag = tag)
                val fallbackFail = getString(Res.string.error_connection_failed)
                sessionState.setResults(
                    medicines = emptyList(),
                    error = e.message ?: fallbackFail
                )
            }
        }
    }
}

data class FetchModelsParams(
    val provider: AiProvider,
    val providedApiKey: String = "",
    val customUrl: String = "",
    val flow: String = ""
)

class FetchAvailableModelsUseCase(
    private val aiService: CompositeAiService
) : LipiRxSuspendUseCase<FetchModelsParams, List<String>> {

    override suspend fun invoke(params: FetchModelsParams): Result<List<String>> {
        return aiService.fetchAvailableModels(
            providedApiKey = params.providedApiKey,
            provider = params.provider,
            customUrl = params.customUrl,
            flow = params.flow
        ).map { models -> models.map { it.name } }
    }
}
