package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
    errorMessage: String? = null,
    isTestingApiKey: Boolean = false,
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onModelSelected: (String) -> Unit,
    onZoom: () -> Unit,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    onChange: () -> Unit,
    onClear: () -> Unit,
) {
    val imageSizeKb = imageByteArray.size / 1024
    var showModelBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
    ) {
        AppHeader { InfoChip("$imageSizeKb KB") }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Prescription ready", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
            Text("Check the photo is sharp, then analyze.", fontSize = 14.sp, color = Lipi.Muted)
        }

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = Lipi.AmberContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                        text = errorMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.OnAmber
                    )
                }
            }
        }

        // Image card with rounded outer container and rounded inner image
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .weight(1f)
                .fillMaxWidth()
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
            ModelSelectorCard(
                model = selectedModel,
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
            onModelSelected = onModelSelected,
            onDismiss = { showModelBottomSheet = false }
        )
    }
}

/** Shows which AI model will run. Tappable to open theme-matched bottom sheet. */
@Composable
private fun ModelSelectorCard(
    model: String,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(BorderStroke(1.dp, Lipi.Outline), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Lipi.PrimaryContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🤖", fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("AI model", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Lipi.Muted)
            Text(
                text = model.ifBlank { "Set AI Provider" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
                maxLines = 1
            )
        }
        if (onClick != null) {
            Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Primary)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Lipi.Primary
            )
        }
    }
}
