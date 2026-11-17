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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.presentation.scan.model.Alternative
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_copy_summary
import com.snehonir.lipirx.resources.btn_done
import com.snehonir.lipirx.resources.btn_sign_off
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.disclaimer_text
import com.snehonir.lipirx.resources.label_needs_pharmacist_review
import com.snehonir.lipirx.resources.label_pharmacist_verified
import com.snehonir.lipirx.resources.no_medicines_subtitle
import com.snehonir.lipirx.resources.no_medicines_title
import com.snehonir.lipirx.resources.prescription_scanned
import com.snehonir.lipirx.resources.results_title
import com.snehonir.lipirx.resources.saved_prescription_record
import com.snehonir.lipirx.resources.tap_to_zoom
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.CopyVectorIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun MedicineListView(
    imageByteArray: ByteArray?,
    medicines: List<MedicineItem>,
    isSuspendedForApproval: Boolean = false,
    approvalReason: String? = null,
    selectedHistoryItem: PrescriptionHistoryEntity? = null,
    onZoom: () -> Unit,
    onCopySummary: () -> Unit,
    onRescan: () -> Unit,
    onApproveAndConfirm: () -> Unit = {},
    onHistoryClick: (() -> Unit)? = null,
    onBackFromHistory: (() -> Unit)? = null
) {
    val reviewCount = remember(medicines) { medicines.count { it.needsReview } }

    // Filter: "All" vs "Needs review". Original numbering (#1, #2…) is kept so it still
    // matches the order on the paper prescription.
    var showOnlyReview by rememberSaveable { mutableStateOf(false) }
    val filterActive = showOnlyReview && reviewCount > 0
    val visibleItems = remember(medicines, filterActive) {
        medicines.withIndex().filter { !filterActive || it.value.needsReview }
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
            if (selectedHistoryItem != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = { onBackFromHistory?.invoke() }),
                        shape = CircleShape,
                        color = Lipi.Surface,
                        border = BorderStroke(1.dp, Lipi.CardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to History",
                                modifier = Modifier.size(20.dp),
                                tint = Lipi.Ink
                            )
                        }
                    }

                    Column {
                        Text(
                            text = selectedHistoryItem.formattedDateTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Ink
                        )
                        Text(
                            text = stringResource(Res.string.saved_prescription_record),
                            fontSize = 12.sp,
                            color = Lipi.Muted
                        )
                    }
                }
            } else {
                AppHeader(
                    onBack = onRescan,
                    title = stringResource(Res.string.results_title)
                ) {
                    if (onHistoryClick != null) {
                        HistoryChip(onClick = onHistoryClick)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 130.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // 1. Safety first: the pharmacist banner goes above everything else.
                if (isSuspendedForApproval || reviewCount > 0) {
                    item {
                        PharmacistApprovalBanner(
                            reason = approvalReason ?: "$reviewCount item(s) flagged for pharmacist verification",
                            isApproved = !isSuspendedForApproval,
                            onApprove = onApproveAndConfirm
                        )
                    }
                }

                // 2. Summary (count + prescription thumbnail in one card)
                item {
                    SummaryCard(
                        medicineCount = medicines.size,
                        imageByteArray = imageByteArray,
                        onZoom = onZoom
                    )
                }

                // 3. Filter pills (only when something needs review)
                if (reviewCount > 0) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterPill(
                                text = "All (${medicines.size})",
                                selected = !filterActive,
                                accent = Lipi.Primary,
                                onClick = { showOnlyReview = false }
                            )
                            FilterPill(
                                text = "Needs review ($reviewCount)",
                                selected = filterActive,
                                accent = Lipi.Error,
                                icon = Icons.Default.Warning,
                                onClick = { showOnlyReview = true }
                            )
                        }
                    }
                }

                if (medicines.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Lipi.Surface,
                            border = BorderStroke(1.dp, Lipi.CardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.no_medicines_title),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Lipi.Ink
                                )
                                Text(
                                    text = stringResource(Res.string.no_medicines_subtitle),
                                    fontSize = 12.5.sp,
                                    color = Lipi.Muted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(visibleItems) { (originalIndex, item) ->
                    MedicineItemCard(originalIndex + 1, item)
                }

                item {
                    Text(
                        text = stringResource(Res.string.disclaimer_text),
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
            color = Lipi.Surface,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, Lipi.CardBorder),
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecondaryButton(
                    text = stringResource(Res.string.btn_copy_summary),
                    icon = CopyVectorIcon,
                    onClick = onCopySummary,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                PrimaryButton(
                    text = stringResource(Res.string.btn_done),
                    icon = Icons.Default.Check,
                    onClick = onRescan,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top-of-list pieces
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PharmacistApprovalBanner(
    reason: String,
    isApproved: Boolean,
    onApprove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isApproved) Lipi.PrimaryContainer else Lipi.AmberContainer,
        border = BorderStroke(1.dp, if (isApproved) Lipi.Primary else Lipi.OnAmber.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isApproved) Lipi.Primary else Lipi.OnAmber,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isApproved) stringResource(Res.string.label_pharmacist_verified)
                    else stringResource(Res.string.label_needs_pharmacist_review),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isApproved) Lipi.OnPrimaryContainer else Lipi.OnAmber
                )
            }

            Text(
                text = reason,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Lipi.Ink
            )

            if (!isApproved) {
                Surface(
                    onClick = onApprove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    color = Lipi.Primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.btn_sign_off),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    medicineCount: Int,
    imageByteArray: ByteArray?,
    onZoom: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = imageByteArray != null, role = Role.Button, onClick = onZoom),
        shape = shape,
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (imageByteArray != null) {
                AsyncImage(
                    model = imageByteArray,
                    contentDescription = stringResource(Res.string.content_desc_prescription),
                    modifier = Modifier
                        .size(48.dp, 60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Lipi.ImageWell),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = if (medicineCount == 1) "1 medicine found" else "$medicineCount medicines found",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )
                if (imageByteArray != null) {
                    Text(
                        text = stringResource(Res.string.prescription_scanned),
                        fontSize = 12.sp,
                        color = Lipi.Muted
                    )
                }
            }
            if (imageByteArray != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Lipi.Primary
                    )
                    Text(
                        text = stringResource(Res.string.tap_to_zoom),
                        fontSize = 10.sp,
                        color = Lipi.Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 40.dp),
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.15f) else Lipi.Surface,
        border = BorderStroke(1.dp, if (selected) accent else Lipi.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = accent)
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) accent else Lipi.Muted
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Medicine card
// ─────────────────────────────────────────────────────────────────────────────

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
            // 1. Title block
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            }

            // 2. Review banner
            if (item.needsReview) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Lipi.Error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Error,
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

            // 4. Secondary info
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Meal timing is safety-relevant, so it keeps the accent colour.
                mealLabel?.let { Badge(it, Lipi.AmberContainer, Lipi.OnAmber) }

                Badge("Qty: ${quantityText(item.units, item.unitType)}", Lipi.PrimaryContainer, Lipi.OnPrimaryContainer)

                if (item.dosageForm.isNotBlank() && !item.dosageForm.equals(item.unitType, ignoreCase = true)) {
                    Badge("Form: ${item.dosageForm}", Lipi.ImageWell, Lipi.Ink)
                }

                if (cleanInstructions.isNotBlank() && cleanInstructions.length <= 20) {
                    Badge(cleanInstructions, Lipi.ImageWell, Lipi.Ink)
                }
            }

            // Long instruction text
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
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(role = Role.Button) { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    Column(
        modifier = modifier
            .background(if (active) Lipi.Primary else Lipi.ImageWell, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.9f))
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

// ─────────────────────────────────────────────────────────────────────────────
// Parsing helpers (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────
// Compose Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview
@Composable
fun MedicineListViewPreview() {
    val sampleMedicines = listOf(
        MedicineItem(
            medicineName = "Napa Extra",
            strengthValue = "500mg+65mg",
            prescribedBrandFull = "Napa Extra 500mg+65mg (Beximco Pharmaceuticals Ltd.)",
            genericName = "Paracetamol + Caffeine",
            dosageForm = "Tablet",
            schedule = "1 + 0 + 1",
            units = 10,
            unitType = "tablets",
            instructions = "খাবারের পর খাবেন",
            similarBrands = listOf("Ace Plus (Square Pharmaceuticals)", "Fast Plus (Acme Laboratories)"),
            needsReview = false
        ),
        MedicineItem(
            medicineName = "Seclo 20",
            strengthValue = "20mg",
            prescribedBrandFull = "Seclo 20mg (Square Pharmaceuticals Ltd.)",
            genericName = "Omeprazole",
            dosageForm = "Capsule",
            schedule = "1 + 0 + 1",
            units = 14,
            unitType = "capsules",
            instructions = "খাবারের ৩০ মিনিট আগে",
            similarBrands = listOf("Losectil 20 (Eskayef Pharmaceuticals)", "Esonix 20 (Incepta Pharmaceuticals)"),
            needsReview = true,
            possibleMisreading = "Unclear strength notation"
        )
    )

    MedicineListView(
        imageByteArray = null,
        medicines = sampleMedicines,
        onZoom = {},
        onCopySummary = {},
        onRescan = {},
        onApproveAndConfirm = {}
    )
}