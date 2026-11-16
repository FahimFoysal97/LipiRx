package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_analyze_prescription
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_change
import com.snehonir.lipirx.resources.btn_clear
import com.snehonir.lipirx.resources.btn_configure
import com.snehonir.lipirx.resources.btn_retake
import com.snehonir.lipirx.resources.btn_show_less
import com.snehonir.lipirx.resources.btn_show_more
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.header_settings
import com.snehonir.lipirx.resources.hint_zoom_pan
import com.snehonir.lipirx.resources.title_analysis_notice
import com.snehonir.lipirx.resources.title_prescription_preview
import com.snehonir.lipirx.ui.theme.Lipi
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
    onOpenAiStudio: (() -> Unit)? = onOpenSettings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clickable(enabled = isLongError) { isErrorExpanded = !isErrorExpanded },
                shape = RoundedCornerShape(14.dp),
                color = Lipi.AmberContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                text = if (isErrorExpanded) stringResource(Res.string.btn_show_less) else stringResource(Res.string.btn_show_more),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.OnAmber
                            )
                        }
                    }
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Lipi.OnAmber,
                        maxLines = if (isErrorExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Image card with rounded outer container and rounded inner image
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 380.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Lipi.ImageWell)
                .clickable(onClick = onZoom),
        ) {
            AsyncImage(
                model = imageByteArray,
                contentDescription = stringResource(Res.string.content_desc_prescription),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit,
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                shape = CircleShape,
                color = Lipi.ViewerBg.copy(alpha = 0.88f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Text(stringResource(Res.string.hint_zoom_pan), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PipelineSummaryCard(
                agentConfigs = agentConfigs,
                onClick = { (onOpenSettings ?: onOpenAiStudio)?.invoke() }
            )

            PrimaryButton(
                text = stringResource(Res.string.btn_analyze_prescription),
                icon = Icons.Default.PlayArrow,
                onClick = onAnalyze
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(Res.string.btn_retake), Icons.Default.Refresh, onRetake, Modifier.weight(1f), fontSize = 13)
                SecondaryButton(stringResource(Res.string.btn_change), Icons.Default.Search, onChange, Modifier.weight(1f), fontSize = 13)
                SecondaryButton(stringResource(Res.string.btn_clear), Icons.Default.Clear, onClear, Modifier.weight(1f), contentColor = Lipi.Error, fontSize = 13)
            }
        }
    }
}

/** Shows summary of all 4 active AI pipeline agents & models. Tappable to open theme-matched bottom sheet. */
@Composable
private fun PipelineSummaryCard(
    agentConfigs: Map<RxAgentType, AgentConfig>,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
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
                    Text(
                        text = stringResource(Res.string.header_settings),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(stringResource(Res.string.btn_configure), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Primary)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Lipi.Primary
                    )
                }
            }

            RxAgentType.ALL.forEach { agentType ->
                val config = agentConfigs[agentType]
                val provider = config?.provider ?: agentType.defaultProvider
                val model = config?.selectedModel?.ifBlank { agentType.defaultModel } ?: agentType.defaultModel

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(agentType.displayNameRes),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Lipi.Muted
                    )

                    Surface(
                        shape = CircleShape,
                        color = provider.brandColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, provider.brandColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = provider.icon,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = provider.brandColor
                            )
                            Text(
                                text = "${provider.displayName} ($model)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = provider.brandColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
