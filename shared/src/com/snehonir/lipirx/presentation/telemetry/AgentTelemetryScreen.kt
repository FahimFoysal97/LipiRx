package com.snehonir.lipirx.presentation.telemetry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.presentation.scan.components.AppHeader
import com.snehonir.lipirx.presentation.scan.components.PrimaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_clear_history
import com.snehonir.lipirx.resources.btn_clear_traces
import com.snehonir.lipirx.resources.btn_run_sample_benchmark
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.header_trace_history
import com.snehonir.lipirx.resources.label_hops
import com.snehonir.lipirx.resources.label_items
import com.snehonir.lipirx.resources.label_turn_breakdown
import com.snehonir.lipirx.resources.label_turn_failed
import com.snehonir.lipirx.resources.label_turn_number
import com.snehonir.lipirx.resources.metric_success_rate
import com.snehonir.lipirx.resources.metric_total_hops
import com.snehonir.lipirx.resources.metric_total_latency
import com.snehonir.lipirx.resources.status_failed
import com.snehonir.lipirx.resources.status_passed
import com.snehonir.lipirx.resources.telemetry_empty_subtitle
import com.snehonir.lipirx.resources.telemetry_empty_title
import com.snehonir.lipirx.resources.telemetry_subtitle
import com.snehonir.lipirx.resources.telemetry_title
import com.snehonir.lipirx.resources.unit_hours
import com.snehonir.lipirx.resources.unit_millis
import com.snehonir.lipirx.resources.unit_minutes
import com.snehonir.lipirx.resources.unit_seconds
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

private data class TelemetryScanSession(
    val sessionId: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val totalTimeMs: Long,
    val totalHops: Int,
    val isSuccess: Boolean,
    val failedTurnCount: Int,
    val finalItemCount: Int,
    val modelSummary: String,
    val traces: List<AgentExecutionTraceEntity>
)

