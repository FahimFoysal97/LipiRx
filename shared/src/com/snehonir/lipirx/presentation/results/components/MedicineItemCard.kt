package com.snehonir.lipirx.presentation.results.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as rotateDraw
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.presentation.results.EditMedicineDialog
import com.snehonir.lipirx.presentation.results.model.Alternative
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_edit
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_dose_slot
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.field_dose_pending
import com.snehonir.lipirx.resources.field_generic_pending
import com.snehonir.lipirx.resources.field_waiting
import com.snehonir.lipirx.resources.label_after_meal
import com.snehonir.lipirx.resources.label_before_meal
import com.snehonir.lipirx.resources.label_checked
import com.snehonir.lipirx.resources.label_dosage_schedule
import com.snehonir.lipirx.resources.label_needs_review
import com.snehonir.lipirx.resources.label_needs_review_prefix
import com.snehonir.lipirx.resources.label_rereading
import com.snehonir.lipirx.resources.label_review
import com.snehonir.lipirx.resources.label_similar_brands_count
import com.snehonir.lipirx.resources.slot_morning
import com.snehonir.lipirx.resources.slot_night
import com.snehonir.lipirx.resources.slot_noon
import com.snehonir.lipirx.resources.title_edit_medicine
import com.snehonir.lipirx.ui.rememberReduceMotion
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.AgentIcon
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Where one group of fields is in the pipeline. */
enum class FieldPhase { PENDING, RUNNING, READY }

/**
 * Which agent has filled which part of the card.
 *  - dose    <- Entity parser (strength, schedule, quantity, form, instructions, maker)
 *  - generic <- Generic substitute (generic name, similar brands)
 *  - safety  <- Safety auditor (checked / needs review)
 * The Vision transcriptor has always finished by the time a card exists (it supplies the name).
 *
 * The default is "everything ready", so history and read-only call sites need no changes.
 */
@Immutable
data class MedicineCardProgress(
    val dose: FieldPhase = FieldPhase.READY,
    val generic: FieldPhase = FieldPhase.READY,
    val safety: FieldPhase = FieldPhase.READY,
    val rereadingDose: Boolean = false,
    /** False when the safety audit failed, so "Checked" is never claimed. */
    val safetyChecked: Boolean = true,
) {
    val isComplete: Boolean
        get() = dose == FieldPhase.READY && generic == FieldPhase.READY && safety == FieldPhase.READY &&
                !rereadingDose

    companion object {
        val Complete = MedicineCardProgress()
    }
}

/**
 * @param compact When true and the card is finished, it collapses to one line (name, dose pattern,
 * status). Tap to open it; tap the header to collapse again. A flagged card keeps its red border and
 * "Review" badge and shows the review note instead of the dose, so a problem can't hide.
 */
