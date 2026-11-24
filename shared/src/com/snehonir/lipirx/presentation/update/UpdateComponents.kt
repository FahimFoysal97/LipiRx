package com.snehonir.lipirx.presentation.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snehonir.lipirx.domain.update.UpdateHeroIcon
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.domain.update.UpdateState
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.presentation.common.SecondaryButton
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
    val requester = remember { BringIntoViewRequester() }
    var wasChecking by remember { mutableStateOf(false) }

    // Inline (Settings) only: when a manual check finishes, scroll the section into view.
    LaunchedEffect(state::class) {
        if (isDialog) return@LaunchedEffect
        val isChecking = state is UpdateState.Checking
        if (isChecking) wasChecking = true
        if (isChecking || wasChecking) {
            withFrameNanos { }              // wait a frame so the new content is measured
            requester.bringIntoView()
            wasChecking = isChecking        // stays true while checking, resets after the result
        }
    }

    if (state is UpdateState.Idle) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        UpdateHeroIcon(state)

        when (state) {
            is UpdateState.Checking -> HeroTitle(stringResource(Res.string.update_checking))

            is UpdateState.UpToDate ->
                HeroTitle(stringResource(Res.string.update_up_to_date, updateManager.currentVersionName))

            is UpdateState.Available -> {
                HeroTitle(stringResource(Res.string.update_available_title, state.updateInfo.versionName))
                VersionChange(updateManager.currentVersionName, state.updateInfo.versionName)
                BodyText(stringResource(Res.string.update_available_msg, state.updateInfo.versionName))
                if (state.updateInfo.releaseNotes.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Lipi.ImageWell,
                        border = BorderStroke(1.dp, Lipi.CardBorder)
                    ) {
                        Column(Modifier.padding(14.dp).verticalScroll(rememberScrollState())) {
                            MarkdownText(markdown = state.updateInfo.releaseNotes)
                        }
                    }
                }
                ActionArea(state, isDialog, updateManager)
            }

            is UpdateState.Downloading -> {
                val sizeKnown = state.totalBytes > 0L

                Text(
                    text = if (sizeKnown) "${formatMb(state.bytesDownloaded)} / ${formatMb(state.totalBytes)}"
                    else formatMb(state.bytesDownloaded),
                    fontSize = 13.sp,
                    color = Lipi.Muted
                )

                HeroTitle(stringResource(Res.string.update_downloading_title))

                Text(
                    text = stringResource(Res.string.update_keep_open_hint),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Lipi.Muted,
                    textAlign = TextAlign.Center
                )
            }

            is UpdateState.NeedsInstallPermission -> {
                HeroTitle(stringResource(Res.string.update_permission_title))
                BodyText(stringResource(Res.string.update_permission_msg))
                ActionArea(state, isDialog, updateManager)
            }

            is UpdateState.ReadyToInstall -> {
                HeroTitle(stringResource(Res.string.update_ready_title))
                BodyText(stringResource(Res.string.update_ready_msg, state.updateInfo.versionName))
                ActionArea(state, isDialog, updateManager)
            }

            is UpdateState.Error -> {
                Text(
                    text = state.message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Lipi.Error,
                    textAlign = TextAlign.Center
                )
                ActionArea(state, isDialog, updateManager)
            }
        }
    }
}

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
        color = Lipi.Muted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TwoButtonRow(
    secondaryText: String,
    onSecondary: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryIcon: ImageVector? = null,
    primaryModifier: Modifier = Modifier,
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
            modifier = Modifier.weight(1f).then(primaryModifier)
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

@Composable
private fun HeroTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Lipi.Ink,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ActionArea(
    state: UpdateState,
    isDialog: Boolean,
    updateManager: UpdateManager,
) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is UpdateState.Available -> Buttons(
                isDialog,
                stringResource(Res.string.btn_later), { updateManager.dismissDialog() },
                stringResource(Res.string.btn_update_now), { updateManager.startDownload(state.updateInfo) }
            )

            is UpdateState.NeedsInstallPermission -> Buttons(
                isDialog,
                stringResource(Res.string.btn_later), { updateManager.dismissDialog() },
                stringResource(Res.string.btn_open_settings), { updateManager.openInstallSettings() }
            )

            is UpdateState.ReadyToInstall -> {
                val pulse by rememberInfiniteTransition(label = "cta").animateFloat(
                    1f, 1.04f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "cta_scale"
                )
                Buttons(
                    isDialog,
                    stringResource(Res.string.btn_later), { updateManager.dismissDialog() },
                    stringResource(Res.string.btn_install_now), { updateManager.installNow(state.apkPath) },
                    primaryModifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }
                )
            }

            is UpdateState.Error -> Buttons(
                isDialog,
                stringResource(Res.string.btn_cancel), { updateManager.dismissDialog() },
                stringResource(Res.string.btn_retry),
                { updateManager.checkForUpdates(silent = false, fromSettings = !isDialog) },
                primaryIcon = Icons.Default.Refresh
            )

            else -> Unit
        }
    }
}

/** Dialog: [secondary][primary]. Inline (settings): primary only. */
@Composable
private fun Buttons(
    isDialog: Boolean,
    secondaryText: String, onSecondary: () -> Unit,
    primaryText: String, onPrimary: () -> Unit,
    primaryIcon: ImageVector? = null,
    primaryModifier: Modifier = Modifier,
) {
    if (isDialog) {
        TwoButtonRow(secondaryText, onSecondary, primaryText, onPrimary, primaryIcon, primaryModifier)
    } else {
        PrimaryButton(
            text = primaryText,
            icon = primaryIcon,
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth().then(primaryModifier)
        )
    }
}