package com.snehonir.lipirx.domain.model

object ClinicalApprovalReasons {
    const val DEFAULT_PHARMACIST_APPROVAL = "Pharmacist sign-off required for flagged items."
    const val FLAGGED_ITEMS_VERIFICATION = "Flagged low confidence or dosage review items requiring human sign-off."

    fun auditAgentFailed(failureDisplayName: String): String {
        return "Safety Audit Agent failed ($failureDisplayName). Mandatory pharmacist sign-off required."
    }
}
