package com.snehonir.lipirx.presentation.loading.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.label_analysis_progress
import com.snehonir.lipirx.resources.label_checks_count
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

/**
 * The only progress indicator on the screen. The old checks bar is folded into the caption
 * ("2 of 4 checks") so the user never has to compare two bars.
 */
@Composable
internal fun StatusHeader(
    fraction: Float,
    completedChecks: Int,
    totalChecks: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.label_analysis_progress),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink,
                )
                Text(
                    text = stringResource(Res.string.label_checks_count, completedChecks, totalChecks),
                    fontSize = 12.sp,
                    color = Lipi.Muted,
                )
            }
            Text(
                text = "${(fraction * 100).toInt()}%",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Lipi.Primary,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = Lipi.Primary,
            trackColor = Lipi.PrimaryContainer,
            strokeCap = StrokeCap.Round,
        )
    }
}