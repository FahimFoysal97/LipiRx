package com.snehonir.lipirx.presentation.preview

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.presentation.common.AppHeader
import com.snehonir.lipirx.presentation.common.PrimaryButton
import com.snehonir.lipirx.presentation.common.SecondaryButton
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_analyze_prescription
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_change
import com.snehonir.lipirx.resources.btn_configure
import com.snehonir.lipirx.resources.label_ai_pipeline
import com.snehonir.lipirx.resources.btn_retake
import com.snehonir.lipirx.resources.btn_show_less
import com.snehonir.lipirx.resources.btn_show_more
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.hint_zoom_pan
import com.snehonir.lipirx.resources.title_analysis_notice
import com.snehonir.lipirx.resources.title_prescription_preview
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.CameraVectorIcon
import com.snehonir.lipirx.ui.theme.icons.GalleryVectorIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImagePreviewAndProcessView(
    imageByteArray: ByteArray,
    errorMessage: String? = null,
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    onZoom: () -> Unit,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    onChange: () -> Unit,
    onClear: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    val zoomHint = stringResource(Res.string.hint_zoom_pan)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
    ) {
        AppHeader(
            onBack = onClear,
            backContentDescription = stringResource(Res.string.btn_cancel),
            title = stringResource(Res.string.title_prescription_preview)
        )

        if (!errorMessage.isNullOrBlank()) {
            var isErrorExpanded by remember { mutableStateOf(false) }
            val isLongError = errorMessage.length > 120 || errorMessage.count { it == '\n' } >= 3

            Surface(
                onClick = { isErrorExpanded = !isErrorExpanded },
                enabled = isLongError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Lipi.AmberContainer,
                border = BorderStroke(1.dp, Lipi.OnAmber.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Lipi.OnAmber
                        )
                        Text(
                            text = stringResource(Res.string.title_analysis_notice),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.OnAmber,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLongError) {
                            Text(
                                text = if (isErrorExpanded) stringResource(Res.string.btn_show_less)
                                else stringResource(Res.string.btn_show_more),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.OnAmber
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Lipi.OnAmber,
                            maxLines = if (isErrorExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Lipi.ImageWell)
                .clickable(role = Role.Button, onClickLabel = zoomHint, onClick = onZoom),
        ) {
            AsyncImage(
                model = imageByteArray,
                contentDescription = stringResource(Res.string.content_desc_prescription),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit,
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                shape = CircleShape,
                color = Lipi.ViewerBg.copy(alpha = 0.88f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Text(zoomHint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Lipi.Surface,
            shadowElevation = 16.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, Lipi.CardBorder),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PipelineSummary(
                    agentConfigs = agentConfigs,
                    onConfigure = onOpenSettings
                )

                PrimaryButton(
                    text = stringResource(Res.string.btn_analyze_prescription),
                    icon = Icons.Default.PlayArrow,
                    onClick = onAnalyze
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = stringResource(Res.string.btn_retake),
                        icon = CameraVectorIcon,
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        fontSize = 14
                    )
                    SecondaryButton(
                        text = stringResource(Res.string.btn_change),
                        icon = GalleryVectorIcon,
                        onClick = onChange,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        fontSize = 14
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineSummary(
    agentConfigs: Map<RxAgentType, AgentConfig>,
    onConfigure: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "pipelineChevron")

    val assignments = RxAgentType.ALL.map { agentType ->
        val config = agentConfigs[agentType]
        val provider = config?.provider ?: agentType.defaultProvider
        val rawModel = config?.selectedModel?.ifBlank { provider.defaultModel } ?: provider.defaultModel
        val model = formatModelDisplayName(rawModel)
        Triple(agentType, provider, model)
    }
    val providerNames = assignments.map { it.second.displayName }.distinct().joinToString(" · ")

    Surface(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Background,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Lipi.PrimaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Lipi.Primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.label_ai_pipeline),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink
                    )
                    Text(
                        text = providerNames,
                        fontSize = 11.sp,
                        color = Lipi.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onConfigure != null) {
                    Surface(
                        onClick = onConfigure,
                        modifier = Modifier.heightIn(min = 36.dp),
                        shape = CircleShape,
                        color = Lipi.PrimaryContainer
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.btn_configure),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.OnPrimaryContainer
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Hide pipeline details" else "Show pipeline details",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation),
                    tint = Lipi.Muted
                )
            }

            if (expanded) {
                assignments.forEach { (agentType, provider, model) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(agentType.displayNameRes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Muted,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.weight(1.4f, fill = false),
                            shape = CircleShape,
                            color = provider.brandColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, provider.brandColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = provider.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = provider.brandColor
                                )
                                val modelLabel = if (model.isNotBlank()) "${provider.displayName} ($model)" else provider.displayName
                                Text(
                                    text = modelLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = provider.brandColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatModelDisplayName(modelName: String): String {
    if (modelName.isBlank()) return ""
    var result = modelName.removePrefix(AppConstants.MODEL_PREFIX_GEMINI)
    AppConstants.SLM_MODEL_EXTENSIONS.forEach { ext ->
        if (result.endsWith(ext, ignoreCase = true)) {
            result = result.dropLast(ext.length)
        }
    }
    return result.trim()
}
