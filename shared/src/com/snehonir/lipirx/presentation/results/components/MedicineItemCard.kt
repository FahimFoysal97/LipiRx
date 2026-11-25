package com.snehonir.lipirx.presentation.results.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.presentation.results.EditMedicineDialog
import com.snehonir.lipirx.presentation.results.model.Alternative
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_dose_slot
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.label_after_meal
import com.snehonir.lipirx.resources.label_before_meal
import com.snehonir.lipirx.resources.label_dosage_schedule
import com.snehonir.lipirx.resources.label_needs_review
import com.snehonir.lipirx.resources.label_needs_review_prefix
import com.snehonir.lipirx.resources.label_quantity
import com.snehonir.lipirx.resources.label_similar_brands_count
import com.snehonir.lipirx.resources.label_unit_type
import com.snehonir.lipirx.resources.slot_morning
import com.snehonir.lipirx.resources.slot_night
import com.snehonir.lipirx.resources.slot_noon
import com.snehonir.lipirx.resources.title_edit_medicine
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
fun MedicineItemCard(
    index: Int,
    item: MedicineItem,
    isEditable: Boolean = true,
    onUpdateItem: (MedicineItem) -> Unit = {}
) {
    var expanded by rememberSaveable(index) { mutableStateOf(false) }
    var showEditDialog by rememberSaveable(index) { mutableStateOf(false) }

    if (showEditDialog) {
        EditMedicineDialog(
            item = item,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                showEditDialog = false
                onUpdateItem(updated)
            }
        )
    }

    val company = remember(item.prescribedBrandFull) { parseManufacturer(item.prescribedBrandFull) }
    val doseSlots = remember(item.schedule) { parseDoseSlots(item.schedule) }
    val beforeMealText = stringResource(Res.string.label_before_meal)
    val afterMealText = stringResource(Res.string.label_after_meal)
    val mealLabel = remember(item.schedule, beforeMealText, afterMealText) {
        parseMealLabel(item.schedule, beforeMealText, afterMealText)
    }
    val reviewNote = remember(item) { buildReviewNote(item) }
    val alternatives =
        remember(item.similarBrands) { item.similarBrands.map { parseAlternative(it) } }
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
        color = Lipi.Surface,
        border = BorderStroke(
            width = if (item.needsReview) 1.5.dp else 1.dp,
            color = if (item.needsReview) Lipi.Error.copy(alpha = 0.55f) else Lipi.CardBorder
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    Text(
                        text = "$index",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = cardTitle,
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink
                    )
                    if (item.genericName.isNotBlank()) {
                        Text(
                            text = item.genericName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Lipi.Ink.copy(alpha = 0.75f)
                        )
                    }
                    if (company.isNotBlank()) {
                        Text(company, fontSize = 12.sp, color = Lipi.Muted)
                    }
                }
                if (isEditable) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.title_edit_medicine),
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Primary
                        )
                    }
                }
            }

            if (item.needsReview) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Lipi.Error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Lipi.Error
                    )
                    Text(
                        text = if (!reviewNote.isNullOrBlank()) {
                            stringResource(Res.string.label_needs_review_prefix, reviewNote)
                        } else {
                            stringResource(Res.string.label_needs_review)
                        },
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Error,
                    )
                }
            }

            if (doseSlots != null && doseSlots.size == 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        stringResource(Res.string.slot_morning),
                        stringResource(Res.string.slot_noon),
                        stringResource(Res.string.slot_night)
                    ).zip(doseSlots).forEach { (label, value) ->
                        DoseSlot(label, value, Modifier.weight(1f))
                    }
                }
            } else if (item.schedule.isNotBlank()) {
                Badge(
                    "${stringResource(Res.string.label_dosage_schedule)}: ${item.schedule}",
                    Lipi.AmberContainer,
                    Lipi.OnAmber
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mealLabel?.let { Badge(it, Lipi.AmberContainer, Lipi.OnAmber) }

                Badge(
                    "${stringResource(Res.string.label_quantity)}: ${
                        quantityText(
                            item.units,
                            item.unitType
                        )
                    }", Lipi.PrimaryContainer, Lipi.OnPrimaryContainer
                )

                if (item.dosageForm.isNotBlank() && !item.dosageForm.equals(
                        item.unitType,
                        ignoreCase = true
                    )
                ) {
                    Badge(
                        "${stringResource(Res.string.label_unit_type)}: ${item.dosageForm}",
                        Lipi.ImageWell,
                        Lipi.Ink
                    )
                }

                if (cleanInstructions.isNotBlank() && cleanInstructions.length <= 20) {
                    Badge(cleanInstructions, Lipi.ImageWell, Lipi.Ink)
                }
            }

            if (cleanInstructions.length > 20) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Lipi.ImageWell
                ) {
                    Text(
                        text = cleanInstructions,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.Ink
                    )
                }
            }

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
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Lipi.Muted
                        )
                        Text(
                            text = stringResource(
                                Res.string.label_similar_brands_count,
                                alternatives.size
                            ),
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Muted,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = stringResource(if (expanded) Res.string.cd_collapse else Res.string.cd_expand),
                            modifier = Modifier
                                .size(24.dp)
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

@Composable
private fun DoseSlot(label: String, value: String, modifier: Modifier = Modifier) {
    val active = value.trim() != "0" && value.isNotBlank()
    val textColor = if (active) MaterialTheme.colorScheme.onPrimary else Lipi.Muted
    val spokenDescription = stringResource(Res.string.cd_dose_slot, label, value)
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = spokenDescription }
            .background(if (active) Lipi.Primary else Lipi.ImageWell, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.9f)
        )
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
    }
}

@Composable
private fun AlternativeChip(alt: Alternative) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                alt.brand,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Ink
            )
            if (alt.company.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(alt.company, fontSize = 11.sp, color = Lipi.Muted)
            }
        }
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(10.dp), color = container) {
        Text(
            text,
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
    val singularType =
        if (units == 1 && cleanType.endsWith("s") && cleanType != "pcs" && cleanType != "caps") {
            cleanType.dropLast(1)
        } else {
            cleanType
        }
    return if (units == 1) "1 $singularType" else "$units $cleanType"
}

private fun parseMealLabel(schedule: String, beforeMeal: String, afterMeal: String): String? {
    val clean = schedule.lowercase()
    return when {
        clean.contains("ac") || clean.contains("before meal") -> beforeMeal
        clean.contains("pc") || clean.contains("after meal") -> afterMeal
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

    val match = Regex("""^\s*([\d½¼.]+)\s*\+\s*([\d½¼.]+)\s*\+\s*([\d½¼.]+)""").find(clean)
    if (match != null) {
        return match.groupValues.drop(1)
    }

    if (clean.contains("2 times") || clean.contains("twice") || clean.contains("2 বেলা") || clean.contains(
            "2-times"
        )
    ) {
        return listOf("1", "0", "1")
    }
    if (clean.contains("3 times") || clean.contains("thrice") || clean.contains("3 বেলা") || clean.contains(
            "3-times"
        )
    ) {
        return listOf("1", "1", "1")
    }
    if (clean.contains("once") || clean.contains("1 time") || clean.contains("1 বেলা") || clean.contains(
            "1-time"
        )
    ) {
        return listOf("0", "0", "1")
    }

    return null
}

private fun buildReviewNote(item: MedicineItem): String? {
    return when {
        item.possibleMisreading.isNotBlank() -> "possible misreading (${item.possibleMisreading})"
        item.strengthSource.equals(
            "assumed",
            ignoreCase = true
        ) -> "strength assumed ${item.assumedStrength.ifBlank { "1%" }}, confirm before dispensing"

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
