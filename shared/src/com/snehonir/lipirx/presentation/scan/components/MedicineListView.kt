package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.ui.theme.Lipi

data class Alternative(val brand: String, val company: String)

@Composable
fun MedicineListView(
    imageByteArray: ByteArray?,
    medicines: List<MedicineItem>,
    onZoom: () -> Unit,
    onCopySummary: () -> Unit,
    onRescan: () -> Unit,
) {
    val reviewCount = remember(medicines) { medicines.count { it.needsReview } }

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
            AppHeader()

            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip("${medicines.size} medicines")
                        if (reviewCount > 0) {
                            ReviewChip("$reviewCount need review")
                        }
                    }
                }

                if (imageByteArray != null) {
                    item { PrescriptionThumbnail(imageByteArray, onZoom) }
                }

                itemsIndexed(medicines) { index, item -> MedicineItemCard(index + 1, item) }

                item {
                    Text(
                        text = "AI-read from handwriting. Verify with a pharmacist before use.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        fontSize = 11.sp,
                        color = Lipi.Muted,
                        textAlign = TextAlign.Center,
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
private fun ReviewChip(text: String) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFFBE9E7)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp), tint = Lipi.Error)
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C1D18))
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
                    .size(40.dp, 50.dp)
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

@Composable
fun MedicineItemCard(index: Int, item: MedicineItem) {
    var expanded by rememberSaveable(item.medicineName, index) { mutableStateOf(false) }

    val company = remember(item.prescribedBrandFull) { parseManufacturer(item.prescribedBrandFull) }
    val doseSlots = remember(item.schedule) { parseDoseSlots(item.schedule) }
    val mealLabel = remember(item.schedule) { parseMealLabel(item.schedule) }
    val reviewNote = remember(item) { buildReviewNote(item) }
    val alternatives = remember(item.similarBrands) { item.similarBrands.map { parseAlternative(it) } }
    val cleanInstructions = remember(item.instructions) { fixInstructionsText(item.instructions) }
    val cardTitle = remember(item.medicineName, item.formattedStrength) {
        formatCleanTitle(item.medicineName, item.formattedStrength)
    }

    val cardShape = RoundedCornerShape(22.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.needsReview) Lipi.Error.copy(alpha = 0.4f) else Lipi.CardBorder
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 1. Title block: brand + strength / company / generic
                Row(
                    verticalAlignment = Alignment.Top,
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
                        Text(
                            text = cardTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Ink
                        )
                        if (company.isNotBlank()) {
                            Text(company, fontSize = 12.sp, color = Lipi.Muted)
                        }
                        if (item.genericName.isNotBlank()) {
                            Text("Generic: ${item.genericName}", fontSize = 13.sp, color = Lipi.Muted)
                        }
                    }
                }

                // 2. One merged review banner
                if (item.needsReview) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFBE9E7), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp), tint = Lipi.Error)
                        Text(
                            text = buildString {
                                append("Needs review")
                                if (!reviewNote.isNullOrBlank()) append(": $reviewNote")
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8C1D18),
                        )
                    }
                }

                // 3. Dose as the hero
                if (doseSlots != null && doseSlots.size == 3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Morning", "Noon", "Night").zip(doseSlots).forEach { (label, value) ->
                            DoseSlot(label, value, Modifier.weight(1f))
                        }
                    }
                } else if (item.schedule.isNotBlank()) {
                    Badge("Schedule: ${item.schedule}", Lipi.AmberContainer, Lipi.OnAmber)
                }

                // 4. Secondary info badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge("Qty: ${quantityText(item.units, item.unitType)}", Lipi.PrimaryContainer, Lipi.OnPrimaryContainer)

                    mealLabel?.let { Badge(it, Lipi.AmberContainer, Lipi.OnAmber) }

                    if (item.dosageForm.isNotBlank() && !item.dosageForm.equals(item.unitType, ignoreCase = true)) {
                        Badge("Form: ${item.dosageForm}", Color(0xFFF1F4F3), Lipi.Ink)
                    }

                    if (cleanInstructions.isNotBlank() && cleanInstructions.length <= 20) {
                        Badge(cleanInstructions, Color(0xFFF1F4F3), Lipi.Ink)
                    }
                }

                // Detailed long instruction card if instruction is long
                if (cleanInstructions.length > 20) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F4F3)
                    ) {
                        Text(
                            text = cleanInstructions,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Ink
                        )
                    }
                }

                // 5. Collapsible alternatives
                if (alternatives.isNotEmpty()) {
                    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Lipi.Background, RoundedCornerShape(14.dp)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { expanded = !expanded }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Lipi.Muted)
                            Text(
                                text = "${alternatives.size} similar brands",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Muted,
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotation),
                                tint = Lipi.Muted
                            )
                        }
                        if (expanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                alternatives.forEach { AlternativeChip(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoseSlot(label: String, value: String, modifier: Modifier = Modifier) {
    val active = value.trim() != "0" && value.isNotBlank()
    Column(
        modifier = modifier
            .background(if (active) Lipi.Primary else Color(0xFFEEF2F1), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val c = if (active) Color.White else Color(0xFF8A9A9F)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c.copy(alpha = 0.9f))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = c)
    }
}

@Composable
private fun AlternativeChip(alt: Alternative) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(alt.brand, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Lipi.Ink)
            if (alt.company.isNotBlank()) {
                Text(alt.company, fontSize = 11.sp, color = Lipi.Muted)
            }
        }
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = container) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = content)
    }
}

