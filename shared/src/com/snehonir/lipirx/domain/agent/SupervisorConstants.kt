package com.snehonir.lipirx.domain.agent

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

    // Default Reasoning Messages
    const val REASON_HUMAN_APPROVAL = "Supervisor requested human approval."
    const val REASON_FORCED_TRANSCRIPTION = "Forced transcription because workspace has no items yet."
    const val REASON_ALL_COMPLETED = "Supervisor completed all orchestration steps."
    const val REASON_EMPTY_WORKSPACE = "Workspace empty. Requesting Vision Transcription Agent to perform handwriting OCR."
    const val REASON_TRANSCRIPTION_DONE = "Raw transcription complete. Requesting Medical Entity Parser Agent to clean brand names and normalize forms."
    const val REASON_ENTITIES_NORMALIZED = "Entities normalized. Requesting Generic & Substitute Agent to match DGDA active ingredients and BD brand substitutes."
    const val REASON_SUBSTITUTES_ENRICHED = "Substitutes enriched. Requesting Safety Auditor Agent to verify clinical dosage safety and review flags."
    const val REASON_FLAGGED_ITEMS = "Flagged low confidence or dosage review items requiring human sign-off."
    const val REASON_PIPELINE_SUCCESS = "All pipeline capabilities executed successfully."

    fun reasonTaskAssigned(agentName: String): String = "Supervisor assigned task to $agentName."

    // Default Feedback Prompts
    const val PROMPT_TRANSCRIBE_ACCURATE = "Transcribe all written medicines accurately."
    const val PROMPT_TRANSCRIBE_DEFAULT = "Transcribe prescription."
    const val PROMPT_CLEAN_STRENGTHS = "Clean prefixes and normalize strengths."
    const val PROMPT_PROVIDE_SUBSTITUTES = "Provide 3 to 5 Bangladeshi brand substitutes per item."
    const val PROMPT_AUDIT_RISKS = "Audit safety risks and unreadable spellings."
}
