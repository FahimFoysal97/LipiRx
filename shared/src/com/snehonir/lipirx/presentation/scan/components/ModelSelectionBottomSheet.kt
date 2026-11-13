package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.ui.theme.ApiKeyVectorIcon
import com.snehonir.lipirx.ui.theme.CustomUrlVectorIcon
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.McpEndpointVectorIcon
import com.snehonir.lipirx.ui.theme.McpServerVectorIcon
import com.snehonir.lipirx.ui.theme.OnDeviceStrategyVectorIcon
import com.snehonir.lipirx.ui.theme.OnDeviceVectorIcon
import com.snehonir.lipirx.ui.theme.PipelineMatrixVectorIcon
import com.snehonir.lipirx.ui.theme.ProviderSelectVectorIcon
import com.snehonir.lipirx.ui.theme.ProviderSettingsVectorIcon
import kotlinx.coroutines.launch

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
    apiKeySuccess: String? = null,
    isTestingApiKey: Boolean = false,
    onDeviceStrategy: OnDeviceStrategy = OnDeviceStrategy.HYBRID_FALLBACK,
    isMcpServerEnabled: Boolean = false,
    onToggleMcpServer: (Boolean) -> Unit = {},
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onSelectOnDeviceStrategy: (OnDeviceStrategy) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var selectedTabProvider by remember(providerType) { mutableStateOf(providerType) }

    val keyForSelectedProvider = when (selectedTabProvider) {
        "groq" -> groqApiKey.ifBlank { apiKey }
        "openai" -> openaiApiKey.ifBlank { apiKey }
        "custom" -> customApiKey.ifBlank { apiKey }
        else -> geminiApiKey.ifBlank { apiKey }
    }

    var inputKey by remember(selectedTabProvider, keyForSelectedProvider) { mutableStateOf(keyForSelectedProvider) }
    var inputUrl by remember(customUrl) { mutableStateOf(customUrl) }
    var inputCustomModel by remember(selectedModel) { mutableStateOf(if (selectedTabProvider == "custom") selectedModel else "") }

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
                        Text("AI Engine & MCP Studio", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
                        Text("Configure multi-agent pipeline, MCP server & provider keys", fontSize = 12.sp, color = Lipi.Muted)
                    }
                }
            }

            // --- ON-DEVICE AI EXECUTION STRATEGY ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = OnDeviceStrategyVectorIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Lipi.Primary
                )
                Text(
                    text = "On-Device AI & Fallback Strategy",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )
            }

            var showStrategyDropdown by remember { mutableStateOf(false) }
            val strategyScrollState = rememberScrollState()
            var selectedStrategyY by remember(showStrategyDropdown) { mutableStateOf<Int?>(null) }

            LaunchedEffect(showStrategyDropdown, selectedStrategyY) {
                val y = selectedStrategyY
                if (showStrategyDropdown && y != null) {
                    strategyScrollState.scrollTo(y.coerceAtMost(strategyScrollState.maxValue))
                }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { showStrategyDropdown = !showStrategyDropdown },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Lipi.Background,
                    border = BorderStroke(1.dp, Lipi.Outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = OnDeviceVectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Lipi.Primary
                            )
                            Text(
                                text = onDeviceStrategy.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Ink
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Strategy",
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Muted
                        )
                    }
                }

                DropdownMenu(
                    expanded = showStrategyDropdown,
                    onDismissRequest = { showStrategyDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .verticalScroll(strategyScrollState)
                    ) {
                        OnDeviceStrategy.ALL.forEach { strat ->
                            val isSelected = onDeviceStrategy == strat
                            DropdownMenuItem(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    if (isSelected && selectedStrategyY == null) {
                                        selectedStrategyY = coordinates.positionInParent().y.toInt()
                                    }
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = strat.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Lipi.Primary else Lipi.Ink
                                        )
                                        Text(
                                            text = strat.description,
                                            fontSize = 11.sp,
                                            color = Lipi.Muted
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectOnDeviceStrategy(strat)
                                    showStrategyDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // --- MODEL CONTEXT PROTOCOL (MCP) SERVER CONTROL ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = McpServerVectorIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Lipi.Primary
                )
                Text(
                    text = "Model Context Protocol (MCP)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )
            }

            var localMcpState by remember(isMcpServerEnabled) { mutableStateOf(isMcpServerEnabled) }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Lipi.Background,
                border = BorderStroke(1.dp, Lipi.Outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = McpEndpointVectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Lipi.Primary
                            )
                            Text(
                                text = "Local Server Endpoint",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Ink
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (localMcpState) {
                                "🟢 Server running at http://localhost:8080/mcp"
                            } else {
                                "Exposes local OCR tools & prescription history for desktop tools & POS software"
                            },
                            fontSize = 11.sp,
                            fontWeight = if (localMcpState) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (localMcpState) Lipi.StatusActiveGreen else Lipi.Muted
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = localMcpState,
                        onCheckedChange = { enabled ->
                            localMcpState = enabled
                            onToggleMcpServer(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Lipi.Primary,
                            uncheckedThumbColor = Lipi.SwitchOffThumb,
                            uncheckedTrackColor = Lipi.SwitchOffTrack,
                            uncheckedBorderColor = Lipi.SwitchOffBorder
                        )
                    )
                }
            }

            // --- PER-AGENT AI ASSIGNMENTS ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = PipelineMatrixVectorIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Lipi.Primary
                )
                Text(
                    text = "Per-Agent AI & Model Assignments",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )
            }

            RxAgentType.ALL.forEach { agentType ->
                val currentConfig = agentConfigs[agentType]
                val currentAssignedProvider = currentConfig?.provider ?: agentType.defaultProvider
                val currentAssignedModel = currentConfig?.selectedModel?.ifBlank { "" } ?: ""

                val liveModelsForProvider = providerModelsMap[currentAssignedProvider.id] ?: emptyList()

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = agentType.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Lipi.Primary
                            )
                            Text(
                                text = agentType.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Ink
                            )
                        }
                        Text(
                            text = agentType.description,
                            fontSize = 11.sp,
                            color = Lipi.Muted
                        )

                        var showProviderDropdown by remember { mutableStateOf(false) }
                        var showModelDropdown by remember { mutableStateOf(false) }

                        val providerScrollState = rememberScrollState()
                        var selectedProviderY by remember(showProviderDropdown) { mutableStateOf<Int?>(null) }

                        LaunchedEffect(showProviderDropdown, selectedProviderY) {
                            val y = selectedProviderY
                            if (showProviderDropdown && y != null) {
                                providerScrollState.scrollTo(y.coerceAtMost(providerScrollState.maxValue))
                            }
                        }

                        val modelScrollState = rememberScrollState()
                        var selectedModelY by remember(showModelDropdown) { mutableStateOf<Int?>(null) }

                        LaunchedEffect(showModelDropdown, selectedModelY) {
                            val y = selectedModelY
                            if (showModelDropdown && y != null) {
                                modelScrollState.scrollTo(y.coerceAtMost(modelScrollState.maxValue))
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { showProviderDropdown = !showProviderDropdown },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, currentAssignedProvider.brandColor.copy(alpha = 0.5f))
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
                                            text = "Provider:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Lipi.Muted
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = currentAssignedProvider.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = currentAssignedProvider.brandColor
                                            )
                                            Text(
                                                text = currentAssignedProvider.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = currentAssignedProvider.brandColor
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Provider",
                                        modifier = Modifier.size(20.dp),
                                        tint = currentAssignedProvider.brandColor
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showProviderDropdown,
                                onDismissRequest = { showProviderDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .verticalScroll(providerScrollState)
                                ) {
                                    AiProvider.ALL.forEach { prov ->
                                        val isSelected = currentAssignedProvider == prov
                                        val isProviderDisabled = (onDeviceStrategy == OnDeviceStrategy.CLOUD_ONLY && prov == AiProvider.OnDevice) ||
                                                (onDeviceStrategy == OnDeviceStrategy.ON_DEVICE_ONLY && prov != AiProvider.OnDevice)

                                        DropdownMenuItem(
                                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                                if (isSelected && selectedProviderY == null) {
                                                    selectedProviderY = coordinates.positionInParent().y.toInt()
                                                }
                                            },
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = prov.icon,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp),
                                                            tint = if (isProviderDisabled) Lipi.Muted.copy(alpha = 0.4f) else prov.brandColor
                                                        )
                                                        Text(
                                                            text = if (isProviderDisabled) "${prov.displayName} (Locked by Strategy)" else prov.displayName,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isProviderDisabled) Lipi.Muted.copy(alpha = 0.4f) else if (isSelected) prov.brandColor else Lipi.Ink
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Text(
                                                            text = "✓",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = prov.brandColor
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                if (!isProviderDisabled) {
                                                    onAgentProviderSelected(agentType, prov)
                                                    showProviderDropdown = false
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

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
                                                currentAssignedModel.isNotBlank() -> currentAssignedModel
                                                liveModelsForProvider.isNotEmpty() -> liveModelsForProvider.first()
                                                currentAssignedProvider == AiProvider.OnDevice -> "No .litertlm model in device storage"
                                                else -> "Configure API key to load models"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (currentAssignedModel.isNotBlank() || liveModelsForProvider.isNotEmpty()) currentAssignedProvider.brandColor else Lipi.Error
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

                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(Color.White)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .verticalScroll(modelScrollState)
                                ) {
                                    if (liveModelsForProvider.isNotEmpty()) {
                                        liveModelsForProvider.forEachIndexed { idx, mName ->
                                            val isModelSelected = if (currentAssignedModel.isNotBlank()) currentAssignedModel == mName else idx == 0
                                            DropdownMenuItem(
                                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                                    if (isModelSelected && selectedModelY == null) {
                                                        selectedModelY = coordinates.positionInParent().y.toInt()
                                                    }
                                                },
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
                                                    text = if (currentAssignedProvider == AiProvider.OnDevice) {
                                                        "No local models found on device storage.\nPush .litertlm file via adb to filesDir."
                                                    } else {
                                                        "No live models found for ${currentAssignedProvider.displayName}.\nTap '🔑 Save Provider Config' below to verify key & fetch models."
                                                    },
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
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = ProviderSettingsVectorIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Lipi.Primary
                )
                Text(
                    text = "Provider Settings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )
            }

            val activeCardProvider = AiProvider.fromId(selectedTabProvider)

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = ProviderSelectVectorIcon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Lipi.Primary
                        )
                        Text("Select Provider", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                    }

                    var showCardProviderDropdown by remember { mutableStateOf(false) }
                    val cardProviderScrollState = rememberScrollState()

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            onClick = { showCardProviderDropdown = !showCardProviderDropdown },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, activeCardProvider.brandColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = activeCardProvider.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = activeCardProvider.brandColor
                                    )
                                    Text(
                                        text = activeCardProvider.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeCardProvider.brandColor
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Provider",
                                    modifier = Modifier.size(20.dp),
                                    tint = activeCardProvider.brandColor
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCardProviderDropdown,
                            onDismissRequest = { showCardProviderDropdown = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .background(Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(cardProviderScrollState)
                            ) {
                                AiProvider.ALL.forEach { prov ->
                                    val isSelected = selectedTabProvider == prov.id
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = prov.icon,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = prov.brandColor
                                                    )
                                                    Text(
                                                        text = prov.displayName,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) prov.brandColor else Lipi.Ink
                                                    )
                                                }
                                                if (isSelected) {
                                                    Text(
                                                        text = "✓",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = prov.brandColor
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedTabProvider = prov.id
                                            onProviderTabSelected(prov.id)
                                            showCardProviderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedTabProvider == "custom") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = CustomUrlVectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Lipi.Primary
                            )
                            Text("Custom Endpoint URL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                        }
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. http://192.168.1.50:11434/v1/chat/completions", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = CustomUrlVectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Lipi.Primary
                            )
                            Text("Custom Model Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                        }
                        OutlinedTextField(
                            value = inputCustomModel,
                            onValueChange = { inputCustomModel = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. llava or gpt-4o", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (selectedTabProvider == "on_device") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Lipi.PrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = OnDeviceVectorIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Lipi.Primary
                                )
                                Text(
                                    text = "On-Device SLM runs locally on your device hardware without requiring an API key.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Lipi.OnPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ApiKeyVectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Lipi.Primary
                            )
                            Text("API Key", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lipi.Ink)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = displayKeyText,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Tap 'Paste' to insert key...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Surface(
                                onClick = {
                                    val text = clipboardManager.getText()?.text?.trim() ?: ""
                                    if (text.isNotBlank()) inputKey = text
                                },
                                modifier = Modifier.size(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Lipi.PrimaryContainer,
                                border = BorderStroke(1.dp, Lipi.Primary.copy(alpha = 0.3f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = PasteVectorIcon,
                                        contentDescription = "Paste API Key from Clipboard",
                                        modifier = Modifier.size(22.dp),
                                        tint = Lipi.Primary
                                    )
                                }
                            }
                        }

                        if (!apiKeyError.isNullOrBlank()) {
                            Text(
                                text = apiKeyError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Lipi.Error
                            )
                        }

                        if (!apiKeySuccess.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Lipi.PrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Lipi.Primary
                                    )
                                    Text(
                                        text = apiKeySuccess,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Lipi.OnPrimaryContainer
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onSaveProviderConfig(
                                    selectedTabProvider,
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
                                containerColor = Lipi.Primary,
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
