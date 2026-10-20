package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.ui.theme.Lipi

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
    isTestingApiKey: Boolean = false,
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onModelSelected: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
) {
    val isModelAvailable = availableModels.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
    ) {
        AppHeader {
            ModelChip(
                selectedModel = selectedModel,
                availableModels = availableModels,
                apiKey = apiKey,
                geminiApiKey = geminiApiKey,
                groqApiKey = groqApiKey,
                openaiApiKey = openaiApiKey,
                customApiKey = customApiKey,
                providerType = providerType,
                customUrl = customUrl,
                apiKeyError = apiKeyError,
                isTestingApiKey = isTestingApiKey,
                onProviderTabSelected = onProviderTabSelected,
                onSaveProviderConfig = onSaveProviderConfig,
                onModelSelected = onModelSelected
            )
        }

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
            PaperIllustration(190.dp, 250.dp, rotation = -5f)
        }

        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 26.dp)) {
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

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!isModelAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Lipi.AmberContainer
                ) {
                    Text(
                        text = "🔑 Please tap the model button on top right to configure your AI Provider.",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.OnAmber
                    )
                }
            }

            PrimaryButton(
                text = "Take Prescription Photo",
                icon = CameraVectorIcon,
                onClick = onTakePhoto,
                enabled = isModelAvailable
            )
            SecondaryButton(
                text = "Choose from Gallery",
                icon = GalleryVectorIcon,
                onClick = onChooseGallery,
                modifier = Modifier.fillMaxWidth(),
                enabled = isModelAvailable,
                height = 58.dp,
                fontSize = 16,
            )
        }
    }
}
