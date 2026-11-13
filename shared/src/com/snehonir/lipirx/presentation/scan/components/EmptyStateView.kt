package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.data.local.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme

@Composable
fun EmptyStateView(
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
    isTestingApiKey: Boolean = false,
    onDeviceStrategy: com.snehonir.lipirx.domain.model.OnDeviceStrategy = com.snehonir.lipirx.domain.model.OnDeviceStrategy.HYBRID_FALLBACK,
    isMcpServerEnabled: Boolean = false,
    onToggleMcpServer: (Boolean) -> Unit = {},
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    historyList: List<PrescriptionHistoryEntity> = emptyList(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onSelectOnDeviceStrategy: (com.snehonir.lipirx.domain.model.OnDeviceStrategy) -> Unit = {},
    onModelSelected: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onHistoryClick: (() -> Unit)? = null,
) {
    var showModelBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
    ) {
        // Clean Top Bar Header
        AppHeader()

        // Viewfinder hero
        Box(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Lipi.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val armLength = 36.dp.toPx()
                val radius = 18.dp.toPx()
                val margin = 24.dp.toPx()
                val strokeWidth = 3.5.dp.toPx()

                // Top-Left L-bracket
                drawPath(
                    path = Path().apply {
                        moveTo(margin + armLength, margin)
                        lineTo(margin + radius, margin)
                        quadraticTo(margin, margin, margin, margin + radius)
                        lineTo(margin, margin + armLength)
                    },
                    color = Lipi.Primary,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Top-Right L-bracket
                drawPath(
                    path = Path().apply {
                        moveTo(size.width - margin - armLength, margin)
                        lineTo(size.width - margin - radius, margin)
                        quadraticTo(size.width - margin, margin, size.width - margin, margin + radius)
                        lineTo(size.width - margin, margin + armLength)
                    },
                    color = Lipi.Primary,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Bottom-Left L-bracket
                drawPath(
                    path = Path().apply {
                        moveTo(margin + armLength, size.height - margin)
                        lineTo(margin + radius, size.height - margin)
                        quadraticTo(margin, size.height - margin, margin, size.height - margin - radius)
                        lineTo(margin, size.height - margin - armLength)
                    },
                    color = Lipi.Primary,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Bottom-Right L-bracket
                drawPath(
                    path = Path().apply {
                        moveTo(size.width - margin - armLength, size.height - margin)
                        lineTo(size.width - margin - radius, size.height - margin)
                        quadraticTo(size.width - margin, size.height - margin, size.width - margin, size.height - margin - radius)
                        lineTo(size.width - margin, size.height - margin - armLength)
                    },
                    color = Lipi.Primary,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Central illustration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PaperIllustration(190.dp, 250.dp)
                //Text("Place prescription inside frame", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Lipi.OnPrimaryContainer)
            }
        }

        // Headline & Description Text Block
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Read any prescription,\ninstantly.",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Lipi.Ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Snap or upload a handwritten prescription. Get every medicine, schedule and alternative brand in seconds.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Lipi.Muted,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Control Dashboard (Row 1: Camera + Gallery | Row 2: Scan History + AI Pipeline)
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Primary Capture Controls (Both full 50% equal width)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(
                    text = "Take Photo",
                    icon = CameraVectorIcon,
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f)
                )

                SecondaryButton(
                    text = "Choose Gallery",
                    icon = GalleryVectorIcon,
                    onClick = onChooseGallery,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: System Tools & Settings (Both full 50% equal width)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    text = "Scan History",
                    icon = Icons.Default.DateRange,
                    onClick = { onHistoryClick?.invoke() },
                    modifier = Modifier.weight(1f),
                    contentColor = Lipi.Primary
                )

                SecondaryButton(
                    text = "AI & MCP Studio",
                    icon = MultiAgentPipelineIcon,
                    onClick = { showModelBottomSheet = true },
                    modifier = Modifier.weight(1f),
                    contentColor = Lipi.Ink
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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

@Preview
@Composable
fun EmptyStateViewPreview() {
    LipiRxTheme {
        EmptyStateView(
            selectedModel = "gemini-2.0-flash",
            availableModels = listOf("gemini-2.0-flash", "gemini-1.5-pro"),
            providerType = "gemini",
            onModelSelected = {},
            onTakePhoto = {},
            onChooseGallery = {}
        )
    }
}
