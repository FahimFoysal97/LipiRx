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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.ui.theme.Lipi

val CopyVectorIcon = ImageVector.Builder(
    name = "CopyOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(16f, 1f)
        lineTo(4f, 1f)
        arcTo(2f, 2f, 0f, false, false, 2f, 3f)
        lineTo(2f, 15f)
        moveTo(8f, 5f)
        lineTo(20f, 5f)
        arcTo(2f, 2f, 0f, false, true, 22f, 7f)
        lineTo(22f, 21f)
        arcTo(2f, 2f, 0f, false, true, 20f, 23f)
        lineTo(8f, 23f)
        arcTo(2f, 2f, 0f, false, true, 6f, 21f)
        lineTo(6f, 7f)
        arcTo(2f, 2f, 0f, false, true, 8f, 5f)
        close()
    }
}.build()

@Composable
fun MedicineListView(
    imageByteArray: ByteArray?,
    medicines: List<MedicineItem>,
    onZoom: () -> Unit,
    onCopySummary: () -> Unit,
    onRescan: () -> Unit,
) {
    val reviewCount = remember(medicines) { medicines.count { it.needsReview } }
    val headerText = remember(medicines, reviewCount) {
        if (reviewCount > 0) {
            "${medicines.size} medicines · $reviewCount need review"
        } else {
            "${medicines.size} medicines"
        }
    }

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
            AppHeader { InfoChip(headerText) }

            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (imageByteArray != null) {
                    item { PrescriptionThumbnail(imageByteArray, onZoom) }
                }
                itemsIndexed(medicines) { index, item -> MedicineItemCard(index + 1, item) }

                item {
                    Text(
                        text = "🤖 AI-read from handwriting. Verify with a pharmacist before use.",
                        fontSize = 12.sp,
                        color = Lipi.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
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
                    icon = CopyVectorIcon,
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
    var isAlternativesExpanded by remember { mutableStateOf(false) }

    val manufacturer = remember(item.prescribedBrandFull) { parseManufacturer(item.prescribedBrandFull) }
    val isPediatric = item.patientAgeGroup.equals("infant", ignoreCase = true) || item.patientAgeGroup.equals("child", ignoreCase = true)
    val scheduleSlots = remember(item.schedule) { parseScheduleSlots(item.schedule) }
    val cleanInstructions = remember(item.instructions) { fixInstructionsText(item.instructions) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (item.needsReview) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Lipi.Error, RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Single Merged Review / Warning Banner
                if (item.needsReview || item.strengthSource.equals("assumed", ignoreCase = true) || item.possibleMisreading.isNotBlank()) {
                    val warningText = when {
                        item.possibleMisreading.isNotBlank() -> "Needs review: possible misreading (${item.possibleMisreading})"
                        item.strengthSource.equals("assumed", ignoreCase = true) -> "Needs review: strength assumed ${item.assumedStrength.ifBlank { "1%" }}, confirm before dispensing"
                        else -> "Needs review: verify handwritten script"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPediatric) Lipi.Error.copy(alpha = 0.12f) else Lipi.AmberContainer,
                        border = BorderStroke(1.dp, if (isPediatric) Lipi.Error.copy(alpha = 0.4f) else Lipi.OnAmber.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isPediatric) Lipi.Error else Lipi.OnAmber
                            )
                            Text(
                                text = warningText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPediatric) Lipi.Error else Lipi.OnAmber
                            )
                        }
                    }
                }

                // Three-Level Clean Title Layout
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Lipi.Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$index", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.medicineName} ${item.formattedStrength}".trim(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Ink
                        )
                        if (manufacturer.isNotBlank()) {
                            Text(
                                text = manufacturer,
                                fontSize = 13.sp,
                                color = Lipi.Muted
                            )
                        }
                        if (item.genericName.isNotBlank()) {
                            Text(
                                text = "Generic: ${item.genericName}",
                                fontSize = 13.sp,
                                color = Lipi.Muted
                            )
                        }
                    }
                }

                // Hero Schedule Slots (Morning, Noon, Night)
                if (scheduleSlots != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ScheduleSlotBox("Morning", scheduleSlots.morning, Modifier.weight(1f))
                            ScheduleSlotBox("Noon", scheduleSlots.noon, Modifier.weight(1f))
                            ScheduleSlotBox("Night", scheduleSlots.night, Modifier.weight(1f))
                        }
                        if (scheduleSlots.mealTiming.isNotBlank()) {
                            Text(
                                text = "🕒 ${scheduleSlots.mealTiming}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.Primary
                            )
                        }
                    }
                } else if (item.schedule.isNotBlank()) {
                    Badge("Schedule: ${item.schedule}", Lipi.AmberContainer, Lipi.OnAmber)
                }

                // Secondary Badges (Quantity, Form, Instructions)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge("Qty: ${formatGrammaticalQuantity(item.units, item.unitType)}", Lipi.PrimaryContainer, Lipi.OnPrimaryContainer)

                    if (item.dosageForm.isNotBlank()) {
                        Badge("Form: ${item.dosageForm}", Lipi.Background, Lipi.Ink)
                    }

                    if (item.duration.isNotBlank()) {
                        Badge("Duration: ${item.duration}", Lipi.Background, Lipi.Ink)
                    }

                    if (cleanInstructions.isNotBlank()) {
                        Badge("Instructions: $cleanInstructions", Lipi.PrimaryContainer, Lipi.Primary)
                    }

                    if (item.totalVolumeNeededMl != null && item.totalVolumeNeededMl > 0) {
                        Badge("Total Vol: ${item.totalVolumeNeededMl.toInt()} ml", Lipi.AmberContainer, Lipi.OnAmber)
                    }
                }

                // Collapsible Alternatives Section
                if (item.similarBrands.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { isAlternativesExpanded = !isAlternativesExpanded },
                        shape = RoundedCornerShape(14.dp),
                        color = Lipi.Background,
                        border = BorderStroke(1.dp, Lipi.Outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Lipi.Muted)
                                    Text(
                                        text = "Alternatives (${item.similarBrands.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Lipi.Muted
                                    )
                                }

                                Text(
                                    text = if (isAlternativesExpanded) "Hide ▲" else "Show ${item.similarBrands.size} brands ›",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Lipi.Primary
                                )
                            }

                            if (isAlternativesExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item.similarBrands.forEach { altName ->
                                        val altMfr = parseManufacturer(altName)
                                        val cleanAltBrand = altName.substringBefore("(").trim()
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Lipi.Outline)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(cleanAltBrand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Lipi.Ink)
                                                if (altMfr.isNotBlank()) {
                                                    Text(altMfr, fontSize = 11.sp, color = Lipi.Muted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSlotBox(label: String, count: String, modifier: Modifier = Modifier) {
    val isActive = count.toIntOrNull()?.let { it > 0 } ?: (count != "0" && count.isNotBlank())
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Lipi.PrimaryContainer else Lipi.Background,
        border = BorderStroke(1.dp, if (isActive) Lipi.Primary else Lipi.Outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = if (isActive) Lipi.Primary else Lipi.Muted, fontWeight = FontWeight.Medium)
            Text(count, fontSize = 15.sp, color = if (isActive) Lipi.Primary else Lipi.Ink, fontWeight = FontWeight.Bold)
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

private fun parseManufacturer(prescribedBrandFull: String): String {
    if (prescribedBrandFull.contains("(") && prescribedBrandFull.contains(")")) {
        return prescribedBrandFull.substringAfter("(").substringBefore(")").trim()
    }
    return ""
}

private fun formatGrammaticalQuantity(units: Int, unitType: String): String {
    val cleanType = unitType.trim().lowercase()
    val singularType = if (units == 1 && cleanType.endsWith("s") && cleanType != "pcs" && cleanType != "caps") {
        cleanType.dropLast(1)
    } else {
        cleanType
    }
    return "$units $singularType"
}

private fun parseScheduleSlots(scheduleText: String): ScheduleSlots? {
    val clean = scheduleText.trim()
    val timing = when {
        clean.contains("(ac)", ignoreCase = true) || clean.contains("before meal", ignoreCase = true) -> "Before meals"
        clean.contains("(pc)", ignoreCase = true) || clean.contains("after meal", ignoreCase = true) -> "After meals"
        else -> ""
    }
    val numericSchedule = clean.removeSuffix("(ac)").removeSuffix("(pc)").trim()
    val parts = numericSchedule.split("+").map { it.trim() }
    if (parts.size == 3) {
        return ScheduleSlots(
            morning = parts[0],
            noon = parts[1],
            night = parts[2],
            mealTiming = timing
        )
    }
    return null
}

private fun fixInstructionsText(text: String): String {
    var clean = text.trim()
    if (clean == "সাথ") clean = "খাবারের সাথে"
    return clean
}

private data class ScheduleSlots(
    val morning: String = "0",
    val noon: String = "0",
    val night: String = "0",
    val mealTiming: String = ""
)
