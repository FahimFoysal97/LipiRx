package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.coroutines.launch

val CameraVectorIcon = ImageVector.Builder(
    name = "CameraOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(23f, 19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 21f)
        lineTo(3f, 21f)
        arcTo(2f, 2f, 0f, false, true, 1f, 19f)
        lineTo(1f, 8f)
        arcTo(2f, 2f, 0f, false, true, 3f, 6f)
        lineTo(7f, 6f)
        lineTo(9f, 3f)
        lineTo(15f, 3f)
        lineTo(17f, 6f)
        lineTo(21f, 6f)
        arcTo(2f, 2f, 0f, false, true, 23f, 8f)
        close()
    }
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 17f)
        arcTo(3.5f, 3.5f, 0f, true, false, 12f, 10f)
        arcTo(3.5f, 3.5f, 0f, false, false, 12f, 17f)
        close()
    }
}.build()

val GalleryVectorIcon = ImageVector.Builder(
    name = "GalleryOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        arcTo(2f, 2f, 0f, false, false, 3f, 5f)
        lineTo(3f, 19f)
        arcTo(2f, 2f, 0f, false, false, 5f, 21f)
        lineTo(19f, 21f)
        arcTo(2f, 2f, 0f, false, false, 21f, 19f)
        lineTo(21f, 5f)
        arcTo(2f, 2f, 0f, false, false, 19f, 3f)
        close()
    }
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(8.5f, 10f)
        arcTo(1.5f, 1.5f, 0f, true, false, 8.5f, 7f)
        arcTo(1.5f, 1.5f, 0f, false, false, 8.5f, 10f)
        close()
    }
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(21f, 15f)
        lineTo(16f, 10f)
        lineTo(5f, 21f)
    }
}.build()

val MultiAgentPipelineIcon = ImageVector.Builder(
    name = "MultiAgentPipeline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Primary),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        lineTo(20f, 7f)
        lineTo(20f, 17f)
        lineTo(12f, 22f)
        lineTo(4f, 17f)
        lineTo(4f, 7f)
        close()
        moveTo(12f, 6f)
        lineTo(12f, 18f)
        moveTo(6f, 9f)
        lineTo(18f, 15f)
        moveTo(18f, 9f)
        lineTo(6f, 15f)
    }
}.build()

val CopyVectorIcon = ImageVector.Builder(
    name = "CopyOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(16f, 1f)
        lineTo(4f, 1f)
        arcTo(2f, 2f, 0f, false, false, 2f, 3f)
        lineTo(2f, 15f)
        moveTo(8f, 5f)
        lineTo(20f, 5f)
        arcTo(2f, 2f, 0f, false, true, 22f, 7f)
        lineTo(22f, 21f)
        arcTo(2f, 2f, 0f, false, true, 20f, 23f)
        lineTo(8f, 23f)
        arcTo(2f, 2f, 0f, false, true, 6f, 21f)
        lineTo(6f, 7f)
        arcTo(2f, 2f, 0f, false, true, 8f, 5f)
        close()
    }
}.build()

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Lipi.Primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Rx", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Text("LipiRx", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
        }
        trailing()
    }
}

