package com.snehonir.lipirx.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.presentation.common.InfoChip
import com.snehonir.lipirx.presentation.common.LipiRxScaffold
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_clear_history
import com.snehonir.lipirx.resources.btn_delete
import com.snehonir.lipirx.resources.confirm_clear_all_traces_msg
import com.snehonir.lipirx.resources.confirm_clear_all_traces_title
import com.snehonir.lipirx.resources.confirm_delete_scan_traces_msg
import com.snehonir.lipirx.resources.confirm_delete_scan_traces_title
import com.snehonir.lipirx.resources.history_empty_subtitle
import com.snehonir.lipirx.resources.history_empty_title
import com.snehonir.lipirx.resources.history_title
import com.snehonir.lipirx.resources.label_count_medicines
import com.snehonir.lipirx.resources.label_one_medicine
import com.snehonir.lipirx.resources.saved_scans_count
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

@Composable
fun HistoryView(
    historyList: List<PrescriptionHistoryEntity>,
    onClose: () -> Unit,
    onSelectRecord: (record: PrescriptionHistoryEntity) -> Unit,
    onDeleteRecord: (id: Long) -> Unit = {},
    onClearAllHistory: () -> Unit = {}
) {
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PrescriptionHistoryEntity?>(null) }

    LipiRxScaffold(
        title = stringResource(Res.string.history_title),
        onBack = onClose,
        backContentDescription = stringResource(Res.string.history_title)
    ) { innerPadding ->
        if (historyList.isEmpty()) {
            HistoryEmptyState(innerPadding = innerPadding)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.saved_scans_count, historyList.size),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.Muted
                    )
                    TextButton(
                        onClick = { showClearAllConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = Lipi.Danger)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.btn_clear_history),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 4.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { it.id }) { record ->
                        HistoryCard(
                            record = record,
                            onSelect = { onSelectRecord(record) },
                            onDelete = { itemToDelete = record }
                        )
                    }
                }
            }
        }
    }

    itemToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(Res.string.confirm_delete_scan_traces_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.confirm_delete_scan_traces_msg,
                        1,
                        record.formattedDateTime
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(record.id)
                        itemToDelete = null
                    }
                ) {
                    Text(stringResource(Res.string.btn_delete), color = Lipi.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text(stringResource(Res.string.confirm_clear_all_traces_title)) },
            text = { Text(stringResource(Res.string.confirm_clear_all_traces_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearAllConfirm = false
                    }
                ) {
                    Text(stringResource(Res.string.btn_clear_history), color = Lipi.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun HistoryEmptyState(
    innerPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Lipi.PrimaryContainer,
                border = BorderStroke(1.dp, Lipi.CardBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Lipi.Primary
                    )
                }
            }

            Text(
                text = stringResource(Res.string.history_empty_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink
            )

            Text(
                text = stringResource(Res.string.history_empty_subtitle),
                fontSize = 14.sp,
                color = Lipi.Muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HistoryCard(
    record: PrescriptionHistoryEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val preview = remember(record.medicineSummary) {
        parseSummaryPreview(record.medicineSummary)
    }

    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Lipi.Primary
                    )
                    Text(
                        text = record.formattedDateTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                }

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete),
                    tint = Lipi.Muted
                )
            }

            if (preview.names.isNotEmpty()) {
                Text(
                    text = preview.names.joinToString(" · "),
                    fontSize = 13.sp,
                    color = Lipi.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip(
                    if (preview.count == 1) stringResource(Res.string.label_one_medicine) else stringResource(
                        Res.string.label_count_medicines,
                        preview.count
                    )
                )
            }
        }
    }
}

private data class HistorySummaryPreview(
    val count: Int,
    val flagged: Int,
    val names: List<String>
)

private val json = Json { ignoreUnknownKeys = true }

private fun parseSummaryPreview(summaryText: String): HistorySummaryPreview {
    if (summaryText.isBlank()) return HistorySummaryPreview(0, 0, emptyList())

    val items = runCatching {
        json.decodeFromString<List<MedicineItem>>(summaryText)
    }.getOrNull()

    if (items != null) {
        val names = items.map { it.formattedFullName }.filter { it.isNotBlank() }
        val flagged = items.count { it.needsReview }
        return HistorySummaryPreview(items.size, flagged, names)
    }

    val parts = summaryText.split("; ").filter { it.isNotBlank() }
    val names = parts.map { it.substringBefore(" (").trim().ifBlank { it.substringBefore(" [").trim() } }
    return HistorySummaryPreview(parts.size, 0, names)
}
