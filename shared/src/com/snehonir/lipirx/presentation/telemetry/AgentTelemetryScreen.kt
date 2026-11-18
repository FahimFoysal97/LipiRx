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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.snehonir.lipirx.presentation.scan.components.SecondaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_clear_traces
import com.snehonir.lipirx.resources.btn_delete
import com.snehonir.lipirx.resources.btn_run_sample_benchmark
import com.snehonir.lipirx.resources.btn_show_less
import com.snehonir.lipirx.resources.btn_show_more
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.confirm_clear_all_traces_msg
import com.snehonir.lipirx.resources.confirm_clear_all_traces_title
import com.snehonir.lipirx.resources.confirm_delete_scan_traces_msg
import com.snehonir.lipirx.resources.confirm_delete_scan_traces_title
import com.snehonir.lipirx.resources.header_trace_history
import com.snehonir.lipirx.resources.label_hops
import com.snehonir.lipirx.resources.label_items
import com.snehonir.lipirx.resources.label_turn_breakdown
import com.snehonir.lipirx.resources.label_turn_failed
import com.snehonir.lipirx.resources.label_turn_number
import com.snehonir.lipirx.resources.metric_success_rate
import com.snehonir.lipirx.resources.metric_total_hops
import com.snehonir.lipirx.resources.metric_total_latency
import com.snehonir.lipirx.resources.progress_running_benchmark
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
    var showClearConfirm by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<TelemetryScanSession?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AppHeader(
            onBack = onClose,
            backContentDescription = stringResource(Res.string.telemetry_title),
            title = stringResource(Res.string.telemetry_title),
            subtitle = stringResource(Res.string.telemetry_subtitle)
        )

        if (scanSessions.isEmpty()) {
            TelemetryEmptyState(
                isLoading = state.isLoading,
                onRunBenchmark = { viewModel.runSampleBenchmarkTest() }
            )
        } else {
            // The whole page is one scroll: metrics, actions and sessions move together,
            // so the list gets the full screen height.
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.header_trace_history),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Ink,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        TextButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Lipi.Danger)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.btn_clear_traces),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    if (state.isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Lipi.Primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(Res.string.progress_running_benchmark),
                                fontSize = 13.sp,
                                color = Lipi.Muted
                            )
                        }
                    } else {
                        SecondaryButton(
                            text = stringResource(Res.string.btn_run_sample_benchmark),
                            icon = Icons.Default.Refresh,
                            onClick = { viewModel.runSampleBenchmarkTest() },
                            modifier = Modifier.fillMaxWidth(),
                            height = 48.dp,
                            contentColor = Lipi.Primary,
                            fontSize = 14
                        )
                    }
                }

                items(scanSessions, key = { it.sessionId }) { session ->
                    ScanSessionCard(
                        session = session,
                        isExpanded = expandedSessionId == session.sessionId,
                        onToggleExpand = {
                            expandedSessionId = if (expandedSessionId == session.sessionId) null else session.sessionId
                        },
                        onDelete = { sessionToDelete = session }
                    )
                }
            }
        }
    }

    // Deleting one scan's traces also asks first.
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(Res.string.confirm_delete_scan_traces_title)) },
            text = {
                Text(stringResource(Res.string.confirm_delete_scan_traces_msg, session.totalHops, session.formattedDateTime))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTraces(session.traces.map { it.id })
                        if (expandedSessionId == session.sessionId) expandedSessionId = null
                        sessionToDelete = null
                    }
                ) {
                    Text(stringResource(Res.string.btn_delete), color = Lipi.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
            }
        )
    }

    // Clearing is destructive, so it asks first.
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(Res.string.confirm_clear_all_traces_title)) },
            text = { Text(stringResource(Res.string.confirm_clear_all_traces_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllTraces()
                        showClearConfirm = false
                    }
                ) {
                    Text(stringResource(Res.string.btn_clear_traces), color = Lipi.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun TelemetryEmptyState(
    isLoading: Boolean,
    onRunBenchmark: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
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
                    icon = Icons.Default.Refresh,
                    onClick = onRunBenchmark,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = Lipi.Muted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 17.sp,
                color = Lipi.Ink,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScanSessionCard(
    session: TelemetryScanSession,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    // Green for pass / red for fail reads faster than brand teal vs red.
    val statusColor = if (session.isSuccess) Lipi.StatusActiveGreen else Lipi.Error
    val formattedTimeText = formatDuration(session.totalTimeMs)
    val slowestTurnMs = remember(session.traces) {
        (session.traces.maxOfOrNull { it.executionTimeMs } ?: 1L).coerceAtLeast(1L)
    }

    Surface(
        onClick = onToggleExpand,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Lipi.Surface,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Muted (not red) so a list of cards isn't a wall of red icons;
                    // the confirmation dialog carries the warning.
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.btn_delete),
                            tint = Lipi.Muted,
                            modifier = Modifier.size(20.dp)
                        )
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
                        TraceTurnItem(trace = trace, slowestTurnMs = slowestTurnMs)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceTurnItem(
    trace: AgentExecutionTraceEntity,
    slowestTurnMs: Long
) {
    val statusColor = if (trace.isSuccess) Lipi.StatusActiveGreen else Lipi.Error
    val agentDisplayName = stringResource(RxAgentType.fromId(trace.agentType).displayNameRes)

    // Long reasoning is clamped to 4 lines; tapping the turn expands it.
    var reasoningExpanded by remember { mutableStateOf(false) }
    val isLongReasoning = trace.reasoning.length > 180

    // Share of the slowest turn in this scan: shows at a glance which agent is the bottleneck.
    val latencyShare = (trace.executionTimeMs.toFloat() / slowestTurnMs.toFloat()).coerceIn(0.03f, 1f)

    Surface(
        onClick = { reasoningExpanded = !reasoningExpanded },
        enabled = isLongReasoning,
        shape = RoundedCornerShape(12.dp),
        color = Lipi.Background,
        border = BorderStroke(1.dp, Lipi.Outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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

            // Latency bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Lipi.CardBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(latencyShare)
                        .height(4.dp)
                        .background(statusColor, CircleShape)
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
                    lineHeight = 16.sp,
                    color = Lipi.Ink,
                    maxLines = if (reasoningExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (isLongReasoning) {
                    Text(
                        text = if (reasoningExpanded) stringResource(Res.string.btn_show_less) else stringResource(Res.string.btn_show_more),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Primary
                    )
                }
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