@Composable
fun MedicineItemCard(
    index: Int,
    item: MedicineItem,
    modifier: Modifier = Modifier,
    progress: MedicineCardProgress = MedicineCardProgress.Complete,
    isEditable: Boolean = true,
    compact: Boolean = false,
    onUpdateItem: (MedicineItem) -> Unit = {},
    reduceMotion: Boolean = rememberReduceMotion(),
) {
    var brandsOpen by rememberSaveable(index) { mutableStateOf(false) }
    var detailOpen by rememberSaveable(index) { mutableStateOf(false) }
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
    val alternatives = remember(item.similarBrands) { item.similarBrands.map { parseAlternative(it) } }
    val cleanInstructions = remember(item.instructions) { fixInstructionsText(item.instructions) }
    val title = remember(item.medicineName, item.formattedStrength) {
        splitTitle(item.medicineName, item.formattedStrength)
    }
    val strength = item.formattedStrength.trim()

    val doseReady = progress.dose == FieldPhase.READY
    val genericReady = progress.generic == FieldPhase.READY
    val safetyReady = progress.safety == FieldPhase.READY
    // A flag can only be trusted once the safety agent has finished.
    val flagged = safetyReady && item.needsReview
    val showCompact = compact && progress.isComplete && !detailOpen

    val borderColor by animateColorAsState(
        targetValue = if (flagged) Lipi.Error.copy(alpha = 0.55f) else Lipi.CardBorder,
        animationSpec = tween(300),
        label = "cardBorder",
    )
    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        color = Lipi.Surface,
        border = BorderStroke(if (flagged) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showCompact) {
                val summary = remember(doseSlots, item.schedule, mealLabel) {
                    listOfNotNull(
                        doseSlots?.joinToString("+") ?: item.schedule.takeIf { it.isNotBlank() },
                        mealLabel,
                    ).joinToString(" · ")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { detailOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NumberBadge(index)
                    Column(modifier = Modifier.weight(1f)) {
                        TitleLine(title, strength, doseReady, fontSize = 15.sp)
                        if (flagged) {
                            Text(
                                text = if (!reviewNote.isNullOrBlank()) {
                                    stringResource(Res.string.label_needs_review_prefix, reviewNote)
                                } else {
                                    stringResource(Res.string.label_needs_review)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.Error,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else if (summary.isNotBlank()) {
                            Text(summary, fontSize = 12.sp, color = Lipi.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    StatusCorner(progress, flagged, reduceMotion)
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(Res.string.cd_expand),
                        modifier = Modifier.size(24.dp),
                        tint = Lipi.Muted,
                    )
                }
            } else {
                // ---- Header: number, name + strength, generic . maker, status ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (compact && progress.isComplete) {
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button) { detailOpen = false }
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NumberBadge(index)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TitleLine(title, strength, doseReady, fontSize = 17.sp)
                        Crossfade(targetState = genericReady, animationSpec = tween(250), label = "genericName") { ready ->
                            if (ready) {
                                val line = listOf(item.genericName.trim(), company).filter { it.isNotBlank() }.joinToString(" · ")
                                if (line.isNotBlank()) {
                                    Text(line, fontSize = 12.5.sp, color = Lipi.Muted)
                                }
                            } else {
                                SkeletonBar(width = 120.dp, height = 10.dp, alpha = rememberShimmerAlpha(true, reduceMotion))
                            }
                        }
                    }
                    StatusCorner(progress, flagged, reduceMotion)
                }

                if (flagged) {
                    ReviewBanner(
                        text = if (!reviewNote.isNullOrBlank()) {
                            stringResource(Res.string.label_needs_review_prefix, reviewNote)
                        } else {
                            stringResource(Res.string.label_needs_review)
                        }
                    )
                }

                if (progress.rereadingDose && doseReady) {
                    Badge(stringResource(Res.string.label_rereading), Lipi.PrimaryContainer, Lipi.OnPrimaryContainer)
                }

                // ---- Dose: skeleton until the entity parser has filled it ----
                Crossfade(targetState = doseReady, animationSpec = tween(300), label = "doseBlock") { ready ->
                    if (!ready) {
                        DoseSkeleton(progress.dose, reduceMotion)
                    } else {
                        DosePanel(
                            doseSlots = doseSlots,
                            schedule = item.schedule,
                            meta = remember(item, mealLabel) { buildMeta(item, mealLabel) },
                            longNote = cleanInstructions.takeIf { it.isNotBlank() },
                        )
                    }
                }

                // ---- Footer: similar brands (generic agent) + Edit ----
                val canEdit = isEditable && progress.isComplete
                val showBrands = genericReady && alternatives.isNotEmpty()
                if (showBrands || !genericReady || canEdit) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Lipi.CardBorder))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!genericReady) {
                            PendingLine(
                                agent = RxAgentType.GENERIC_SUBSTITUTE,
                                phase = progress.generic,
                                label = stringResource(Res.string.field_generic_pending),
                                reduceMotion = reduceMotion,
                            )
                        } else if (showBrands) {
                            val rotation by animateFloatAsState(if (brandsOpen) 180f else 0f, label = "chevron")
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button) { brandsOpen = !brandsOpen }
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(Res.string.label_similar_brands_count, alternatives.size),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Lipi.Muted,
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(if (brandsOpen) Res.string.cd_collapse else Res.string.cd_expand),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rotate(rotation),
                                    tint = Lipi.Muted,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (canEdit) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(role = Role.Button) { showEditDialog = true }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(Res.string.title_edit_medicine),
                                    modifier = Modifier.size(16.dp),
                                    tint = Lipi.Primary,
                                )
                                Text(
                                    stringResource(Res.string.btn_edit),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Lipi.Primary,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = brandsOpen && showBrands,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            alternatives.forEach { AlternativeChip(it) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Header pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun NumberBadge(index: Int) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .background(Lipi.Primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$index",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Brand name, then the strength as its own pill (a skeleton until the entity parser reads it). */
@Composable
private fun TitleLine(
    title: String,
    strength: String,
    doseReady: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            fontSize = fontSize,
            lineHeight = fontSize * 1.25f,
            fontWeight = FontWeight.ExtraBold,
            color = Lipi.Ink,
        )
        when {
            strength.isNotBlank() -> Surface(shape = RoundedCornerShape(50), color = Lipi.ImageWell) {
                Text(
                    strength,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink,
                )
            }
            !doseReady -> SkeletonBar(width = 44.dp, height = 16.dp, alpha = 0.6f)
        }
    }
}

/**
 * Top-right status. While agents work: the three downstream agents' own icons (waiting, running,
 * done). When finished: one badge, Checked or Needs review.
 */
@Composable
private fun StatusCorner(progress: MedicineCardProgress, flagged: Boolean, reduceMotion: Boolean) {
    Box(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        when {
            !progress.isComplete -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fun status(phase: FieldPhase, rerun: Boolean = false) = when {
                    rerun -> AgentStatus.RUNNING
                    phase == FieldPhase.PENDING -> AgentStatus.WAITING
                    phase == FieldPhase.RUNNING -> AgentStatus.RUNNING
                    else -> AgentStatus.DONE
                }
                AgentIcon(RxAgentType.ENTITY_PARSER, status(progress.dose, progress.rereadingDose), size = 22.dp, showDoneBadge = false, reduceMotion = reduceMotion)
                AgentIcon(RxAgentType.GENERIC_SUBSTITUTE, status(progress.generic), size = 22.dp, showDoneBadge = false, reduceMotion = reduceMotion)
                AgentIcon(RxAgentType.SAFETY_AUDITOR, status(progress.safety), size = 22.dp, showDoneBadge = false, reduceMotion = reduceMotion)
            }

            flagged -> StatusBadge(
                text = stringResource(Res.string.label_review),
                icon = Icons.Default.Warning,
                container = Lipi.Error.copy(alpha = 0.15f),
                content = Lipi.Error,
            )

            progress.safetyChecked -> StatusBadge(
                text = stringResource(Res.string.label_checked),
                icon = Icons.Default.Check,
                container = Lipi.PrimaryContainer,
                content = Lipi.OnPrimaryContainer,
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    content: Color,
) {
    Row(
        modifier = Modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = content)
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Dose panel
// ---------------------------------------------------------------------------------------------

@Composable
private fun DosePanel(
    doseSlots: List<String>?,
    schedule: String,
    meta: List<MetaItem>,
    longNote: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Lipi.Background,
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (doseSlots != null && doseSlots.size == 3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            SlotKind.MORNING to stringResource(Res.string.slot_morning),
                            SlotKind.NOON to stringResource(Res.string.slot_noon),
                            SlotKind.NIGHT to stringResource(Res.string.slot_night),
                        ).zip(doseSlots).forEach { (slot, value) ->
                            DoseSlot(slot.first, slot.second, value, Modifier.weight(1f))
                        }
                    }
                } else if (schedule.isNotBlank()) {
                    Text(
                        text = "${stringResource(Res.string.label_dosage_schedule)}: $schedule",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.OnAmber,
                    )
                }
                if (meta.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        meta.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                MetaGlyph(item.kind, Lipi.Muted, Modifier.size(13.dp))
                                Text(item.text, fontSize = 12.sp, color = Lipi.Muted)
                            }
                        }
                    }
                }
            }
        }
        if (longNote != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 2.dp),
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Lipi.Muted)
                Text(longNote, fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = Lipi.Ink)
            }
        }
    }
}

