package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.FeedEntry
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.cd_hide_details
import com.snehonir.lipirx.resources.cd_show_details
import com.snehonir.lipirx.resources.feed_double_check
import com.snehonir.lipirx.resources.feed_entity_done
import com.snehonir.lipirx.resources.feed_finalizing
import com.snehonir.lipirx.resources.feed_found_medicines
import com.snehonir.lipirx.resources.feed_generic_done
import com.snehonir.lipirx.resources.feed_preparing
import com.snehonir.lipirx.resources.feed_safety_done
import com.snehonir.lipirx.resources.feed_safety_failed
import com.snehonir.lipirx.resources.feed_safety_flagged
import com.snehonir.lipirx.resources.feed_step_failed
import com.snehonir.lipirx.resources.feed_waiting_pharmacist
import com.snehonir.lipirx.resources.label_details
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

/**
 * The play-by-play (what used to be FeedCard) tucked behind a single "Details" toggle. The
 * headline story lives in the supervisor card and the stepper; this is for people who want the log.
 */
@Composable
internal fun DetailsDrawer(entries: List<FeedEntry>, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "detailsChevron",
    )
    val toggleLabel = stringResource(
        if (expanded) Res.string.cd_hide_details else Res.string.cd_show_details
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(300, easing = FastOutSlowInEasing)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = toggleLabel) { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.label_details),
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Muted,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Lipi.Muted,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevron),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(),
            exit = fadeOut(tween(120)) + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
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