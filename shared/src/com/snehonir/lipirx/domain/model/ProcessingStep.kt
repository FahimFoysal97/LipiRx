package com.snehonir.lipirx.domain.model

enum class ProcessingStep(val stepIndex: Int, val statusText: String) {
    ENCODING(0, "Encoding image & payload..."),
    READING_HANDWRITING(1, "Reading handwriting..."),
    MATCHING_BRANDS(2, "Matching brand names..."),
    FINDING_SIMILAR(3, "Finding similar brands..."),
    COMPLETE(4, "✓ Analysis Complete! Extracting medicines...");

    companion object {
        fun fromStepIndex(index: Int): ProcessingStep = entries.firstOrNull { it.stepIndex == index } ?: ENCODING
    }
}