@Composable
fun ModelChip(
    selectedModel: String,
    apiKey: String = "",
    geminiApiKey: String = "",
    groqApiKey: String = "",
    openaiApiKey: String = "",
    customApiKey: String = "",
    providerType: String = "gemini",
    customUrl: String = "",
    apiKeyError: String? = null,
    isTestingApiKey: Boolean = false,
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> }
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { showSheet = true },
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Lipi.Success, CircleShape)
            )
            Text(
                text = "⚡ AI Pipeline (4)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
                maxLines = 1
            )
        }
    }

    if (showSheet) {
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
            isTestingApiKey = isTestingApiKey,
            agentConfigs = agentConfigs,
            providerModelsMap = providerModelsMap,
            onAgentProviderSelected = onAgentProviderSelected,
            onAgentModelSelected = onAgentModelSelected,
            onProviderTabSelected = onProviderTabSelected,
            onSaveProviderConfig = onSaveProviderConfig,
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
    selectedModel: String,
    apiKey: String = "",
    geminiApiKey: String = "",
    groqApiKey: String = "",
    openaiApiKey: String = "",
    customApiKey: String = "",
    providerType: String = "gemini",
    customUrl: String = "",
    apiKeyError: String? = null,
    isTestingApiKey: Boolean = false,
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val keyForSelectedProvider = when (providerType) {
        "groq" -> groqApiKey.ifBlank { apiKey }
        "openai" -> openaiApiKey.ifBlank { apiKey }
        "custom" -> customApiKey.ifBlank { apiKey }
        else -> geminiApiKey.ifBlank { apiKey }
    }

    var inputKey by remember(providerType, keyForSelectedProvider) { mutableStateOf(keyForSelectedProvider) }
    var inputUrl by remember(customUrl) { mutableStateOf(customUrl) }
    var inputCustomModel by remember(selectedModel) { mutableStateOf(if (providerType == "custom") selectedModel else "") }

    val displayKeyText = remember(inputKey) { formatMaskedApiKey(inputKey) }

    fun dismissWithAnimation(onComplete: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
                onComplete()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { dismissWithAnimation() },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val activeProviderObj = AiProvider.fromId(providerType)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Lipi.PrimaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = MultiAgentPipelineIcon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Lipi.Primary
                        )
                    }
                    Column {
                        Text("AI Pipeline & Engine Studio", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
                        Text("Assign specialized AI models per agent & configure keys", fontSize = 12.sp, color = Lipi.Muted)
                    }
                }
            }

            // --- TOP SECTION: Per-Agent AI Assignments ---
            Text(
                text = "Per-Agent AI & Model Assignments",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink
            )

            RxAgentType.ALL.forEach { agentType ->
                val currentConfig = agentConfigs[agentType]
                val currentAssignedProvider = currentConfig?.provider ?: agentType.defaultProvider
                val currentAssignedModel = currentConfig?.selectedModel?.ifBlank { "" } ?: ""

                // Strictly live models for THIS agent's assigned provider from providerModelsMap
                val liveModelsForProvider = providerModelsMap[currentAssignedProvider.id] ?: emptyList()

                var showModelDropdown by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Lipi.Background,
                    border = BorderStroke(1.dp, Lipi.Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Agent Title & Description
                        Text(
                            text = agentType.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Ink
                        )
                        Text(
                            text = agentType.description,
                            fontSize = 11.sp,
                            color = Lipi.Muted
                        )

                        // 2. Provider Selector Chips Row
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AiProvider.ALL.forEach { prov ->
                                val isSelected = currentAssignedProvider == prov
                                Surface(
                                    onClick = {
                                        onAgentProviderSelected(agentType, prov)
                                        val modelsForProv = providerModelsMap[prov.id] ?: emptyList()
                                        if (modelsForProv.isNotEmpty()) {
                                            onAgentModelSelected(agentType, modelsForProv.first())
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (isSelected) prov.brandColor else Color.White,
                                    border = BorderStroke(1.dp, if (isSelected) prov.brandColor else Lipi.Outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = prov.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (isSelected) Color.White else prov.brandColor
                                        )
                                        Text(
                                            text = prov.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else Lipi.Ink
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Model Dropdown Selector Bar BELOW Provider Row
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { showModelDropdown = !showModelDropdown },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (liveModelsForProvider.isNotEmpty()) currentAssignedProvider.brandColor.copy(alpha = 0.5f) else Lipi.Error
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Model:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Lipi.Muted
                                        )
                                        Text(
                                            text = when {
                                                liveModelsForProvider.isNotEmpty() && currentAssignedModel.isNotBlank() -> currentAssignedModel
                                                liveModelsForProvider.isNotEmpty() -> liveModelsForProvider.first()
                                                else -> "Configure API key to load models"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (liveModelsForProvider.isNotEmpty()) currentAssignedProvider.brandColor else Lipi.Error
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Model",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (liveModelsForProvider.isNotEmpty()) currentAssignedProvider.brandColor else Lipi.Error
                                    )
                                }
                            }

                            // Native Material3 DropdownMenu listing strictly live provider models (max ~5 items visible height)
                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .heightIn(max = 220.dp)
                                    .background(Color.White)
                            ) {
                                if (liveModelsForProvider.isNotEmpty()) {
                                    liveModelsForProvider.forEach { mName ->
                                        val isModelSelected = currentAssignedModel == mName
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = mName,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isModelSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isModelSelected) currentAssignedProvider.brandColor else Lipi.Ink
                                                    )
                                                    if (isModelSelected) {
                                                        Text(
                                                            text = "✓",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = currentAssignedProvider.brandColor
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                onAgentModelSelected(agentType, mName)
                                                showModelDropdown = false
                                            }
                                        )
                                    }
                                } else {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "No live models found for ${currentAssignedProvider.displayName}.\nTap '🔑 Save Provider Config' below to verify key & fetch models.",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Lipi.Error
                                            )
                                        },
                                        onClick = {
                                            showModelDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- SECOND SECTION: Global Provider Config & API Keys ---
            Text(
                text = "Provider Keys & Settings",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink
            )

            // Provider Selector Tabs
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiProvider.ALL.forEach { providerItem ->
                    val isSelected = providerType == providerItem.id
                    Surface(
                        onClick = { onProviderTabSelected(providerItem.id) },
                        shape = CircleShape,
                        color = if (isSelected) providerItem.brandColor else Lipi.Background,
                        border = BorderStroke(1.dp, if (isSelected) providerItem.brandColor else Lipi.Outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = providerItem.icon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isSelected) Color.White else providerItem.brandColor
                            )
                            Text(
                                text = providerItem.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Lipi.Ink
                            )
                        }
                    }
                }
            }

            // Provider Input Config Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Lipi.Background,
                border = BorderStroke(1.dp, Lipi.Outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (providerType == "custom") {
                        Text("Custom Endpoint URL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. http://192.168.1.50:11434/v1/chat/completions", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Text("Custom Model Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                        OutlinedTextField(
                            value = inputCustomModel,
                            onValueChange = { inputCustomModel = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. llava or gpt-4o", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Text("API Key", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = displayKeyText,
                            onValueChange = {}, // Uneditable
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tap 'Paste' to insert key...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        SecondaryButton(
                            text = "Paste",
                            icon = null,
                            onClick = {
                                val text = clipboardManager.getText()?.text?.trim() ?: ""
                                if (text.isNotBlank()) inputKey = text
                            },
                            height = 50.dp,
                            fontSize = 13
                        )
                    }

                    if (!apiKeyError.isNullOrBlank()) {
                        Text(
                            text = apiKeyError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Error
                        )
                    }

                    Button(
                        onClick = {
                            onSaveProviderConfig(
                                providerType,
                                inputUrl,
                                inputKey,
                                inputCustomModel
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isTestingApiKey,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeProviderObj.brandColor,
                            contentColor = Color.White
                        )
                    ) {
                        if (isTestingApiKey) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connecting & Saving...", fontSize = 13.sp)
                        } else {
                            Text("🔑 Save Provider Config", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun formatMaskedApiKey(key: String): String {
    val trimmed = key.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.length <= 8) return "*".repeat(trimmed.length)
    val firstFour = trimmed.take(4)
    val lastFour = trimmed.takeLast(4)
    val middleMask = "*".repeat((trimmed.length - 8).coerceAtLeast(4))
    return "$firstFour$middleMask$lastFour"
}

@Composable
fun InfoChip(text: String) {
    Surface(
        shape = CircleShape,
        color = Lipi.PrimaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Lipi.Primary,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lipi.Primary, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 58.dp,
    contentColor: Color = Lipi.Ink,
    fontSize: Int = 16,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Lipi.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** Decorative prescription sheet used in the home hero and as a loading placeholder. */
@Composable
fun PaperIllustration(
    width: Dp,
    height: Dp,
    rotation: Float = -5f,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .size(width, height)
            .rotate(rotation)
            .shadow(20.dp, shape, ambientColor = Lipi.Ink.copy(0.15f), spotColor = Lipi.Ink.copy(0.2f))
            .background(Color.White, shape)
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("Rx", color = Lipi.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(6.dp))
        listOf(0.85f, 0.65f, 0.76f, 0.52f, 0.8f, 0.58f).forEach { w ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(w)
                    .height(6.dp)
                    .background(Lipi.InkLine, CircleShape)
            )
        }
    }
}
