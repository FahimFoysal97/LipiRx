package com.snehonir.lipirx.presentation.results

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.AgentStatus
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.presentation.common.HistoryChip
import com.snehonir.lipirx.presentation.common.LipiRxScaffold
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.presentation.common.SecondaryButton
import com.snehonir.lipirx.presentation.results.components.AnalysisHeader
import com.snehonir.lipirx.presentation.results.components.MedicineItemCard
import com.snehonir.lipirx.presentation.results.components.toCardProgress
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.banner_items_flagged
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_copied
import com.snehonir.lipirx.resources.btn_copy_summary
import com.snehonir.lipirx.resources.btn_done
import com.snehonir.lipirx.resources.btn_keep_waiting
import com.snehonir.lipirx.resources.btn_scan_again
import com.snehonir.lipirx.resources.btn_sign_off
import com.snehonir.lipirx.resources.btn_stop
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.cd_back_to_history
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.dialog_leave_body
import com.snehonir.lipirx.resources.dialog_leave_confirm
import com.snehonir.lipirx.resources.dialog_leave_title
import com.snehonir.lipirx.resources.dialog_signoff_body
import com.snehonir.lipirx.resources.dialog_signoff_confirm
import com.snehonir.lipirx.resources.dialog_signoff_title
import com.snehonir.lipirx.resources.dialog_stop_message
import com.snehonir.lipirx.resources.dialog_stop_title
import com.snehonir.lipirx.resources.dialog_stay
import com.snehonir.lipirx.resources.disclaimer_text
import com.snehonir.lipirx.resources.error_read_failed_title
import com.snehonir.lipirx.resources.field_reading_pending
import com.snehonir.lipirx.resources.filter_all_count
import com.snehonir.lipirx.resources.filter_needs_review_count
import com.snehonir.lipirx.resources.label_medicines_found
import com.snehonir.lipirx.resources.label_needs_pharmacist_review
import com.snehonir.lipirx.resources.label_one_medicine_found
import com.snehonir.lipirx.resources.label_pharmacist_verified
import com.snehonir.lipirx.resources.no_medicines_subtitle
import com.snehonir.lipirx.resources.no_medicines_title
import com.snehonir.lipirx.resources.prescription_scanned
import com.snehonir.lipirx.resources.results_title
import com.snehonir.lipirx.resources.saved_prescription_record
import com.snehonir.lipirx.resources.tap_to_zoom
import com.snehonir.lipirx.resources.view_compact
import com.snehonir.lipirx.resources.view_detailed
import com.snehonir.lipirx.ui.BackHandler
import com.snehonir.lipirx.ui.rememberReduceMotion
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.AgentIcon
import com.snehonir.lipirx.ui.theme.icons.CopyVectorIcon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.material3.TextButton as StopTextButton

/** How long the Copy button shows "Copied" before going back to normal. */
private const val COPIED_FEEDBACK_MS = 2_000L

