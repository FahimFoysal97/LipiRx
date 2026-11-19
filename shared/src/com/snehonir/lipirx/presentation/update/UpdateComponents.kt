package com.snehonir.lipirx.presentation.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.domain.update.UpdateState
import com.snehonir.lipirx.presentation.scan.components.PrimaryButton
import com.snehonir.lipirx.presentation.scan.components.SecondaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_install_now
import com.snehonir.lipirx.resources.btn_later
import com.snehonir.lipirx.resources.btn_open_settings
import com.snehonir.lipirx.resources.btn_retry
import com.snehonir.lipirx.resources.btn_update_now
import com.snehonir.lipirx.resources.update_available_msg
import com.snehonir.lipirx.resources.update_available_title
import com.snehonir.lipirx.resources.update_checking
import com.snehonir.lipirx.resources.update_downloading_title
import com.snehonir.lipirx.resources.update_keep_open_hint
import com.snehonir.lipirx.resources.update_permission_msg
import com.snehonir.lipirx.resources.update_permission_title
import com.snehonir.lipirx.resources.update_ready_msg
import com.snehonir.lipirx.resources.update_ready_title
import com.snehonir.lipirx.resources.update_up_to_date
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateDialog(
    updateManager: UpdateManager
) {
    val state by updateManager.updateState.collectAsState()
    val isDialogDismissed by updateManager.isDialogDismissed.collectAsState()

    // An Error that follows a real update step (e.g. the download failed) must be shown,
    // otherwise the dialog just vanishes. An Error from a silent background check must stay hidden.
    var lastWasActive by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        val active = state is UpdateState.Available ||
                state is UpdateState.Downloading ||
                state is UpdateState.NeedsInstallPermission ||
                state is UpdateState.ReadyToInstall
        showError = state is UpdateState.Error && lastWasActive
        lastWasActive = active
    }

    val showDialog = !isDialogDismissed && (showError || when (state) {
        is UpdateState.Available,
        is UpdateState.Downloading,
        is UpdateState.NeedsInstallPermission,
        is UpdateState.ReadyToInstall -> true
        else -> false
    })

    if (showDialog) {
        val isDownloading = state is UpdateState.Downloading

        Dialog(
            onDismissRequest = {
                if (!isDownloading) updateManager.dismissDialog()
            },
            properties = DialogProperties(
                dismissOnBackPress = !isDownloading,
                // Tapping outside is a natural "later" everywhere except mid-download,
                // where an accidental tap shouldn't hide progress.
                dismissOnClickOutside = !isDownloading,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 420.dp) // keeps the card tidy on tablets / desktop
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Lipi.Surface,
                border = BorderStroke(1.dp, Lipi.CardBorder),
                shadowElevation = 8.dp
            ) {
                // Crossfade between steps. The height is deliberately NOT animated: a Dialog is its
                // own window that re-centres itself on every size change, so animating the height
                // (and especially two size animations at once) makes the whole card shake.
                // Fade only, and let the size change once.
                AnimatedContent(
                    targetState = state,
                    contentKey = { it::class },
                    transitionSpec = {
                        (fadeIn(tween(180, delayMillis = 60)) togetherWith fadeOut(tween(120))) using
                                SizeTransform(clip = false) { _, _ -> snap() }
                    },
                    label = "update_step"
                ) { animatedState ->
                    // During the exit animation `animatedState` is the old state; during
                    // progress ticks inside the same class we use the live `state`.
                    val shown = if (animatedState::class == state::class) state else animatedState
                    UpdateContent(
                        state = shown,
                        updateManager = updateManager,
                        modifier = Modifier.padding(22.dp),
                        isDialog = true
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateContent(
    state: UpdateState,
    updateManager: UpdateManager,
    modifier: Modifier = Modifier,
    isDialog: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (state) {
            is UpdateState.Idle -> {
                // Nothing rendered when idle
            }

            is UpdateState.Checking -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Lipi.Primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(Res.string.update_checking),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.Muted
                    )
                }
            }

            is UpdateState.UpToDate -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Lipi.PrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Lipi.Primary
                        )
                        Text(
                            text = stringResource(Res.string.update_up_to_date, updateManager.currentVersionName),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.OnPrimaryContainer
                        )
                    }
                }
            }

            is UpdateState.Available -> {
                UpdateHeader(
                    icon = Icons.Default.Refresh,
                    iconTint = Lipi.Primary,
                    iconContainer = Lipi.PrimaryContainer,
                    title = stringResource(Res.string.update_available_title, state.updateInfo.versionName)
                )

                // Shows exactly what is changing: "installed -> new".
                VersionChange(
                    from = updateManager.currentVersionName,
                    to = state.updateInfo.versionName
                )

                BodyText(stringResource(Res.string.update_available_msg, state.updateInfo.versionName))

                // Release notes (when the release has any): short, scrollable, never pushes the buttons off screen.
                if (state.updateInfo.releaseNotes.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Lipi.ImageWell,
                        border = BorderStroke(1.dp, Lipi.CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Release notes are written in Markdown, so render them instead of showing raw symbols.
                            MarkdownText(markdown = state.updateInfo.releaseNotes)
                        }
                    }
                }

                if (isDialog) {
                    TwoButtonRow(
                        secondaryText = stringResource(Res.string.btn_later),
                        onSecondary = { updateManager.dismissDialog() },
                        primaryText = stringResource(Res.string.btn_update_now),
                        onPrimary = { updateManager.startDownload(state.updateInfo) }
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.btn_update_now),
                        icon = null,
                        onClick = { updateManager.startDownload(state.updateInfo) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            is UpdateState.Downloading -> {
                UpdateHeader(
                    icon = Icons.Default.Refresh,
                    iconTint = Lipi.Primary,
                    iconContainer = Lipi.PrimaryContainer,
                    title = stringResource(Res.string.update_downloading_title)
                )

                val sizeKnown = state.totalBytes > 0L
                val animatedProgress by animateFloatAsState(
                    targetValue = state.progress.coerceIn(0f, 1f),
                    animationSpec = tween(300),
                    label = "download_progress"
                )

                // Unknown size: show a moving bar instead of a stuck 0%.
                if (sizeKnown) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .semantics { contentDescription = "${(animatedProgress * 100).toInt()}%" },
                        color = Lipi.Primary,
                        trackColor = Lipi.ImageWell,
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = Lipi.Primary,
                        trackColor = Lipi.ImageWell,
                    )
                }

                // Fixed minimum height: the big "%" text appears once the total size is known, and
                // without this the row grows by a few dp mid-download and resizes the whole dialog.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (sizeKnown) "${formatMb(state.bytesDownloaded)} / ${formatMb(state.totalBytes)}"
                        else formatMb(state.bytesDownloaded),
                        fontSize = 12.sp,
                        color = Lipi.Muted
                    )
                    if (sizeKnown) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Primary
                        )
                    }
                }

                Text(
                    text = stringResource(Res.string.update_keep_open_hint),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Lipi.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is UpdateState.NeedsInstallPermission -> {
                UpdateHeader(
                    icon = Icons.Default.Warning,
                    iconTint = Lipi.OnAmber,
                    iconContainer = Lipi.AmberContainer,
                    title = stringResource(Res.string.update_permission_title)
                )

                BodyText(stringResource(Res.string.update_permission_msg))

                if (isDialog) {
                    TwoButtonRow(
                        secondaryText = stringResource(Res.string.btn_later),
                        onSecondary = { updateManager.dismissDialog() },
                        primaryText = stringResource(Res.string.btn_open_settings),
                        onPrimary = { updateManager.openInstallSettings() }
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.btn_open_settings),
                        icon = null,
                        onClick = { updateManager.openInstallSettings() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            is UpdateState.ReadyToInstall -> {
                UpdateHeader(
                    icon = Icons.Default.CheckCircle,
                    iconTint = Lipi.Primary,
                    iconContainer = Lipi.PrimaryContainer,
                    title = stringResource(Res.string.update_ready_title)
                )

                BodyText(stringResource(Res.string.update_ready_msg, state.updateInfo.versionName))

                if (isDialog) {
                    TwoButtonRow(
                        secondaryText = stringResource(Res.string.btn_later),
                        onSecondary = { updateManager.dismissDialog() },
                        primaryText = stringResource(Res.string.btn_install_now),
                        onPrimary = { updateManager.installNow(state.apkPath) }
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.btn_install_now),
                        icon = null,
                        onClick = { updateManager.installNow(state.apkPath) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            is UpdateState.Error -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                    shape = RoundedCornerShape(14.dp),
                    color = Lipi.Error.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Lipi.Error
                        )
                        Text(
                            text = state.message,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Error
                        )
                    }
                }

                if (isDialog) {
                    TwoButtonRow(
                        secondaryText = stringResource(Res.string.btn_cancel),
                        onSecondary = { updateManager.dismissDialog() },
                        primaryText = stringResource(Res.string.btn_retry),
                        onPrimary = { updateManager.checkForUpdates(silent = false, fromSettings = false) },
                        primaryIcon = Icons.Default.Refresh
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(Res.string.btn_retry),
                        icon = Icons.Default.Refresh,
                        onClick = { updateManager.checkForUpdates(silent = false, fromSettings = true) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Round icon badge + title, the same header for every step of the update flow. */
@Composable
private fun UpdateHeader(
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Lipi.Ink
        )
    }
}

/** Small pill: installed version -> new version. */
@Composable
private fun VersionChange(from: String, to: String) {
    Surface(
        shape = CircleShape,
        color = Lipi.PrimaryContainer
    ) {
        Text(
            text = "$from  →  $to",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Lipi.OnPrimaryContainer
        )
    }
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Lipi.Muted
    )
}

@Composable
private fun TwoButtonRow(
    secondaryText: String,
    onSecondary: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SecondaryButton(
            text = secondaryText,
            icon = null,
            onClick = onSecondary,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = primaryText,
            icon = primaryIcon,
            onClick = onPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mbTimes10 = (bytes * 10L) / (1024L * 1024L)
    val whole = mbTimes10 / 10L
    val decimal = mbTimes10 % 10L
    return if (whole == 0L && decimal == 0L) "< 0.1 MB" else "$whole.$decimal MB"
}
