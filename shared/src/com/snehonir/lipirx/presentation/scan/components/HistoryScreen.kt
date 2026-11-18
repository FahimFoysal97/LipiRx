package com.snehonir.lipirx.presentation.scan.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_clear_history
import com.snehonir.lipirx.resources.btn_delete
import com.snehonir.lipirx.resources.confirm_clear_all_msg
import com.snehonir.lipirx.resources.confirm_clear_all_title
import com.snehonir.lipirx.resources.confirm_delete_msg
import com.snehonir.lipirx.resources.confirm_delete_title
import com.snehonir.lipirx.resources.history_empty_subtitle
import com.snehonir.lipirx.resources.history_empty_title
import com.snehonir.lipirx.resources.history_title
import com.snehonir.lipirx.resources.saved_scans_count
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

@Composable
fun HistoryScreen(
    historyList: List<PrescriptionHistoryEntity>,
    onClose: () -> Unit,
    onSelectRecord: (PrescriptionHistoryEntity) -> Unit,
    onDeleteRecord: (id: Long) -> Unit = {},
    onClearAllHistory: () -> Unit = {}
) {
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PrescriptionHistoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AppHeader(
            onBack = onClose,
            backContentDescription = stringResource(Res.string.history_title),
            title = stringResource(Res.string.history_title)
        )

        if (historyList.isEmpty()) {
            HistoryEmptyState()
        } else {
            // Count on the left, destructive "clear all" on the right as a quiet text button.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp),
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
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

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(Res.string.confirm_delete_title)) },
            text = { Text(stringResource(Res.string.confirm_delete_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.id?.let { onDeleteRecord(it) }
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
            title = { Text(stringResource(Res.string.confirm_clear_all_title)) },
            text = { Text(stringResource(Res.string.confirm_clear_all_msg)) },
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
private fun HistoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                text = stringResource(Res.string.history_empty_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(Res.string.history_empty_subtitle),
                fontSize = 14.sp,
                lineHeight = 20.sp,
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
    val preview = remember(record.medicineSummary) { parseHistoryPreview(record.medicineSummary) }

    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. When
                Text(
                    text = record.formattedDateTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )

                // 2. At-a-glance status chips
                if (preview.count > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoChip(if (preview.count == 1) "1 medicine" else "${preview.count} medicines")
                        if (preview.flagged > 0) {
                            FlaggedChip("${preview.flagged} to review")
                        }
                    }
                }

                // 3. What (brand names only; open the record for full details)
                if (preview.text.isNotBlank()) {
                    Text(
                        text = preview.text,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Lipi.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Delete is muted (not red) so a list of cards isn't a wall of red icons;
            // the confirmation dialog carries the warning.
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.btn_delete),
                    tint = Lipi.Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Lipi.Primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )
        }
    }
}

@Composable
private fun FlaggedChip(text: String) {
    Surface(
        shape = CircleShape,
        color = Lipi.Error.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Lipi.Error
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Error
            )
        }
    }
}

private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

private data class HistoryPreview(
    val count: Int,
    val flagged: Int,
    val text: String,
)

/**
 * Turns the stored summary into card data. If it is the JSON list of MedicineItem we show
 * a count, how many need review, and the first few brand names; otherwise (older plain-text
 * records) we fall back to the raw text.
 */
private fun parseHistoryPreview(summaryText: String): HistoryPreview {
    if (summaryText.isBlank()) return HistoryPreview(0, 0, "")

    val items = runCatching {
        jsonParser.decodeFromString<List<MedicineItem>>(summaryText)
    }.getOrNull()

    if (!items.isNullOrEmpty()) {
        val names = items.map { item -> item.formattedFullName.ifBlank { item.medicineName } }
        val shown = names.take(3).joinToString(" · ")
        val extra = names.size - 3
        val text = if (extra > 0) "$shown  +$extra more" else shown
        return HistoryPreview(
            count = items.size,
            flagged = items.count { it.needsReview },
            text = text
        )
    }
    return HistoryPreview(0, 0, summaryText)
}