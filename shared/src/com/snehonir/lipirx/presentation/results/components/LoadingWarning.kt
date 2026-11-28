package com.snehonir.lipirx.presentation.results.components

import androidx.compose.ui.graphics.Color
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.feed_preparing
import com.snehonir.lipirx.resources.feed_start_entity
import com.snehonir.lipirx.resources.feed_start_generic
import com.snehonir.lipirx.resources.feed_start_safety
import com.snehonir.lipirx.resources.feed_start_transcription
import com.snehonir.lipirx.resources.manager_title
import com.snehonir.lipirx.resources.row_entity
import com.snehonir.lipirx.resources.row_generic
import com.snehonir.lipirx.resources.row_safety
import com.snehonir.lipirx.resources.row_transcription
import com.snehonir.lipirx.resources.step_desc_entity
import com.snehonir.lipirx.resources.step_desc_generic
import com.snehonir.lipirx.resources.step_desc_safety
import com.snehonir.lipirx.resources.step_desc_transcription
import org.jetbrains.compose.resources.StringResource

/** Amber used for "needs review" states. Kept next to the labels so every card agrees. */
internal val LoadingWarning = Color(0xFFB26A00)
internal val LoadingWarningContainer = Color(0xFFB26A00).copy(alpha = 0.14f)

internal const val MAX_NAMES_SHOWN = 3

internal fun shortList(names: List<String>): String {
    val shown = names.take(MAX_NAMES_SHOWN).joinToString(", ")
    val extra = names.size - MAX_NAMES_SHOWN
    return if (extra > 0) "$shown +$extra" else shown
}

/** Short step name, e.g. "Safety check". */
internal fun rowLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.row_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.row_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.row_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.row_safety
    else -> Res.string.manager_title
}

/** Feed sentence for "this step just started". */
internal fun startLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.feed_start_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.feed_start_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.feed_start_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.feed_start_safety
    else -> Res.string.feed_preparing
}

/** One-line description shown under a step that has not started yet. */
internal fun stepDescRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.step_desc_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.step_desc_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.step_desc_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.step_desc_safety
    else -> Res.string.feed_preparing
}