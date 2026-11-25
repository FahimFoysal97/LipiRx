package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.FeedEntry
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.feed_double_check
import com.snehonir.lipirx.resources.feed_entity_done
import com.snehonir.lipirx.resources.feed_finalizing
import com.snehonir.lipirx.resources.feed_found_medicines
import com.snehonir.lipirx.resources.feed_generic_done
import com.snehonir.lipirx.resources.feed_preparing
import com.snehonir.lipirx.resources.feed_safety_done
import com.snehonir.lipirx.resources.feed_safety_failed
import com.snehonir.lipirx.resources.feed_safety_flagged
import com.snehonir.lipirx.resources.feed_start_entity
import com.snehonir.lipirx.resources.feed_start_generic
import com.snehonir.lipirx.resources.feed_start_safety
import com.snehonir.lipirx.resources.feed_start_transcription
import com.snehonir.lipirx.resources.feed_step_failed
import com.snehonir.lipirx.resources.feed_waiting_pharmacist
import com.snehonir.lipirx.resources.manager_title
import com.snehonir.lipirx.resources.row_entity
import com.snehonir.lipirx.resources.row_generic
import com.snehonir.lipirx.resources.row_safety
import com.snehonir.lipirx.resources.row_transcription
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_NAMES_SHOWN = 3

@Composable
internal fun FeedCard(entries: List<FeedEntry>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(tween(350, easing = FastOutSlowInEasing))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                val latest = index == entries.lastIndex
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (latest) Lipi.Primary else Lipi.CardBorder)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = feedText(entry),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = if (latest) Lipi.Ink else Lipi.Muted,
                        fontWeight = if (latest) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun feedText(entry: FeedEntry): String = when (entry) {
    FeedEntry.Preparing -> stringResource(Res.string.feed_preparing)
    FeedEntry.Finalizing -> stringResource(Res.string.feed_finalizing)
    FeedEntry.WaitingForPharmacist -> stringResource(Res.string.feed_waiting_pharmacist)
    is FeedEntry.Started ->
        if (entry.rerun) {
            stringResource(Res.string.feed_double_check, stringResource(rowLabelRes(entry.agent)))
        } else {
            stringResource(startLabelRes(entry.agent))
        }

    is FeedEntry.Finished -> when {
        !entry.success && entry.agent == RxAgentType.SAFETY_AUDITOR ->
            stringResource(Res.string.feed_safety_failed)

        !entry.success ->
            stringResource(Res.string.feed_step_failed, stringResource(rowLabelRes(entry.agent)))

        entry.agent == RxAgentType.TRANSCRIPTION ->
            stringResource(Res.string.feed_found_medicines, entry.itemCount)

        entry.agent == RxAgentType.ENTITY_PARSER ->
            stringResource(Res.string.feed_entity_done)

        entry.agent == RxAgentType.GENERIC_SUBSTITUTE ->
            stringResource(Res.string.feed_generic_done)

        entry.flaggedNames.isNotEmpty() ->
            stringResource(Res.string.feed_safety_flagged, shortList(entry.flaggedNames))

        else -> stringResource(Res.string.feed_safety_done)
    }
}

private fun shortList(names: List<String>): String {
    val shown = names.take(MAX_NAMES_SHOWN).joinToString(", ")
    val extra = names.size - MAX_NAMES_SHOWN
    return if (extra > 0) "$shown +$extra" else shown
}

private fun rowLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.row_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.row_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.row_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.row_safety
    else -> Res.string.manager_title
}

private fun startLabelRes(agent: RxAgentType): StringResource = when (agent) {
    RxAgentType.TRANSCRIPTION -> Res.string.feed_start_transcription
    RxAgentType.ENTITY_PARSER -> Res.string.feed_start_entity
    RxAgentType.GENERIC_SUBSTITUTE -> Res.string.feed_start_generic
    RxAgentType.SAFETY_AUDITOR -> Res.string.feed_start_safety
    else -> Res.string.feed_preparing
}