@Composable
fun AgentTelemetryScreen(
    viewModel: AgentTraceViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val scanSessions = remember(state.traces) {
        groupTracesIntoScanSessions(state.traces)
    }

    var expandedSessionId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Header
        AppHeader(
            onBack = onClose,
            backContentDescription = stringResource(Res.string.telemetry_title),
            title = stringResource(Res.string.telemetry_title),
            subtitle = stringResource(Res.string.telemetry_subtitle)
        )

        // Metrics Summary Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = stringResource(Res.string.metric_total_latency),
                value = formatDuration(state.totalPipelineTimeMs),
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

        // Row 1: Section Title
        Text(
            text = stringResource(Res.string.header_trace_history),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Lipi.Ink,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Action Bar (Run Benchmark & Clear Traces buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Lipi.Primary,
                    strokeWidth = 2.dp
                )
            } else {
                Surface(
                    onClick = { viewModel.runSampleBenchmarkTest() },
                    shape = CircleShape,
                    color = Lipi.PrimaryContainer,
                    border = BorderStroke(1.dp, Lipi.Primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.btn_run_sample_benchmark),
                            modifier = Modifier.size(14.dp),
                            tint = Lipi.OnPrimaryContainer
                        )
                        Text(
                            text = stringResource(Res.string.btn_run_sample_benchmark),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.OnPrimaryContainer
                        )
                    }
                }
            }

            if (state.traces.isNotEmpty()) {
                Surface(
                    onClick = { viewModel.clearAllTraces() },
                    shape = CircleShape,
                    color = Lipi.Error.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Lipi.Error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.btn_clear_history),
                            modifier = Modifier.size(14.dp),
                            tint = Lipi.Error
                        )
                        Text(
                            text = stringResource(Res.string.btn_clear_traces),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (scanSessions.isEmpty()) {
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
                items(scanSessions, key = { it.sessionId }) { session ->
                    ScanSessionCard(
                        session = session,
                        isExpanded = expandedSessionId == session.sessionId,
                        onToggleExpand = {
                            expandedSessionId = if (expandedSessionId == session.sessionId) null else session.sessionId
                        }
                    )
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
private fun ScanSessionCard(
    session: TelemetryScanSession,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val statusColor = if (session.isSuccess) Lipi.Primary else Lipi.Error
    val formattedTimeText = formatDuration(session.totalTimeMs)

    Surface(
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isExpanded) Lipi.Primary.copy(alpha = 0.5f) else Lipi.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (session.isSuccess) stringResource(Res.string.status_passed) else stringResource(Res.string.status_failed),
                                color = statusColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = session.formattedDateTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Ink
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Lipi.Background
                        ) {
                            Text(
                                text = formattedTimeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Ink,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${session.totalHops} ${stringResource(Res.string.label_hops)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Muted
                        )

                        if (session.finalItemCount > 0) {
                            Text("•", fontSize = 11.sp, color = Lipi.Muted)
                            Text(
                                text = "${session.finalItemCount} ${stringResource(Res.string.label_items)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Primary
                            )
                        }
                    }

                    if (session.modelSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.modelSummary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Lipi.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (session.failedTurnCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.label_turn_failed, session.failedTurnCount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Error
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Lipi.Background
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) stringResource(Res.string.cd_collapse) else stringResource(Res.string.cd_expand),
                            tint = Lipi.Ink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Lipi.Outline.copy(alpha = 0.5f), thickness = 1.dp)

                    Text(
                        text = stringResource(Res.string.label_turn_breakdown),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Muted
                    )

                    session.traces.forEach { trace ->
                        TraceTurnItem(trace = trace)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceTurnItem(trace: AgentExecutionTraceEntity) {
    val statusColor = if (trace.isSuccess) Lipi.Primary else Lipi.Error
    val agentDisplayName = stringResource(RxAgentType.fromId(trace.agentType).displayNameRes)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Lipi.Background,
        border = BorderStroke(1.dp, Lipi.Outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Lipi.PrimaryContainer
                    ) {
                        Text(
                            text = stringResource(Res.string.label_turn_number, trace.turnNumber),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.OnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = agentDisplayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                }

                Text(
                    text = if (trace.isSuccess) formatDuration(trace.executionTimeMs) else stringResource(Res.string.status_failed),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Text(
                text = "${trace.provider} • ${trace.modelName}",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Lipi.Muted
            )

            if (trace.reasoning.isNotBlank()) {
                Text(
                    text = trace.reasoning,
                    fontSize = 11.5.sp,
                    color = Lipi.Ink,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (trace.errorMessage.isNotBlank()) {
                Text(
                    text = trace.errorMessage,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.Error
                )
            }
        }
    }
}

@Composable
private fun formatDuration(totalTimeMs: Long): String {
    if (totalTimeMs <= 0L) return stringResource(Res.string.unit_millis, 0)

    val hours = totalTimeMs / (1000 * 60 * 60)
    val minutes = (totalTimeMs % (1000 * 60 * 60)) / (1000 * 60)
    val seconds = (totalTimeMs % (1000 * 60)) / 1000
    val remMs = totalTimeMs % 1000

    return when {
        hours > 0 -> {
            val hStr = stringResource(Res.string.unit_hours, hours)
            val mStr = stringResource(Res.string.unit_minutes, minutes)
            val sStr = stringResource(Res.string.unit_seconds, seconds)
            "$hStr $mStr $sStr"
        }
        minutes > 0 -> {
            val mStr = stringResource(Res.string.unit_minutes, minutes)
            val sStr = stringResource(Res.string.unit_seconds, seconds)
            "$mStr $sStr"
        }
        seconds > 0 -> {
            val sStr = stringResource(Res.string.unit_seconds, seconds)
            if (remMs > 0) {
                val msStr = stringResource(Res.string.unit_millis, remMs)
                "$sStr $msStr"
            } else {
                sStr
            }
        }
        else -> stringResource(Res.string.unit_millis, remMs)
    }
}

private fun groupTracesIntoScanSessions(rawTraces: List<AgentExecutionTraceEntity>): List<TelemetryScanSession> {
    if (rawTraces.isEmpty()) return emptyList()

    val chronologicalTraces = rawTraces.sortedBy { it.id }
    val sessionGroups = mutableListOf<MutableList<AgentExecutionTraceEntity>>()
    var currentGroup: MutableList<AgentExecutionTraceEntity>? = null

    for (trace in chronologicalTraces) {
        val lastTrace = currentGroup?.lastOrNull()
        val isNewScan = currentGroup == null ||
                (trace.turnNumber == 1 && trace.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true)) ||
                (trace.turnNumber == 1 && lastTrace?.turnNumber != 1) ||
                (lastTrace != null && (trace.timestamp - lastTrace.timestamp) > 30_000L)

        if (isNewScan) {
            currentGroup = mutableListOf()
            sessionGroups.add(currentGroup)
        }
        currentGroup.add(trace)
    }

    return sessionGroups.mapIndexed { idx, sessionTraces ->
        val firstTrace = sessionTraces.first()
        val lastTrace = sessionTraces.last()
        val totalTime = sessionTraces.sumOf { it.executionTimeMs }
        val allSuccessful = sessionTraces.all { it.isSuccess }
        val failedCount = sessionTraces.count { !it.isSuccess }

        val providers = sessionTraces.map { it.provider }.filter { it.isNotBlank() }.distinct()
        val models = sessionTraces.map { it.modelName }.filter { it.isNotBlank() }.distinct()

        val summary = when {
            providers.isEmpty() -> ""
            providers.size == 1 && models.size == 1 -> "${providers.first()} • ${models.first()}"
            providers.size == 1 -> "${providers.first()} (${models.joinToString(", ")})"
            else -> "Multi-Provider (${providers.joinToString(", ")})"
        }

        TelemetryScanSession(
            sessionId = "scan_${firstTrace.id}_$idx",
            timestamp = lastTrace.timestamp,
            formattedDateTime = lastTrace.formattedDateTime,
            totalTimeMs = totalTime,
            totalHops = sessionTraces.size,
            isSuccess = allSuccessful,
            failedTurnCount = failedCount,
            finalItemCount = lastTrace.itemCount,
            modelSummary = summary,
            traces = sessionTraces
        )
    }.sortedByDescending { it.timestamp }
}