@Composable
fun MedicineListView(
    imageByteArray: ByteArray?,
    medicines: List<MedicineItem>,
    isSuspendedForApproval: Boolean = false,
    approvalReason: String? = null,
    selectedHistoryItem: PrescriptionHistoryEntity? = null,
    onZoom: () -> Unit,
    onCopySummary: () -> Unit,
    onDone: () -> Unit = {},
    onRescan: () -> Unit = {},
    onApproveAndConfirm: () -> Unit = {},
    onUpdateMedicineItem: ((index: Int, updatedItem: MedicineItem) -> Unit)? = null,
    onHistoryClick: (() -> Unit)? = null,
    onBackFromHistory: (() -> Unit)? = null,
    isLoading: Boolean = false,
    progress: PipelineProgress = PipelineProgress(),
    errorMessage: String? = null,
    onStopAnalysis: () -> Unit = {},
) {
    val isHistory = selectedHistoryItem != null
    val reduceMotion = rememberReduceMotion()
    // Flags are only trustworthy once the safety agent has finished, so no counts while running.
    val reviewCount = remember(medicines, isLoading) {
        if (isLoading) 0 else medicines.count { it.needsReview }
    }
    val cardProgress = remember(progress, isLoading) { progress.toCardProgress(isLoading) }
    var confirmStop by remember { mutableStateOf(false) }
    var compactView by rememberSaveable { mutableStateOf(false) }

    var showOnlyReview by rememberSaveable { mutableStateOf(false) }
    val filterActive = showOnlyReview && reviewCount > 0

    // Order never changes (numbers keep matching the paper prescription and cards do not jump
    // while agents work). The "Needs review" filter is how flagged items are surfaced.
    val visibleItems = remember(medicines, filterActive) {
        medicines.withIndex().filter { !filterActive || it.value.needsReview }.toList()
    }

    val canEdit = selectedHistoryItem == null && onUpdateMedicineItem != null

    // ── Dialog state ─────────────────────────────────────────────────────────
    var showSignOffConfirm by remember { mutableStateOf(false) }
    var pendingExit by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Only guard when leaving a live scan that still needs sign-off.
    val guardedExit: (() -> Unit) -> (() -> Unit) = { action ->
        {
            if (isSuspendedForApproval && selectedHistoryItem == null) pendingExit =
                action else action()
        }
    }

    // ── Copy feedback ────────────────────────────────────────────────────────
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MS.milliseconds)
            copied = false
        }
    }

    val handleBackNavigation: () -> Unit = {
        when {
            isHistory -> onBackFromHistory?.invoke()
            isLoading -> confirmStop = true
            else -> guardedExit(onDone).invoke()
        }
    }

    BackHandler(enabled = true) {
        handleBackNavigation()
    }

    LipiRxScaffold(
        title = selectedHistoryItem?.formattedDateTime ?: stringResource(Res.string.results_title),
        subtitle = if (selectedHistoryItem != null) stringResource(Res.string.saved_prescription_record) else null,
        onBack = handleBackNavigation,
        backContentDescription = if (selectedHistoryItem != null) stringResource(Res.string.cd_back_to_history) else "",
        topBarActions = {
            if (selectedHistoryItem == null && onHistoryClick != null) {
                HistoryChip(onClick = onHistoryClick)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 130.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!isHistory) {
                    item {
                        AnalysisHeader(
                            progress = progress,
                            imageByteArray = imageByteArray,
                            isRunning = isLoading,
                            isSuspended = !isLoading && isSuspendedForApproval,
                            onZoom = onZoom,
                            reduceMotion = reduceMotion,
                        )
                    }
                }

                if (!isLoading && (isSuspendedForApproval || reviewCount > 0)) {
                    item {
                        PharmacistApprovalBanner(
                            reason = approvalReason
                                ?: stringResource(Res.string.banner_items_flagged, reviewCount),
                            isApproved = !isSuspendedForApproval,
                            // One tap should not verify a prescription: confirm first.
                            onApprove = { showSignOffConfirm = true }
                        )
                    }
                }

                if (isHistory) {
                    item {
                        SummaryCard(
                            medicineCount = medicines.size,
                            imageByteArray = imageByteArray,
                            onZoom = onZoom
                        )
                    }
                }

                if (!isLoading && medicines.size > 1) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (reviewCount > 0) {
                                FilterPill(
                                    text = stringResource(Res.string.filter_all_count, medicines.size),
                                    selected = !filterActive,
                                    accent = Lipi.Primary,
                                    onClick = { showOnlyReview = false }
                                )
                                FilterPill(
                                    text = stringResource(
                                        Res.string.filter_needs_review_count,
                                        reviewCount
                                    ),
                                    selected = filterActive,
                                    accent = Lipi.Error,
                                    icon = Icons.Default.Warning,
                                    onClick = { showOnlyReview = true }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            // Compact collapses every finished card to one line; flagged ones keep their red row.
                            FilterPill(
                                text = stringResource(if (compactView) Res.string.view_detailed else Res.string.view_compact),
                                selected = compactView,
                                accent = Lipi.Primary,
                                onClick = { compactView = !compactView }
                            )
                        }
                    }
                }

                if (medicines.isEmpty() && isLoading) {
                    item {
                        ReadingPlaceholderCard(
                            running = progress.worker(RxAgentType.TRANSCRIPTION).status == AgentStatus.RUNNING,
                            reduceMotion = reduceMotion,
                        )
                    }
                }

                if (medicines.isEmpty() && !isLoading) {
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
                                    text = stringResource(
                                        if (errorMessage != null) Res.string.error_read_failed_title
                                        else Res.string.no_medicines_title
                                    ),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Lipi.Ink
                                )
                                Text(
                                    text = errorMessage ?: stringResource(Res.string.no_medicines_subtitle),
                                    fontSize = 12.5.sp,
                                    color = Lipi.Muted,
                                    textAlign = TextAlign.Center
                                )
                                // Give the empty state a way forward.
                                if (selectedHistoryItem == null) {
                                    Spacer(modifier = Modifier.size(4.dp))
                                    PrimaryButton(
                                        text = stringResource(Res.string.btn_scan_again),
                                        icon = Icons.Default.Refresh,
                                        onClick = onRescan,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                items(visibleItems, key = { it.index }) { (originalIndex, item) ->
                    MedicineItemCard(
                        index = originalIndex + 1,
                        item = item,
                        progress = cardProgress,
                        isEditable = canEdit && !isLoading,
                        compact = compactView && !isLoading,
                        reduceMotion = reduceMotion,
                        onUpdateItem = { updated ->
                            onUpdateMedicineItem?.invoke(originalIndex, updated)
                        },
                        modifier = Modifier.animateItem()
                    )
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
                    if (isLoading) {
                        StopTextButton(
                            onClick = { confirmStop = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.btn_stop_analysis),
                                color = Lipi.Error,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } else {
                        // Nothing to copy when there are no medicines.
                        if (medicines.isNotEmpty()) {
                            SecondaryButton(
                                text = stringResource(if (copied) Res.string.btn_copied else Res.string.btn_copy_summary),
                                icon = if (copied) Icons.Default.Check else CopyVectorIcon,
                                onClick = {
                                    onCopySummary()
                                    copied = true
                                },
                                modifier = Modifier.weight(1f),
                                height = 52.dp
                            )
                        }

                        val handleDoneAction: () -> Unit =
                            if (selectedHistoryItem != null && onBackFromHistory != null) {
                                onBackFromHistory
                            } else {
                                guardedExit(onDone)
                            }

                        PrimaryButton(
                            text = stringResource(Res.string.btn_done),
                            icon = Icons.Default.Check,
                            onClick = handleDoneAction,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // ── Stop confirmation (button, top-bar back, system Back) ────────────────
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(Res.string.dialog_stop_title)) },
            text = { Text(stringResource(Res.string.dialog_stop_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    onStopAnalysis()
                }) { Text(stringResource(Res.string.btn_stop), color = Lipi.Error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) {
                    Text(stringResource(Res.string.btn_keep_waiting))
                }
            },
            containerColor = Lipi.Surface
        )
    }

    // ── Sign-off confirmation ────────────────────────────────────────────────
    if (showSignOffConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOffConfirm = false },
            title = { Text(stringResource(Res.string.dialog_signoff_title)) },
            text = { Text(stringResource(Res.string.dialog_signoff_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSignOffConfirm = false
                    onApproveAndConfirm()
                }) {
                    Text(stringResource(Res.string.dialog_signoff_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOffConfirm = false }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
            },
            containerColor = Lipi.Surface
        )
    }

    // ── Leaving with unverified items ────────────────────────────────────────
    pendingExit?.let { exit ->
        AlertDialog(
            onDismissRequest = { pendingExit = null },
            title = { Text(stringResource(Res.string.dialog_leave_title)) },
            text = { Text(stringResource(Res.string.dialog_leave_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingExit = null
                    exit()
                }) {
                    Text(stringResource(Res.string.dialog_leave_confirm), color = Lipi.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExit = null }) {
                    Text(stringResource(Res.string.dialog_stay))
                }
            },
            containerColor = Lipi.Surface
        )
    }
}

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
        border = BorderStroke(
            1.dp,
            if (isApproved) Lipi.Primary else Lipi.OnAmber.copy(alpha = 0.4f)
        )
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
                // Bigger thumbnail: this is what people compare the results against.
                AsyncImage(
                    model = imageByteArray,
                    contentDescription = stringResource(Res.string.content_desc_prescription),
                    modifier = Modifier
                        .size(64.dp, 80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Lipi.ImageWell),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (medicineCount == 1) stringResource(Res.string.label_one_medicine_found) else stringResource(
                        Res.string.label_medicines_found,
                        medicineCount
                    ),
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
                        fontSize = 11.sp,
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
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = accent
                )
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

/** Shown before the first medicine is found, so the screen is never empty while reading. */
@Composable
private fun ReadingPlaceholderCard(running: Boolean, reduceMotion: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AgentIcon(
                agent = RxAgentType.TRANSCRIPTION,
                status = if (running) AgentStatus.RUNNING else AgentStatus.WAITING,
                size = 32.dp,
                showDoneBadge = false,
                reduceMotion = reduceMotion,
            )
            Text(
                text = stringResource(Res.string.field_reading_pending),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Muted,
            )
        }
    }
}