package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.summary_counts
import com.snehonir.lipirx.resources.summary_flagged
import com.snehonir.lipirx.resources.summary_ok
import com.snehonir.lipirx.resources.summary_pharmacist
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

/**
 * One-glance outcome shown when the pipeline is done: how many medicines, how many generic
 * options, and whether anything needs a human. Turns amber when something is flagged or the
 * pipeline is suspended for the pharmacist.
 */
@Composable
internal fun ResultBanner(
    visible: Boolean,
    progress: PipelineProgress,
    isSuspended: Boolean,
    modifier: Modifier = Modifier,
) {
    val medicines = progress.medicines.size
    val generics = progress.worker(RxAgentType.GENERIC_SUBSTITUTE).substituteCount
    val flagged = progress.flaggedMedicineNames.size
    val needsAttention = isSuspended || flagged > 0

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (needsAttention) LoadingWarningContainer else Lipi.PrimaryContainer,
        ) {
            Row(
                modifier = Modifier
                    .animateContentSize()
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (needsAttention) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (needsAttention) LoadingWarning else Lipi.Primary,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(20.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when {
                            flagged > 0 ->
                                stringResource(Res.string.summary_flagged, medicines, generics, flagged)
                            // Suspended with nothing flagged means the audit itself failed, so never claim "no problems".
                            isSuspended ->
                                stringResource(Res.string.summary_counts, medicines, generics)
                            else ->
                                stringResource(Res.string.summary_ok, medicines, generics)
                        },
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink,
                    )
                    if (isSuspended) {
                        Text(
                            text = stringResource(Res.string.summary_pharmacist),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Lipi.Muted,
                        )
                    }
                }
            }
        }
    }
}