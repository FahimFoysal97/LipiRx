package com.snehonir.lipirx.presentation.telemetry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_run_sample_benchmark
import com.snehonir.lipirx.resources.telemetry_empty_subtitle
import com.snehonir.lipirx.resources.telemetry_empty_title
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TelemetryEmptyState(
    innerPadding: PaddingValues,
    isLoading: Boolean,
    onRunBenchmark: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding + PaddingValues(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(Lipi.PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Lipi.Primary
                )
            }
            Text(
                text = stringResource(Res.string.telemetry_empty_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.telemetry_empty_subtitle),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Lipi.Muted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Lipi.Primary,
                    strokeWidth = 2.5.dp
                )
            } else {
                PrimaryButton(
                    text = stringResource(Res.string.btn_run_sample_benchmark),
                    icon = Icons.Default.PlayArrow,
                    onClick = onRunBenchmark,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
