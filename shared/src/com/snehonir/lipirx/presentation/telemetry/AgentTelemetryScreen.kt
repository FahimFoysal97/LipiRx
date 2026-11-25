package com.snehonir.lipirx.presentation.telemetry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import com.snehonir.lipirx.presentation.telemetry.components.MetricCard
import com.snehonir.lipirx.presentation.telemetry.components.TelemetryEmptyState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.entity.AgentExecutionTraceEntity
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.presentation.common.LipiRxScaffold
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.presentation.common.SecondaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_clear_traces
import com.snehonir.lipirx.resources.btn_copy_trace
import com.snehonir.lipirx.resources.btn_delete
import com.snehonir.lipirx.resources.btn_run_sample_benchmark
import com.snehonir.lipirx.resources.btn_show_less
import com.snehonir.lipirx.resources.btn_show_more
import com.snehonir.lipirx.resources.cd_back
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.confirm_clear_all_traces_msg
import com.snehonir.lipirx.resources.confirm_clear_all_traces_title
import com.snehonir.lipirx.resources.confirm_delete_scan_traces_msg
import com.snehonir.lipirx.resources.confirm_delete_scan_traces_title
import com.snehonir.lipirx.resources.filter_all
import com.snehonir.lipirx.resources.filter_failed
import com.snehonir.lipirx.resources.header_trace_history
import com.snehonir.lipirx.resources.label_hops_count
import com.snehonir.lipirx.resources.label_items_count
import com.snehonir.lipirx.resources.label_latest
import com.snehonir.lipirx.resources.label_no_matching_scans
import com.snehonir.lipirx.resources.label_parallel_execution
import com.snehonir.lipirx.resources.label_turn_breakdown
import com.snehonir.lipirx.resources.label_turn_failed
import com.snehonir.lipirx.resources.label_turn_number
import com.snehonir.lipirx.resources.metric_avg_latency
import com.snehonir.lipirx.resources.metric_slowest_scan
import com.snehonir.lipirx.resources.metric_success_rate
import com.snehonir.lipirx.resources.outcome_entity_parser
import com.snehonir.lipirx.resources.outcome_generic_substitute
import com.snehonir.lipirx.resources.outcome_generic_substitute_raw
import com.snehonir.lipirx.resources.outcome_safety_auditor_flagged
import com.snehonir.lipirx.resources.outcome_safety_auditor_passed
import com.snehonir.lipirx.resources.outcome_transcription
import com.snehonir.lipirx.resources.outcome_transcription_raw
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
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

// ───────────────────────────── Constants & helpers ─────────────────────────────

/** Number of LazyColumn items that come before the session list (metrics, header, benchmark). */
private const val HEADER_ITEM_COUNT = 3
private const val TREND_SCAN_COUNT = 20
private const val SLOW_THRESHOLD_MS = 3_000L
private const val WARN_THRESHOLD_MS = 1_000L
private val LatencyAmber = Color(0xFFF59E0B)

private enum class TelemetryFilter { ALL, FAILED }

/** Neutral under 1s, amber 1–3s, red above. A raw "2.4s" means nothing without context. */
@Composable
private fun latencyColor(ms: Long): Color = when {
    ms >= SLOW_THRESHOLD_MS -> Lipi.Error
    ms >= WARN_THRESHOLD_MS -> LatencyAmber
    else -> Lipi.Ink
}

// ───────────────────────────── Duration formatting ─────────────────────────────
// The unit strings are resolved once and shared, so the formatter is a plain function
// instead of calling stringResource() up to 3x per call site.

private const val DURATION_TOKEN = "\u0001"

