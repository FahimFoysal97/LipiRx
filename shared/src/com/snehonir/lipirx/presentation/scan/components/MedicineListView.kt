package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.ui.theme.Lipi

@Composable
fun MedicineListView(
    imageByteArray: ByteArray?,
    medicines: List<MedicineItem>,
    onZoom: () -> Unit,
    onCopySummary: () -> Unit,
    onRescan: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            AppHeader { InfoChip("${medicines.size} medicines") }

            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (imageByteArray != null) {
                    item { PrescriptionThumbnail(imageByteArray, onZoom) }
                }
                itemsIndexed(medicines) { index, item -> MedicineItemCard(index + 1, item) }
            }
        }

        // Sticky bottom action bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryButton(
                    text = "Copy Summary",
                    icon = Icons.Default.Share,
                    onClick = onCopySummary,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                SecondaryButton(
                    text = "Rescan",
                    icon = Icons.Default.Refresh,
                    onClick = onRescan,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
            }
        }
    }
}

@Composable
private fun PrescriptionThumbnail(
    imageByteArray: ByteArray,
    onZoom: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onZoom),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = imageByteArray,
                contentDescription = "Prescription thumbnail",
                modifier = Modifier
                    .size(48.dp, 60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Lipi.ImageWell),
                contentScale = ContentScale.Crop,
            )
            Column {
                Text("Prescription scanned", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = Lipi.Muted)
                    Text("Tap to zoom", fontSize = 12.sp, color = Lipi.Muted)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicineItemCard(index: Int, item: MedicineItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Index badge + names
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Lipi.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$index", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.formattedFullName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
                    if (item.genericName.isNotBlank()) {
                        Text("Generic: ${item.genericName}", fontSize = 13.sp, color = Lipi.Muted)
                    }
                }
            }

            // Low Confidence Warning Badge
            if (item.confidence.equals("low", ignoreCase = true)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Lipi.AmberContainer
                ) {
                    Text(
                        text = "⚠️ Low Confidence: Verify handwritten spelling",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.OnAmber
                    )
                }
            }

            // Schedule + quantity + form + duration badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.schedule.isNotBlank()) {
                    Badge("Schedule: ${item.schedule}", Lipi.AmberContainer, Lipi.OnAmber)
                }
                Badge("Qty: ${item.formattedQuantity}", Lipi.PrimaryContainer, Lipi.OnPrimaryContainer)

                if (item.dosageForm.isNotBlank()) {
                    Badge("Form: ${item.dosageForm}", Lipi.Background, Lipi.Ink)
                }

                if (item.duration.isNotBlank()) {
                    Badge("Duration: ${item.duration}", Lipi.Background, Lipi.Ink)
                }

                if (item.instructions.isNotBlank()) {
                    Badge("Instructions: ${item.instructions}", Lipi.PrimaryContainer, Lipi.Primary)
                }

                if (item.totalVolumeNeededMl != null && item.totalVolumeNeededMl > 0) {
                    Badge("Total Vol: ${item.totalVolumeNeededMl.toInt()} ml", Lipi.AmberContainer, Lipi.OnAmber)
                }

                if (item.patientAgeGroup.isNotBlank()) {
                    Badge("Age: ${item.patientAgeGroup}", Lipi.Background, Lipi.Muted)
                }
            }

            // Substitute brands
            if (item.similarBrands.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Lipi.Background, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Lipi.Muted)
                        Text("Similar Brands", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Muted)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.similarBrands.forEach { name ->
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, Lipi.Outline)
                            ) {
                                Text(
                                    text = name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Lipi.Ink,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = container) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = content
        )
    }
}
