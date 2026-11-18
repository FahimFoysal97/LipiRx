package com.snehonir.lipirx.presentation.update

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.domain.update.UpdateState
import com.snehonir.lipirx.presentation.scan.components.PrimaryButton
import com.snehonir.lipirx.presentation.scan.components.SecondaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_install_now
import com.snehonir.lipirx.resources.btn_later
import com.snehonir.lipirx.resources.btn_open_settings
import com.snehonir.lipirx.resources.btn_retry
import com.snehonir.lipirx.resources.btn_update_now
import com.snehonir.lipirx.resources.update_available_title
import com.snehonir.lipirx.resources.update_checking
import com.snehonir.lipirx.resources.update_downloading_title
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

    val showDialog = when (state) {
        is UpdateState.Available,
        is UpdateState.Downloading,
        is UpdateState.NeedsInstallPermission,
        is UpdateState.ReadyToInstall -> true
        else -> false
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (state !is UpdateState.Downloading) {
                    updateManager.resetState()
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = state !is UpdateState.Downloading,
                dismissOnClickOutside = false
            ),
            title = null,
            text = {
                UpdateContent(
                    state = state,
                    updateManager = updateManager
                )
            },
            confirmButton = {},
            dismissButton = null,
            shape = RoundedCornerShape(22.dp),
            containerColor = Lipi.Surface
        )
    }
}

@Composable
fun UpdateContent(
    state: UpdateState,
    updateManager: UpdateManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            is UpdateState.Idle -> {
                // Nothing rendered when idle
            }

            is UpdateState.Checking -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                Text(
                    text = stringResource(Res.string.update_available_title, state.updateInfo.versionName),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )

                if (state.updateInfo.releaseNotes.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Lipi.ImageWell,
                        border = BorderStroke(1.dp, Lipi.CardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = state.updateInfo.releaseNotes,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = Lipi.Ink
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = stringResource(Res.string.btn_later),
                        icon = null,
                        onClick = { updateManager.resetState() },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.btn_update_now),
                        icon = null,
                        onClick = { updateManager.startDownload(state.updateInfo) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            is UpdateState.Downloading -> {
                Text(
                    text = stringResource(Res.string.update_downloading_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )

                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Lipi.Primary,
                    trackColor = Lipi.ImageWell,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatMb(state.bytesDownloaded)} / ${formatMb(state.totalBytes)}",
                        fontSize = 12.sp,
                        color = Lipi.Muted
                    )
                    Text(
                        text = "${(state.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Primary
                    )
                }
            }

            is UpdateState.NeedsInstallPermission -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Lipi.Error
                    )
                    Text(
                        text = stringResource(Res.string.update_permission_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                }

                Text(
                    text = stringResource(Res.string.update_permission_msg),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = Lipi.Muted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = stringResource(Res.string.btn_later),
                        icon = null,
                        onClick = { updateManager.resetState() },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.btn_open_settings),
                        icon = null,
                        onClick = { updateManager.openInstallSettings() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            is UpdateState.ReadyToInstall -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Lipi.Primary
                    )
                    Text(
                        text = stringResource(Res.string.update_ready_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )
                }

                Text(
                    text = stringResource(Res.string.update_ready_msg, state.updateInfo.versionName),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = Lipi.Muted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = stringResource(Res.string.btn_later),
                        icon = null,
                        onClick = { updateManager.resetState() },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        text = stringResource(Res.string.btn_install_now),
                        icon = null,
                        onClick = { updateManager.installNow(state.apkPath) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            is UpdateState.Error -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Lipi.Error.copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Lipi.Error
                            )
                            Text(
                                text = state.message,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.Error
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SecondaryButton(
                        text = stringResource(Res.string.btn_retry),
                        icon = Icons.Default.Refresh,
                        onClick = { updateManager.checkForUpdates(silent = false) },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }
            }
        }
    }
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mbTimes10 = (bytes * 10L) / (1024L * 1024L)
    val whole = mbTimes10 / 10L
    val decimal = mbTimes10 % 10L
    return if (whole == 0L && decimal == 0L) "< 0.1 MB" else "$whole.$decimal MB"
}