private class DurationLabels(
    private val hours: String,
    private val minutes: String,
    private val seconds: String,
    private val millis: String
) {
    private fun String.with(value: Long) = replace(DURATION_TOKEN, value.toString())

    fun format(totalTimeMs: Long): String {
        if (totalTimeMs <= 0L) return millis.with(0)

        val h = totalTimeMs / (1000 * 60 * 60)
        val m = (totalTimeMs % (1000 * 60 * 60)) / (1000 * 60)
        val s = (totalTimeMs % (1000 * 60)) / 1000
        val ms = totalTimeMs % 1000

        return when {
            h > 0 -> "${hours.with(h)} ${minutes.with(m)} ${seconds.with(s)}"
            m > 0 -> "${minutes.with(m)} ${seconds.with(s)}"
            s > 0 -> if (ms > 0) "${seconds.with(s)} ${millis.with(ms)}" else seconds.with(s)
            else -> millis.with(ms)
        }
    }
}

@Composable
private fun rememberDurationLabels(): DurationLabels {
    val h = stringResource(Res.string.unit_hours, DURATION_TOKEN)
    val m = stringResource(Res.string.unit_minutes, DURATION_TOKEN)
    val s = stringResource(Res.string.unit_seconds, DURATION_TOKEN)
    val ms = stringResource(Res.string.unit_millis, DURATION_TOKEN)
    return remember(h, m, s, ms) { DurationLabels(h, m, s, ms) }
}

private val LocalDurationLabels = staticCompositionLocalOf<DurationLabels> {
    error("DurationLabels not provided")
}

@Composable
private fun formatDuration(totalTimeMs: Long): String = LocalDurationLabels.current.format(totalTimeMs)

// ───────────────────────────── JSON export ─────────────────────────────

private fun jsonEscape(s: String): String = buildString {
    for (c in s) {
        when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c < ' ' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> append(c)
        }
    }
}

private fun sessionToJson(session: TelemetryScanSession): String {
    val traces = session.traces.joinToString(",\n") { t ->
        """    {"id": ${t.id}, "turn": ${t.turnNumber}, "agent": "${jsonEscape(t.agentType)}", """ +
                """"provider": "${jsonEscape(t.provider)}", "model": "${jsonEscape(t.modelName)}", """ +
                """"timeMs": ${t.executionTimeMs}, "success": ${t.isSuccess}, "items": ${t.itemCount}, """ +
                """"reasoning": "${jsonEscape(t.reasoning)}", "error": "${jsonEscape(t.errorMessage)}"}"""
    }
    return """{
  "sessionId": "${jsonEscape(session.sessionId)}",
  "time": "${jsonEscape(session.formattedDateTime)}",
  "success": ${session.isSuccess},
  "totalTimeMs": ${session.totalTimeMs},
  "hops": ${session.totalHops},
  "traces": [
$traces
  ]
}"""
}

// ───────────────────────────── Screen ─────────────────────────────

