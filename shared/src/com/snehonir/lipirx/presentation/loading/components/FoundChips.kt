package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.ScanMedicine
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.label_found_so_far
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

/**
 * Medicines show up here the moment the transcription step finishes (names only), and gain their
 * strengths once the entity parser has run. Anything the safety audit flagged turns amber.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FoundChips(
    medicines: List<ScanMedicine>,
    flaggedNames: List<String>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = medicines.isNotEmpty(),
        enter = fadeIn(tween(250)) + expandVertically(),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.label_found_so_far),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Muted,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                medicines.forEachIndexed { index, medicine ->
                    key(index, medicine.name) {
                        MedicineChip(
                            medicine = medicine,
                            flagged = medicine.name in flaggedNames,
                            appearDelayMs = index * 60,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineChip(medicine: ScanMedicine, flagged: Boolean, appearDelayMs: Int) {
    // Starts hidden and flips to visible on first composition, which plays the pop-in once.
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visible,
        enter = scaleIn(tween(250, delayMillis = appearDelayMs), initialScale = 0.8f) +
            fadeIn(tween(250, delayMillis = appearDelayMs)),
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (flagged) LoadingWarningContainer else Lipi.PrimaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (flagged) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = LoadingWarning,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = if (medicine.strength.isBlank()) medicine.name else "${medicine.name} · ${medicine.strength}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (flagged) LoadingWarning else Lipi.OnPrimaryContainer,
                )
            }
        }
    }
}