private fun parseManufacturer(prescribedBrandFull: String): String {
    if (prescribedBrandFull.contains("(") && prescribedBrandFull.contains(")")) {
        return prescribedBrandFull.substringAfter("(").substringBefore(")").trim()
    }
    return ""
}

private fun parseAlternative(altString: String): Alternative {
    val clean = altString.trim()
    val brand = clean.substringBefore("(").substringBefore("-").trim()
    val company = if (clean.contains("(") && clean.contains(")")) {
        clean.substringAfter("(").substringBefore(")").trim()
    } else {
        ""
    }
    return Alternative(brand = brand, company = company)
}

private fun quantityText(units: Int, unitType: String): String {
    val cleanType = unitType.trim().lowercase()
    val singularType = if (units == 1 && cleanType.endsWith("s") && cleanType != "pcs" && cleanType != "caps") {
        cleanType.dropLast(1)
    } else {
        cleanType
    }
    return if (units == 1) "1 $singularType" else "$units $cleanType"
}

private fun parseMealLabel(schedule: String): String? {
    val clean = schedule.lowercase()
    return when {
        clean.contains("ac") || clean.contains("before meal") -> "Before meal"
        clean.contains("pc") || clean.contains("after meal") -> "After meal"
        else -> null
    }
}

private fun convertBengaliDigits(text: String): String {
    return text
        .replace('০', '0')
        .replace('১', '1')
        .replace('২', '2')
        .replace('৩', '3')
        .replace('৪', '4')
        .replace('৫', '5')
        .replace('৬', '6')
        .replace('৭', '7')
        .replace('৮', '8')
        .replace('৯', '9')
}

private fun parseDoseSlots(scheduleText: String): List<String>? {
    val clean = convertBengaliDigits(scheduleText.trim().lowercase())

    // Check "1 + 0 + 1" pattern
    val match = Regex("""^\s*([\d½¼.]+)\s*\+\s*([\d½¼.]+)\s*\+\s*([\d½¼.]+)""").find(clean)
    if (match != null) {
        return match.groupValues.drop(1)
    }

    // Check "2 times" or "2 বেলা" -> 1 + 0 + 1
    if (clean.contains("2 times") || clean.contains("twice") || clean.contains("2 বেলা") || clean.contains("2-times")) {
        return listOf("1", "0", "1")
    }
    // Check "3 times" or "3 বেলা" -> 1 + 1 + 1
    if (clean.contains("3 times") || clean.contains("thrice") || clean.contains("3 বেলা") || clean.contains("3-times")) {
        return listOf("1", "1", "1")
    }
    // Check "once" or "1 বেলা" -> 0 + 0 + 1
    if (clean.contains("once") || clean.contains("1 time") || clean.contains("1 বেলা") || clean.contains("1-time")) {
        return listOf("0", "0", "1")
    }

    return null
}

private fun buildReviewNote(item: MedicineItem): String? {
    return when {
        item.possibleMisreading.isNotBlank() -> "possible misreading (${item.possibleMisreading})"
        item.strengthSource.equals("assumed", ignoreCase = true) -> "strength assumed ${item.assumedStrength.ifBlank { "1%" }}, confirm before dispensing"
        else -> null
    }
}

private fun formatCleanTitle(brandName: String, strength: String): String {
    val cleanBrand = brandName.trim()
    val cleanStrength = strength.trim()
    if (cleanStrength.isBlank()) return cleanBrand
    val strengthNumber = cleanStrength.substringBefore(" ").trim()
    return if (strengthNumber.isNotBlank() && cleanBrand.endsWith(strengthNumber)) {
        val unit = cleanStrength.substringAfter(" ", "").trim()
        "$cleanBrand $unit".trim()
    } else {
        "$cleanBrand $cleanStrength".trim()
    }
}

private fun fixInstructionsText(text: String): String {
    var clean = text.trim().removeSuffix("?").trim()
    if (clean == "সাথ") clean = "খাবারের সাথে"
    return clean
}