private enum class SlotKind { MORNING, NOON, NIGHT }

private enum class MetaKind { MEAL, QUANTITY, FORM }

private data class MetaItem(val kind: MetaKind, val text: String)

/** Drawn glyphs for the meta line: plate (meal timing), box (quantity), capsule (form). */
@Composable
private fun MetaGlyph(kind: MetaKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = s * 0.11f
        val c = Offset(size.width / 2f, size.height / 2f)
        when (kind) {
            MetaKind.MEAL -> {
                drawCircle(tint, radius = s * 0.42f, center = c, style = Stroke(sw))
                drawCircle(tint, radius = s * 0.2f, center = c, style = Stroke(sw))
            }

            MetaKind.QUANTITY -> {
                drawRoundRect(
                    tint, topLeft = Offset(s * 0.1f, s * 0.2f), size = Size(s * 0.8f, s * 0.64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f), style = Stroke(sw),
                )
                drawLine(tint, Offset(s * 0.1f, s * 0.42f), Offset(s * 0.9f, s * 0.42f), sw)
            }

            MetaKind.FORM -> rotateDraw(-45f, c) {
                drawRoundRect(
                    tint, topLeft = Offset(s * 0.05f, s * 0.3f), size = Size(s * 0.9f, s * 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.2f), style = Stroke(sw),
                )
                drawLine(tint, Offset(c.x, s * 0.3f), Offset(c.x, s * 0.7f), sw)
            }
        }
    }
}

@Composable
private fun DoseSlot(kind: SlotKind, label: String, value: String, modifier: Modifier = Modifier) {
    val active = value.trim() != "0" && value.isNotBlank()
    val textColor = if (active) MaterialTheme.colorScheme.onPrimary else Lipi.Muted
    val spokenDescription = stringResource(Res.string.cd_dose_slot, label, value)
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = spokenDescription }
            .background(if (active) Lipi.Primary else Lipi.ImageWell, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SlotGlyph(kind, textColor.copy(alpha = 0.9f), Modifier.size(12.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.9f))
        }
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
    }
}

