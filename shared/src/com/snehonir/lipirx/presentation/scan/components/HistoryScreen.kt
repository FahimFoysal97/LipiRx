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
import com.snehonir.lipirx.data.local.PrescriptionHistoryEntity
import com.snehonir.lipirx.ui.theme.Lipi

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
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Lipi.CardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Ink
                        )
                    }
                }

                Text(
                    text = "Scan History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip("${historyList.size} scans")

                if (historyList.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showClearAllConfirm = true },
                        shape = CircleShape,
                        color = Lipi.AmberContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All",
                                modifier = Modifier.size(13.dp),
                                tint = Lipi.Error
                            )
                            Text(
                                text = "Clear All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Error
                            )
                        }
                    }
                }
            }
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
                        text = "No prescription history found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                    Text(
                        text = "Scanned prescription summaries will appear here.",
                        fontSize = 13.sp,
                        color = Lipi.Muted
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        onClick = { onSelectRecord(record) },
                        onDelete = { itemToDelete = record }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Single Item Delete
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text("Delete History Record?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Lipi.Ink)
            },
            text = {
                Text("Are you sure you want to delete the scan record for '${itemToDelete?.formattedDateTime}'?", fontSize = 13.sp, color = Lipi.Muted)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDelete?.let { onDeleteRecord(it.id) }
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = Lipi.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", color = Lipi.Ink)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirmation Dialog for Clear All History
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text("Clear Scan History?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Lipi.Ink)
            },
            text = {
                Text("This will delete all saved prescription history records from local storage. This action cannot be undone.", fontSize = 13.sp, color = Lipi.Muted)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearAllConfirm = false
                    }
                ) {
                    Text("Clear All", color = Lipi.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Lipi.Ink)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun HistoryItemCard(
    record: PrescriptionHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Lipi.PrimaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Lipi.Primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // 12-hour AM/PM analyze start date & time with seconds
                Text(
                    text = record.formattedDateTime,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.medicineSummary.ifBlank { "Prescription analysis record" },
                    fontSize = 13.sp,
                    color = Lipi.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    modifier = Modifier.size(18.dp),
                    tint = Lipi.Error
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = "Open",
                modifier = Modifier.size(20.dp),
                tint = Lipi.Muted
            )
        }
    }
}
