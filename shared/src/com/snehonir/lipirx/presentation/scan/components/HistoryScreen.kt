package com.snehonir.lipirx.presentation.scan.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
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
import com.snehonir.lipirx.ui.theme.Lipi
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
        // Top Navigation Bar
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
                            contentDescription = stringResource(Res.string.history_title),
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Ink
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.history_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )
            }

            if (historyList.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showClearAllConfirm = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Lipi.Error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Lipi.Error.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.btn_clear_history),
                            modifier = Modifier.size(15.dp),
                            tint = Lipi.Danger
                        )
                        Text(
                            text = stringResource(Res.string.btn_clear_history),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Danger
                        )
                    }
                }
            }
        }

        // Sub-Header Stats Bar
        if (historyList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Lipi.PrimaryContainer,
                    border = BorderStroke(1.dp, Lipi.Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Lipi.Primary
                        )
                        Text(
                            text = "${historyList.size} Saved Scans",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.OnPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (historyList.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Lipi.Muted
                    )
                    Text(
                        text = stringResource(Res.string.history_empty_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                    Text(
                        text = stringResource(Res.string.history_empty_subtitle),
                        fontSize = 13.sp,
                        color = Lipi.Muted
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
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
private fun HistoryCard(
    record: PrescriptionHistoryEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.formattedDateTime,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.medicineSummary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.btn_delete),
                        tint = Lipi.Danger,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Lipi.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
