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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.ui.theme.Lipi

@Composable
fun EmptyStateView(
    selectedModel: String,
    availableModels: List<String>,
    apiKey: String = "",
    apiKeyError: String? = null,
    isTestingApiKey: Boolean = false,
    onSaveApiKey: (String) -> Unit = {},
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
                apiKeyError = apiKeyError,
                isTestingApiKey = isTestingApiKey,
                onSaveApiKey = onSaveApiKey,
                onModelSelected = onModelSelected
            )
        }

        // Viewfinder hero
        Box(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Lipi.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val s = 34.dp.toPx()
                val m = 22.dp.toPx()
                val sw = 3.dp.toPx()
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(Lipi.Primary, Offset(x, y), Offset(x + dx * s, y), sw, StrokeCap.Round)
                    drawLine(Lipi.Primary, Offset(x, y), Offset(x, y + dy * s), sw, StrokeCap.Round)
                }
                corner(m, m, 1f, 1f)
                corner(size.width - m, m, -1f, 1f)
                corner(m, size.height - m, 1f, -1f)
                corner(size.width - m, size.height - m, -1f, -1f)
            }
            PaperIllustration(186.dp, 240.dp, rotation = -4f)
        }

        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
            Text(
                text = "Read any prescription, instantly.",
                fontSize = 30.sp,
                lineHeight = 34.sp,
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!isModelAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Lipi.AmberContainer
                ) {
                    Text(
                        text = "🔑 Please tap the model button on top right to enter your Gemini API Key and load models.",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.OnAmber
                    )
                }
            }

            PrimaryButton(
                text = "Take Prescription Photo",
                icon = Icons.Default.Add,
                onClick = onTakePhoto,
                enabled = isModelAvailable
            )
            SecondaryButton(
                text = "Choose from Gallery",
                icon = Icons.Default.Search,
                onClick = onChooseGallery,
                modifier = Modifier.fillMaxWidth(),
                enabled = isModelAvailable,
                height = 56.dp,
                fontSize = 16,
            )
        }
    }
}
