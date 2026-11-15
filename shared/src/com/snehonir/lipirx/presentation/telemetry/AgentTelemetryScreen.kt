package com.snehonir.lipirx.presentation.telemetry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.presentation.scan.components.PrimaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_clear_history
import com.snehonir.lipirx.resources.btn_run_sample_benchmark
import com.snehonir.lipirx.resources.header_trace_history
import com.snehonir.lipirx.resources.metric_success_rate
import com.snehonir.lipirx.resources.metric_total_hops
import com.snehonir.lipirx.resources.metric_total_latency
import com.snehonir.lipirx.resources.telemetry_empty_subtitle
import com.snehonir.lipirx.resources.telemetry_empty_title
import com.snehonir.lipirx.resources.telemetry_title
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
fun AgentTelemetryScreen(
    viewModel: AgentTraceViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Lipi.CardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(Res.string.telemetry_title),
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Ink
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.telemetry_title),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )
            }

            if (state.traces.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.clearAllTraces() },
                    shape = CircleShape,
                    color = Lipi.Error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Lipi.Error.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.btn_clear_history),
                            modifier = Modifier.size(14.dp),
                            tint = Lipi.Error
                        )
                    }
                }
            }
        }

        // Metrics Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(Res.string.metric_total_latency),
                value = "${state.totalPipelineTimeMs}ms",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(Res.string.metric_success_rate),
                value = "${(state.successRate * 100).toInt()}%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(Res.string.metric_total_hops),
                value = "${state.traces.size}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.header_trace_history),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Lipi.Ink,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.traces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Lipi.Muted
                    )
                    Text(
                        text = stringResource(Res.string.telemetry_empty_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                    Text(
                        text = stringResource(Res.string.telemetry_empty_subtitle),
                        fontSize = 13.sp,
                        color = Lipi.Muted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Lipi.Primary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        PrimaryButton(
                            text = stringResource(Res.string.btn_run_sample_benchmark),
                            icon = Icons.Default.Refresh,
                            onClick = { viewModel.runSampleBenchmarkTest() },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.traces, key = { it.id }) { trace ->
                    TraceCard(trace = trace)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, fontSize = 11.sp, color = Lipi.Muted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, color = Lipi.Ink, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun TraceCard(trace: AgentExecutionTraceEntity) {
    val statusColor = if (trace.isSuccess) Lipi.Primary else Lipi.Error

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Turn ${trace.turnNumber}: ${trace.agentType}",
                    color = Lipi.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (trace.isSuccess) "${trace.executionTimeMs}ms" else "FAILED",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "Provider: ${trace.provider} (${trace.modelName})",
                color = Lipi.Muted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            if (trace.reasoning.isNotBlank()) {
                Text(
                    text = "Reasoning: ${trace.reasoning}",
                    color = Lipi.Ink,
                    fontSize = 12.5.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trace.errorMessage.isNotBlank()) {
                Text(
                    text = "Error: ${trace.errorMessage}",
                    color = Lipi.Error,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