@Composable
fun AgentTelemetryScreen(
    viewModel: AgentTraceViewModel,
    onClose: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadAgentTraces()
    }

    val state by viewModel.uiState.collectAsState()
    val durationLabels = rememberDurationLabels()

    val scanSessions = remember(state.traces) {
        groupTracesIntoScanSessions(state.traces)
    }

    var filter by remember { mutableStateOf(TelemetryFilter.ALL) }
    val visibleSessions = remember(scanSessions, filter) {
        if (filter == TelemetryFilter.FAILED) scanSessions.filterNot { it.isSuccess } else scanSessions
    }

    // Assumes groupTracesIntoScanSessions returns newest first.
    val latestSessionId = scanSessions.firstOrNull()?.sessionId

    val avgLatencyMs = remember(scanSessions) {
        if (scanSessions.isEmpty()) 0L else scanSessions.map { it.totalTimeMs }.average().toLong()
    }
    val slowestScanMs = remember(scanSessions) { scanSessions.maxOfOrNull { it.totalTimeMs } ?: 0L }
    val trend = remember(scanSessions) { scanSessions.take(TREND_SCAN_COUNT).map { it.totalTimeMs }.reversed() }

    var expandedSessionId by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<TelemetryScanSession?>(null) }

    val listState = rememberLazyListState()

    // Bring a freshly expanded card into view instead of leaving it half off-screen.
    LaunchedEffect(expandedSessionId) {
        val id = expandedSessionId ?: return@LaunchedEffect
        val index = visibleSessions.indexOfFirst { it.sessionId == id }
        if (index >= 0) listState.animateScrollToItem(HEADER_ITEM_COUNT + index)
    }

    CompositionLocalProvider(LocalDurationLabels provides durationLabels) {
        LipiRxScaffold(
            title = stringResource(Res.string.telemetry_title),
            subtitle = stringResource(Res.string.telemetry_subtitle),
            onBack = onClose,
            backContentDescription = stringResource(Res.string.cd_back)
        ) { innerPadding ->
            if (scanSessions.isEmpty()) {
                TelemetryEmptyState(
                    innerPadding = innerPadding,
                    isLoading = state.isLoading,
                    onRunBenchmark = { viewModel.runSampleBenchmarkTest() }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    // Bottom inset lives in contentPadding so the list scrolls behind the nav bar.
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp) +
                            WindowInsets.navigationBars.asPaddingValues(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 0 — health summary
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = stringResource(Res.string.metric_success_rate),
                                value = "${(state.successRate * 100).roundToInt()}%",
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            MetricCard(
                                title = stringResource(Res.string.metric_avg_latency),
                                value = formatDuration(avgLatencyMs),
                                valueColor = latencyColor(avgLatencyMs),
                                trend = trend,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            MetricCard(
                                title = stringResource(Res.string.metric_slowest_scan),
                                value = formatDuration(slowestScanMs),
                                valueColor = latencyColor(slowestScanMs),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }

                    // 1 — title, clear, filters
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            FilterRow(
                                selected = filter,
                                onSelect = { filter = it }
                            )
                        }
                    }

                    // 2 — benchmark action (same slot while running, so the layout doesn't jump)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SecondaryButton(
                                text = if (state.isLoading) {
                                    stringResource(Res.string.progress_running_benchmark)
                                } else {
                                    stringResource(Res.string.btn_run_sample_benchmark)
                                },
                                icon = Icons.Default.PlayArrow,
                                onClick = { if (!state.isLoading) viewModel.runSampleBenchmarkTest() },
                                modifier = Modifier.fillMaxWidth(),
                                height = 48.dp,
                                contentColor = Lipi.Primary,
                                fontSize = 14
                            )
                            Box(Modifier.fillMaxWidth().height(3.dp)) {
                                if (state.isLoading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Lipi.Primary,
                                        trackColor = Lipi.CardBorder
                                    )
                                }
                            }
                        }
                    }

                    if (visibleSessions.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.label_no_matching_scans),
                                fontSize = 13.sp,
                                color = Lipi.Muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            )
                        }
                    }

                    items(visibleSessions, key = { it.sessionId }) { session ->
                        ScanSessionCard(
                            session = session,
                            isLatest = session.sessionId == latestSessionId,
                            isExpanded = expandedSessionId == session.sessionId,
                            onToggleExpand = {
                                expandedSessionId =
                                    if (expandedSessionId == session.sessionId) null else session.sessionId
                            },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        }

        // Deleting one scan's traces asks first.
        sessionToDelete?.let { session ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text(stringResource(Res.string.confirm_delete_scan_traces_title)) },
                text = {
                    Text(
                        stringResource(
                            Res.string.confirm_delete_scan_traces_msg,
                            session.totalHops,
                            session.formattedDateTime
                        )
                    )
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

        // Clearing everything is destructive, so it asks first.
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
}

// ───────────────────────────── Filters ─────────────────────────────

@Composable
private fun FilterRow(
    selected: TelemetryFilter,
    onSelect: (TelemetryFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == TelemetryFilter.ALL,
            onClick = { onSelect(TelemetryFilter.ALL) },
            label = { Text(stringResource(Res.string.filter_all), fontSize = 12.sp) }
        )
        FilterChip(
            selected = selected == TelemetryFilter.FAILED,
            onClick = { onSelect(TelemetryFilter.FAILED) },
            label = { Text(stringResource(Res.string.filter_failed), fontSize = 12.sp) }
        )
    }
}