/** Tiny drawn glyphs so no icon library is needed: rising sun, full sun, crescent moon. */
@Composable
private fun SlotGlyph(kind: SlotKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = s * 0.12f
        val c = Offset(size.width / 2f, size.height / 2f)
        when (kind) {
            SlotKind.NOON -> {
                drawCircle(tint, radius = s * 0.2f, center = c, style = Stroke(sw))
                for (i in 0 until 8) {
                    val a = i * PI / 4
                    val d = Offset(cos(a).toFloat(), sin(a).toFloat())
                    drawLine(tint, c + d * (s * 0.32f), c + d * (s * 0.46f), sw, StrokeCap.Round)
                }
            }

            SlotKind.MORNING -> {
                val horizon = s * 0.72f
                drawArc(
                    color = tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(c.x - s * 0.22f, horizon - s * 0.22f),
                    size = Size(s * 0.44f, s * 0.44f), style = Stroke(sw),
                )
                drawLine(tint, Offset(s * 0.08f, horizon), Offset(s * 0.92f, horizon), sw, StrokeCap.Round)
                drawLine(tint, Offset(c.x, s * 0.1f), Offset(c.x, s * 0.26f), sw, StrokeCap.Round)
            }

            SlotKind.NIGHT -> {
                val outer = Path().apply { addOval(Rect(c, s * 0.4f)) }
                val bite = Path().apply { addOval(Rect(Offset(c.x + s * 0.2f, c.y - s * 0.1f), s * 0.34f)) }
                drawPath(Path.combine(PathOperation.Difference, outer, bite), tint)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pending / skeleton pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun rememberShimmerAlpha(active: Boolean, reduceMotion: Boolean): Float {
    if (!active || reduceMotion) return if (active) 0.7f else 0.45f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    return alpha
}

/** Agent icon + short label. WAITING dims, RUNNING pulses. */
@Composable
private fun PendingLine(
    agent: RxAgentType,
    phase: FieldPhase,
    label: String,
    reduceMotion: Boolean,
) {
    val running = phase == FieldPhase.RUNNING
    val waitingText = stringResource(Res.string.field_waiting)
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (running) label else "$label · $waitingText"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AgentIcon(
            agent = agent,
            status = if (running) AgentStatus.RUNNING else AgentStatus.WAITING,
            size = 16.dp,
            showDoneBadge = false,
            reduceMotion = reduceMotion,
        )
        Text(
            text = if (running) label else waitingText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Lipi.Muted.copy(alpha = if (running) 1f else 0.7f),
        )
    }
}

@Composable
private fun DoseSkeleton(phase: FieldPhase, reduceMotion: Boolean) {
    val shimmer = rememberShimmerAlpha(phase == FieldPhase.RUNNING, reduceMotion)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .alpha(shimmer)
                        .background(Lipi.ImageWell, RoundedCornerShape(12.dp))
                )
            }
        }
        PendingLine(
            agent = RxAgentType.ENTITY_PARSER,
            phase = phase,
            label = stringResource(Res.string.field_dose_pending),
            reduceMotion = reduceMotion,
        )
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp, alpha: Float) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .alpha(alpha)
            .background(Lipi.ImageWell, RoundedCornerShape(50))
    )
}

@Composable
private fun ReviewBanner(text: String) {
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
            text = text,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Lipi.Error,
        )
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

// ---------------------------------------------------------------------------------------------
// Parsing helpers
// ---------------------------------------------------------------------------------------------

/** Brand without a trailing strength number, since the strength is shown as its own pill. */
private fun splitTitle(name: String, strength: String): String {
    val brand = name.trim()
    val number = strength.trim().substringBefore(" ").trim()
    return if (number.isNotBlank() && brand.endsWith(number) && brand.length > number.length) {
        brand.removeSuffix(number).trim()
    } else {
        brand
    }
}

/** Meta line items in the demo's order: meal timing, quantity, form. Instructions live in the note row. */
private fun buildMeta(item: MedicineItem, mealLabel: String?): List<MetaItem> = buildList {
    mealLabel?.let { add(MetaItem(MetaKind.MEAL, it)) }
    if (item.units > 0 || item.unitType.isNotBlank()) add(MetaItem(MetaKind.QUANTITY, quantityText(item.units, item.unitType)))
    item.dosageForm.takeIf { it.isNotBlank() && !it.equals(item.unitType, ignoreCase = true) }
        ?.let { add(MetaItem(MetaKind.FORM, it)) }
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

private fun fixInstructionsText(text: String): String {
    var clean = text.trim().removeSuffix("?").trim()
    if (clean == "সাথ") clean = "খাবারের সাথে"
    return clean
}