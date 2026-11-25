package com.snehonir.lipirx.domain.agent

import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.reason_all_completed
import com.snehonir.lipirx.resources.reason_empty_workspace
import com.snehonir.lipirx.resources.reason_entities_normalized
import com.snehonir.lipirx.resources.reason_flagged_items
import com.snehonir.lipirx.resources.reason_forced_transcription
import com.snehonir.lipirx.resources.reason_human_approval
import com.snehonir.lipirx.resources.reason_parallel_enrichment
import com.snehonir.lipirx.resources.reason_pipeline_success
import com.snehonir.lipirx.resources.reason_substitutes_enriched
import com.snehonir.lipirx.resources.reason_transcription_done
import org.jetbrains.compose.resources.getString

object SupervisorConstants {
    const val AGENT_NAME = "Rx Supervisor Manager Agent"

    // Action Tokens
    const val TOKEN_FINISH = "FINISH"
    const val TOKEN_TRANSCRIPTION = "TRANSCRIPTION"
    const val TOKEN_PARSER = "PARSER"
    const val TOKEN_ENTITY = "ENTITY"
    const val TOKEN_GENERIC = "GENERIC"
    const val TOKEN_SUBSTITUTE = "SUBSTITUTE"
    const val TOKEN_SAFETY = "SAFETY"
    const val TOKEN_AUDITOR = "AUDITOR"

    // Default Reasoning Messages (Localized via Res.string)
    suspend fun getHumanApprovalReason(): String = getString(Res.string.reason_human_approval)
    suspend fun getForcedTranscriptionReason(): String = getString(Res.string.reason_forced_transcription)
    suspend fun getAllCompletedReason(): String = getString(Res.string.reason_all_completed)
    suspend fun getEmptyWorkspaceReason(): String = getString(Res.string.reason_empty_workspace)
    suspend fun getTranscriptionDoneReason(): String = getString(Res.string.reason_transcription_done)
    suspend fun getEntitiesNormalizedReason(): String = getString(Res.string.reason_entities_normalized)
    suspend fun getSubstitutesEnrichedReason(): String = getString(Res.string.reason_substitutes_enriched)
    suspend fun getParallelEnrichmentReason(): String = getString(Res.string.reason_parallel_enrichment)
    suspend fun getFlaggedItemsReason(): String = getString(Res.string.reason_flagged_items)
    suspend fun getPipelineSuccessReason(): String = getString(Res.string.reason_pipeline_success)

    suspend fun reasonTaskAssigned(agentType: RxAgentType, isParallel: Boolean = false): String {
        if (isParallel) return getParallelEnrichmentReason()
        return when (agentType) {
            RxAgentType.SUPERVISOR -> getEmptyWorkspaceReason()
            RxAgentType.TRANSCRIPTION -> getForcedTranscriptionReason()
            RxAgentType.ENTITY_PARSER -> getTranscriptionDoneReason()
            RxAgentType.GENERIC_SUBSTITUTE -> getEntitiesNormalizedReason()
            RxAgentType.SAFETY_AUDITOR -> getSubstitutesEnrichedReason()
        }
    }

    // Default Feedback Prompts
    const val PROMPT_TRANSCRIBE_ACCURATE = "Transcribe all written medicines accurately."
    const val PROMPT_TRANSCRIBE_DEFAULT = "Transcribe prescription."
    const val PROMPT_CLEAN_STRENGTHS = "Clean prefixes and normalize strengths."
    const val PROMPT_PROVIDE_SUBSTITUTES = "Provide 3 to 5 Bangladeshi brand substitutes per item."
    const val PROMPT_PARALLEL_ENRICHMENT = "Provide Bangladeshi brand substitutes and audit clinical dosage safety in parallel."
    const val PROMPT_AUDIT_RISKS = "Audit safety risks and unreadable spellings."
}
