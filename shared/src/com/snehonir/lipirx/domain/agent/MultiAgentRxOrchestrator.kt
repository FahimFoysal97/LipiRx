package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.model.ProcessingStep
import io.github.aakira.napier.Napier

class MultiAgentRxOrchestrator(
    private val visionTranscriptionAgent: VisionTranscriptionAgent,
    private val medicalEntityParserAgent: MedicalEntityParserAgent,
    private val genericSubstituteAgent: GenericSubstituteAgent,
    private val safetyAuditorAgent: SafetyAuditorAgent,
    private val database: LipiRxLocalDatabase
) {
    suspend fun executePipeline(
        input: TranscriptionInput,
        agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
        onProgress: (step: ProcessingStep) -> Unit = {}
    ): Result<List<MedicineItem>> {
        Napier.d("[$TAG] Initiating Multiagent Pipeline with ${agentConfigs.size} custom agent assignments...", tag = TAG)

        // Stage 1 Provider & Model
        val transcriptionConfig = agentConfigs[RxAgentType.TRANSCRIPTION]
        val transcriptionProvider = transcriptionConfig?.provider ?: input.provider
        val transcriptionModel = transcriptionConfig?.selectedModel?.ifBlank { input.selectedModel } ?: input.selectedModel

        // Retrieve provider-specific API Key and Custom URL for Stage 1
        val stage1ApiKey = database.getApiKeyForProvider(transcriptionProvider.id)
        val stage1CustomUrl = if (transcriptionProvider == AiProvider.Custom) database.getProviderConfig().customUrl else ""

        Napier.d("[$TAG -> Stage 1] ${visionTranscriptionAgent.agentName} assigned to provider '${transcriptionProvider.displayName}' (model: $transcriptionModel, apiKeyLength=${stage1ApiKey.length})", tag = TAG)

        val stage1Input = input.copy(
            provider = transcriptionProvider,
            selectedModel = transcriptionModel,
            providedApiKey = stage1ApiKey,
            customUrl = stage1CustomUrl
        )

        // Stage 1: Vision Transcription Agent (Handwriting OCR)
        onProgress(ProcessingStep.READING_HANDWRITING)
        val rawMedicines = visionTranscriptionAgent.execute(stage1Input).getOrElse { ex ->
            Napier.e("[$TAG -> Stage 1 Failure] ${visionTranscriptionAgent.agentName} failed: ${ex.message}", throwable = ex, tag = TAG)
            return Result.failure(ex)
        }

        // Stage 2: Medical Entity Parser Agent
        val parserConfig = agentConfigs[RxAgentType.ENTITY_PARSER]
        val parserProvider = parserConfig?.provider ?: AiProvider.Gemini
        val parserModel = parserConfig?.selectedModel?.ifBlank { parserProvider.defaultModel } ?: parserProvider.defaultModel
        Napier.d("[$TAG -> Stage 2] ${medicalEntityParserAgent.agentName} assigned to provider '${parserProvider.displayName}' (model: $parserModel)", tag = TAG)
        onProgress(ProcessingStep.MATCHING_BRANDS)
        val parsedMedicines = medicalEntityParserAgent.execute(rawMedicines).getOrDefault(rawMedicines)

        // Stage 3: Generic & Substitute Agent
        val substituteConfig = agentConfigs[RxAgentType.GENERIC_SUBSTITUTE]
        val substituteProvider = substituteConfig?.provider ?: AiProvider.Groq
        val substituteModel = substituteConfig?.selectedModel?.ifBlank { substituteProvider.defaultModel } ?: substituteProvider.defaultModel
        Napier.d("[$TAG -> Stage 3] ${genericSubstituteAgent.agentName} assigned to provider '${substituteProvider.displayName}' (model: $substituteModel)", tag = TAG)
        onProgress(ProcessingStep.FINDING_SIMILAR)
        val enrichedMedicines = genericSubstituteAgent.execute(parsedMedicines).getOrDefault(parsedMedicines)

        // Stage 4: Safety Auditor Agent
        val safetyConfig = agentConfigs[RxAgentType.SAFETY_AUDITOR]
        val safetyProvider = safetyConfig?.provider ?: AiProvider.OpenAi
        val safetyModel = safetyConfig?.selectedModel?.ifBlank { safetyProvider.defaultModel } ?: safetyProvider.defaultModel
        Napier.d("[$TAG -> Stage 4] ${safetyAuditorAgent.agentName} assigned to provider '${safetyProvider.displayName}' (model: $safetyModel)", tag = TAG)
        val finalAuditedMedicines = safetyAuditorAgent.execute(enrichedMedicines).getOrDefault(enrichedMedicines)

        onProgress(ProcessingStep.COMPLETE)
        Napier.d("[$TAG -> Pipeline Success] Successfully processed ${finalAuditedMedicines.size} medicine items through all 4 AI agents.", tag = TAG)

        return Result.success(finalAuditedMedicines)
    }

    companion object {
        private const val TAG = "MultiAgentRxOrchestrator"
    }
}