// ───────────────────────────── Session card ─────────────────────────────

@Composable
private fun StatusBadge(isSuccess: Boolean) {
    // Icon + text, so pass/fail never relies on color alone.
    val color = if (isSuccess) Lipi.StatusActiveGreen else Lipi.Error
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isSuccess) stringResource(Res.string.status_passed) else stringResource(Res.string.status_failed),
                color = color,
                maxLines = 1,
                softWrap = false,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * The pipeline in execution order, one step per turn: supervisor first, then each turn's workers.
 * Workers that ran in the same turn are parallel, so they share one outlined pill instead of
 * being drawn as a sequence.
 */
@Composable
private fun PipelineChain(turns: List<TelemetryTurnGroup>) {
    val supervisorOk = remember(turns) {
        val runs = turns.mapNotNull { it.supervisorTrace }
        if (runs.isEmpty()) null else runs.all { it.isSuccess }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var first = true
        if (supervisorOk != null) {
            Icon(
                imageVector = RxAgentType.SUPERVISOR.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (supervisorOk) Lipi.StatusActiveGreen else Lipi.Error
            )
            first = false
        }
        turns.forEach { turn ->
            if (turn.workerTraces.isEmpty()) return@forEach
            if (!first) Text("›", fontSize = 12.sp, color = Lipi.Muted)
            first = false

            if (turn.workerTraces.size == 1) {
                ChainIcon(turn.workerTraces.first())
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Lipi.Background)
                        .border(1.dp, Lipi.CardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    turn.workerTraces.forEachIndexed { i, trace ->
                        if (i > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(Lipi.CardBorder)
                            )
                        }
                        ChainIcon(trace)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChainIcon(trace: AgentExecutionTraceEntity) {
    Icon(
        imageVector = RxAgentType.fromId(trace.agentType).icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = if (trace.isSuccess) Lipi.StatusActiveGreen else Lipi.Error
    )
}

/** One segment per turn, width proportional to its duration. Failed turns are red. */
@Composable
private fun TurnTimeBar(turns: List<TelemetryTurnGroup>) {
    val total = turns.sumOf { it.turnDurationMs }.coerceAtLeast(1L).toFloat()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        turns.forEach { turn ->
            val failed = turn.workerTraces.any { !it.isSuccess }
            Box(
                modifier = Modifier
                    .weight((turn.turnDurationMs / total).coerceAtLeast(0.03f))
                    .fillMaxHeight()
                    .background(if (failed) Lipi.Error else Lipi.Primary.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun ScanSessionCard(
    session: TelemetryScanSession,
    isLatest: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val turnGroups = remember(session.traces) { groupTracesByTurn(session.traces) }
    val slowestTraceMs = remember(session.traces) {
        (session.traces.maxOfOrNull { it.executionTimeMs } ?: 1L).coerceAtLeast(1L)
    }
    val firstError = remember(session.traces) {
        session.traces
            .firstOrNull { it.errorMessage.isNotBlank() }
            ?.errorMessage
            ?.lineSequence()
            ?.first()
    }
    val clipboard = LocalClipboardManager.current
    val stateText = if (isExpanded) stringResource(Res.string.cd_collapse) else stringResource(Res.string.cd_expand)

    Surface(
        onClick = onToggleExpand,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                stateDescription = stateText
            },
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status · exact timestamp · "Latest"
                    // FlowRow: if the timestamp is long, "Latest" wraps to the next line as a whole chip.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(isSuccess = session.isSuccess)

                        // Absolute time on purpose: you correlate traces with logs and bug reports.
                        Text(
                            text = session.formattedDateTime,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Ink
                        )

                        if (isLatest) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Lipi.PrimaryContainer
                            ) {
                                Text(
                                    text = stringResource(Res.string.label_latest),
                                    maxLines = 1,
                                    softWrap = false,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Lipi.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Duration · hops · items
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Lipi.Background
                        ) {
                            Text(
                                text = formatDuration(session.totalTimeMs),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = latencyColor(session.totalTimeMs),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = stringResource(Res.string.label_hops_count, session.totalHops),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Muted
                        )

                        if (session.finalItemCount > 0) {
                            Text("•", fontSize = 12.sp, color = Lipi.Muted)
                            Text(
                                text = stringResource(Res.string.label_items_count, session.finalItemCount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Primary
                            )
                        }
                    }

                    // Path + where the time went, visible without expanding.
                    PipelineChain(turns = turnGroups)
                    TurnTimeBar(turns = turnGroups)

                    if (session.modelSummary.isNotBlank()) {
                        Text(
                            text = session.modelSummary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Lipi.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (session.failedTurnCount > 0) {
                        Text(
                            text = stringResource(Res.string.label_turn_failed, session.failedTurnCount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Error
                        )
                    }

                    // Why it failed, without having to expand.
                    if (!isExpanded && firstError != null) {
                        Text(
                            text = firstError,
                            fontSize = 12.sp,
                            color = Lipi.Error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            // The whole card is the button (see semantics above), so no second announcement.
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
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
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = Lipi.Outline.copy(alpha = 0.5f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.label_turn_breakdown),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Muted
                        )
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(sessionToJson(session))) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Lipi.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.btn_copy_trace),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Flat timeline: one vertical line, one dot per turn, no nested card borders.
                    Column {
                        turnGroups.forEachIndexed { index, group ->
                            TurnTimelineItem(
                                group = group,
                                slowestTraceMs = slowestTraceMs,
                                isLast = index == turnGroups.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Turn grouping ─────────────────────────────

private data class TelemetryTurnGroup(
    val turnNumber: Int,
    val supervisorTrace: AgentExecutionTraceEntity?,
    val workerTraces: List<AgentExecutionTraceEntity>,
    val turnDurationMs: Long,
    val isParallel: Boolean
)

private fun groupTracesByTurn(traces: List<AgentExecutionTraceEntity>): List<TelemetryTurnGroup> {
    return traces.groupBy { it.turnNumber }.entries.map { (turnNum, turnTraces) ->
        val supervisorTrace = turnTraces.firstOrNull { it.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true) }
        val workerTraces = turnTraces.filterNot { it.agentType.equals(RxAgentType.SUPERVISOR.id, ignoreCase = true) }
        val supervisorMs = supervisorTrace?.executionTimeMs ?: 0L
        val maxWorkerMs = workerTraces.maxOfOrNull { it.executionTimeMs } ?: 0L
        TelemetryTurnGroup(
            turnNumber = turnNum,
            supervisorTrace = supervisorTrace,
            workerTraces = workerTraces,
            turnDurationMs = supervisorMs + maxWorkerMs,
            isParallel = workerTraces.size > 1
        )
    }.sortedBy { it.turnNumber }
}

// ───────────────────────────── Timeline ─────────────────────────────

@Composable
private fun TurnTimelineItem(
    group: TelemetryTurnGroup,
    slowestTraceMs: Long,
    isLast: Boolean
) {
    val failed = group.workerTraces.any { !it.isSuccess }
    val dotColor = if (failed) Lipi.Error else Lipi.Primary
    val lineColor = Lipi.Outline.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Vertical rail through the dot center; the last turn ends at its dot.
                val x = 7.dp.toPx()
                val startY = 15.dp.toPx()
                drawLine(
                    color = lineColor,
                    start = Offset(x, startY),
                    end = Offset(x, if (isLast) startY else size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(14.dp)
                .background(dotColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.label_turn_number, group.turnNumber),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink
                    )
                    Text(
                        text = formatDuration(group.turnDurationMs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = latencyColor(group.turnDurationMs)
                    )
                }

                if (group.isParallel) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Lipi.PrimaryContainer
                    ) {
                        Text(
                            text = stringResource(Res.string.label_parallel_execution),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            group.supervisorTrace?.let { SupervisorTurnBlock(supervisor = it) }

            if (group.workerTraces.isNotEmpty()) {
                val railColor = Lipi.Outline.copy(alpha = 0.5f)
                Column(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .drawBehind {
                            drawLine(
                                color = railColor,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    group.workerTraces.forEach { workerTrace ->
                        WorkerTurnRow(trace = workerTrace, slowestTraceMs = slowestTraceMs)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupervisorTurnBlock(supervisor: AgentExecutionTraceEntity) {
    var reasoningExpanded by remember { mutableStateOf(false) }
    // Only offer "show more" when the text is actually clamped, not by a length guess.
    var hasOverflow by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Lipi.PrimaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, Lipi.Primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = RxAgentType.SUPERVISOR.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Lipi.Primary
                    )
                    Text(
                        text = stringResource(RxAgentType.SUPERVISOR.displayNameRes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.OnPrimaryContainer
                    )
                }

                Text(
                    text = "${supervisor.provider} • ${formatDuration(supervisor.executionTimeMs)}",
                    softWrap = false,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Lipi.Muted
                )
            }

            if (supervisor.reasoning.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = supervisor.reasoning,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Lipi.Ink,
                        maxLines = if (reasoningExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!reasoningExpanded) hasOverflow = it.hasVisualOverflow }
                    )
                }

                if (hasOverflow || reasoningExpanded) {
                    Text(
                        text = if (reasoningExpanded) stringResource(Res.string.btn_show_less) else stringResource(Res.string.btn_show_more),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Primary,
                        modifier = Modifier.clickable { reasoningExpanded = !reasoningExpanded }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerTurnRow(
    trace: AgentExecutionTraceEntity,
    slowestTraceMs: Long
) {
    val agentType = RxAgentType.fromId(trace.agentType)
    val statusColor = if (trace.isSuccess) Lipi.StatusActiveGreen else Lipi.Error
    val latencyShare = (trace.executionTimeMs.toFloat() / slowestTraceMs.toFloat()).coerceIn(0.03f, 1f)

    val outcomeSummary = when (agentType) {
        RxAgentType.TRANSCRIPTION -> if (trace.itemCount > 0) stringResource(Res.string.outcome_transcription, trace.itemCount) else stringResource(Res.string.outcome_transcription_raw)
        RxAgentType.ENTITY_PARSER -> stringResource(Res.string.outcome_entity_parser)
        RxAgentType.GENERIC_SUBSTITUTE -> if (trace.itemCount > 0) stringResource(Res.string.outcome_generic_substitute) else stringResource(Res.string.outcome_generic_substitute_raw)
        RxAgentType.SAFETY_AUDITOR -> if (trace.errorMessage.isNotBlank()) stringResource(Res.string.outcome_safety_auditor_flagged, trace.itemCount) else stringResource(Res.string.outcome_safety_auditor_passed)
        else -> trace.reasoning
    }

    // Flat row: no border or surface, only spacing and the rail drawn by the parent.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp),
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
                Icon(
                    imageVector = agentType.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Lipi.Primary
                )
                Text(
                    text = stringResource(agentType.displayNameRes),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (trace.isSuccess) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = statusColor
                )
                Text(
                    text = if (trace.isSuccess) formatDuration(trace.executionTimeMs) else stringResource(Res.string.status_failed),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (trace.isSuccess) latencyColor(trace.executionTimeMs) else Lipi.Error
                )
            }
        }

        // Latency bar, relative to the slowest trace in the scan.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(Lipi.CardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(latencyShare)
                    .height(3.dp)
                    .background(statusColor, CircleShape)
            )
        }

        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${trace.provider} • ${trace.modelName}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Lipi.Muted
                )

                Text(
                    text = outcomeSummary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Lipi.Ink,
                    fontWeight = FontWeight.Medium
                )

                if (trace.errorMessage.isNotBlank()) {
                    Text(
                        text = trace.errorMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.Error
                    )
                }
            }
        }
    }
}