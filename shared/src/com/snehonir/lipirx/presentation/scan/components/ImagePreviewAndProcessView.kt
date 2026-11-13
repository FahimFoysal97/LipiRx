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
import com.snehonir.lipirx.data.local.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.ui.theme.Lipi

@Composable
fun ImagePreviewAndProcessView(
    imageByteArray: ByteArray,
    selectedModel: String,
    availableModels: List<String>,
    apiKey: String = "",
    geminiApiKey: String = "",
    groqApiKey: String = "",
    openaiApiKey: String = "",
    customApiKey: String = "",
    providerType: String = "gemini",
    customUrl: String = "",
    apiKeyError: String? = null,
    apiKeySuccess: String? = null,
    errorMessage: String? = null,
    isTestingApiKey: Boolean = false,
    onDeviceStrategy: OnDeviceStrategy = OnDeviceStrategy.HYBRID_FALLBACK,
    isMcpServerEnabled: Boolean = false,
    onToggleMcpServer: (Boolean) -> Unit = {},
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    historyList: List<PrescriptionHistoryEntity> = emptyList(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onSelectOnDeviceStrategy: (OnDeviceStrategy) -> Unit = {},
    onModelSelected: (String) -> Unit,
    onZoom: () -> Unit,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    onChange: () -> Unit,
    onClear: () -> Unit,
    onHistoryClick: (() -> Unit)? = null,
) {
    val imageSizeKb = imageByteArray.size / 1024
    var showModelBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader(
            onBack = onClear,
            backContentDescription = "Back",
            title = "Prescription Preview"
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
                            text = "Analysis Notice / Error",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.OnAmber,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLongError) {
                            Text(
                                text = if (isErrorExpanded) "Show less" else "Show more",
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
                contentDescription = "Selected prescription",
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
                color = Lipi.Ink.copy(alpha = 0.88f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Text("Tap photo to zoom & pan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
                onClick = { showModelBottomSheet = true }
            )

            PrimaryButton(
                text = "Analyze Prescription",
                icon = Icons.Default.PlayArrow,
                onClick = onAnalyze
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Retake", Icons.Default.Refresh, onRetake, Modifier.weight(1f), fontSize = 13)
                SecondaryButton("Change", Icons.Default.Search, onChange, Modifier.weight(1f), fontSize = 13)
                SecondaryButton("Clear", Icons.Default.Clear, onClear, Modifier.weight(1f), contentColor = Lipi.Error, fontSize = 13)
            }
        }
    }

    if (showModelBottomSheet) {
        ModelSelectionBottomSheet(
            selectedModel = selectedModel,
            apiKey = apiKey,
            geminiApiKey = geminiApiKey,
            groqApiKey = groqApiKey,
            openaiApiKey = openaiApiKey,
            customApiKey = customApiKey,
            providerType = providerType,
            customUrl = customUrl,
            apiKeyError = apiKeyError,
            apiKeySuccess = apiKeySuccess,
            isTestingApiKey = isTestingApiKey,
            onDeviceStrategy = onDeviceStrategy,
            isMcpServerEnabled = isMcpServerEnabled,
            onToggleMcpServer = onToggleMcpServer,
            agentConfigs = agentConfigs,
            providerModelsMap = providerModelsMap,
            onAgentProviderSelected = onAgentProviderSelected,
            onAgentModelSelected = onAgentModelSelected,
            onProviderTabSelected = onProviderTabSelected,
            onSaveProviderConfig = onSaveProviderConfig,
            onSelectOnDeviceStrategy = onSelectOnDeviceStrategy,
            onDismiss = { showModelBottomSheet = false }
        )
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
        color = Color.White,
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
                            imageVector = MultiAgentPipelineIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Lipi.Primary
                        )
                    }
                    Text(
                        text = "⚡ Active AI Pipeline (4 Agents)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Configure", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Primary)
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
                        text = agentType.displayName,
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
