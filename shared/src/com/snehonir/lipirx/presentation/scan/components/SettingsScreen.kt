package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_connecting_saving
import com.snehonir.lipirx.resources.btn_save_config
import com.snehonir.lipirx.resources.btn_view_telemetry
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.configure_key_to_load_models
import com.snehonir.lipirx.resources.content_desc_clear_api_key
import com.snehonir.lipirx.resources.content_desc_paste_api_key
import com.snehonir.lipirx.resources.content_desc_select_fallback_model
import com.snehonir.lipirx.resources.content_desc_select_fallback_provider
import com.snehonir.lipirx.resources.content_desc_select_model
import com.snehonir.lipirx.resources.content_desc_select_provider
import com.snehonir.lipirx.resources.content_desc_select_strategy
import com.snehonir.lipirx.resources.content_desc_selected
import com.snehonir.lipirx.resources.label_api_key
import com.snehonir.lipirx.resources.label_api_key_saved
import com.snehonir.lipirx.resources.label_custom_model
import com.snehonir.lipirx.resources.label_custom_url
import com.snehonir.lipirx.resources.label_escalation_badge
import com.snehonir.lipirx.resources.label_escalation_provider
import com.snehonir.lipirx.resources.label_fallback_badge
import com.snehonir.lipirx.resources.label_fallback_model
import com.snehonir.lipirx.resources.label_fallback_provider
import com.snehonir.lipirx.resources.label_locked_by_strategy
import com.snehonir.lipirx.resources.label_model
import com.snehonir.lipirx.resources.label_no_api_key_saved
import com.snehonir.lipirx.resources.label_provider
import com.snehonir.lipirx.resources.label_select_provider
import com.snehonir.lipirx.resources.mcp_server_description
import com.snehonir.lipirx.resources.mcp_server_running
import com.snehonir.lipirx.resources.no_live_models_found
import com.snehonir.lipirx.resources.no_local_models_found
import com.snehonir.lipirx.resources.on_device_info_banner
import com.snehonir.lipirx.resources.placeholder_custom_model_example
import com.snehonir.lipirx.resources.placeholder_custom_url_example
import com.snehonir.lipirx.resources.placeholder_paste_key
import com.snehonir.lipirx.resources.section_appearance_theme
import com.snehonir.lipirx.resources.section_mcp
import com.snehonir.lipirx.resources.section_mcp_endpoint
import com.snehonir.lipirx.resources.section_on_device
import com.snehonir.lipirx.resources.section_per_agent_assignments
import com.snehonir.lipirx.resources.section_provider_settings
import com.snehonir.lipirx.resources.studio_subtitle
import com.snehonir.lipirx.resources.studio_title
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.ApiKeyVectorIcon
import com.snehonir.lipirx.ui.theme.icons.CustomUrlVectorIcon
import com.snehonir.lipirx.ui.theme.icons.DarkThemeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.LightThemeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.McpEndpointVectorIcon
import com.snehonir.lipirx.ui.theme.icons.McpServerVectorIcon
import com.snehonir.lipirx.ui.theme.icons.OnDeviceStrategyVectorIcon
import com.snehonir.lipirx.ui.theme.icons.PasteVectorIcon
import com.snehonir.lipirx.ui.theme.icons.PipelineMatrixVectorIcon
import com.snehonir.lipirx.ui.theme.icons.ProviderSettingsVectorIcon
import com.snehonir.lipirx.ui.theme.icons.SystemThemeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.icon
import org.jetbrains.compose.resources.stringResource

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// Order: Appearance → AI execution → Provider & key → Agents → Integrations.
// Things people touch most / must do first come first; advanced stuff is last.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    selectedModel: String,
    apiKey: String = "",
    geminiApiKey: String = "",
    groqApiKey: String = "",
    openaiApiKey: String = "",
    claudeApiKey: String = "",
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
    onAgentFallbackProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentFallbackModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onSelectOnDeviceStrategy: (OnDeviceStrategy) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onViewTelemetry: (() -> Unit)? = null,
    onClose: () -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var selectedTabProvider by remember(providerType) { mutableStateOf(providerType) }

    val keyForSelectedProvider = when (selectedTabProvider) {
        "groq" -> groqApiKey.ifBlank { apiKey }
        "openai" -> openaiApiKey.ifBlank { apiKey }
        "claude" -> claudeApiKey.ifBlank { apiKey }
        "custom" -> customApiKey.ifBlank { apiKey }
        else -> geminiApiKey.ifBlank { apiKey }
    }

    var inputKey by remember(selectedTabProvider, keyForSelectedProvider) { mutableStateOf(keyForSelectedProvider) }
    var inputUrl by remember(customUrl) { mutableStateOf(customUrl) }
    var inputCustomModel by remember(selectedModel) { mutableStateOf(if (selectedTabProvider == "custom") selectedModel else "") }

    val displayKeyText = remember(inputKey) { formatMaskedApiKey(inputKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AppHeader(
            onBack = onClose,
            backContentDescription = stringResource(Res.string.btn_cancel),
            title = stringResource(Res.string.studio_title),
            subtitle = stringResource(Res.string.studio_subtitle)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── 1. APPEARANCE ────────────────────────────────────────────────
            SettingsSection(
                icon = SystemThemeVectorIcon,
                title = stringResource(Res.string.section_appearance_theme)
            ) {
                ThemeModeSelector(themeMode = themeMode, onSelect = onSelectThemeMode)
            }

            // ── 2. AI EXECUTION STRATEGY ─────────────────────────────────────
            SettingsSection(
                icon = OnDeviceStrategyVectorIcon,
                title = stringResource(Res.string.section_on_device)
            ) {
                SettingsCard {
                    PickerField(
                        valueText = stringResource(onDeviceStrategy.displayNameRes),
                        accent = Lipi.Primary,
                        leadingIcon = onDeviceStrategy.icon,
                        contentDescription = stringResource(Res.string.content_desc_select_strategy),
                        options = OnDeviceStrategy.ALL.map { strat ->
                            MenuOption(
                                title = stringResource(strat.displayNameRes),
                                subtitle = stringResource(strat.descriptionRes),
                                icon = strat.icon,
                                tint = Lipi.Primary,
                                selected = onDeviceStrategy == strat,
                                onClick = { onSelectOnDeviceStrategy(strat) }
                            )
                        }
                    )
                    InfoNote(text = stringResource(onDeviceStrategy.explanationRes))
                }
            }

            // ── 3. PROVIDER & API KEY ────────────────────────────────────────
            SettingsSection(
                icon = ProviderSettingsVectorIcon,
                title = stringResource(Res.string.section_provider_settings)
            ) {
                val activeCardProvider = AiProvider.fromId(selectedTabProvider)

                SettingsCard {
                    PickerField(
                        label = stringResource(Res.string.label_select_provider),
                        valueText = activeCardProvider.displayName,
                        accent = activeCardProvider.brandColor,
                        leadingIcon = activeCardProvider.icon,
                        contentDescription = stringResource(Res.string.label_select_provider),
                        options = providerOptions(
                            selectedId = selectedTabProvider,
                            onPick = { prov ->
                                selectedTabProvider = prov.id
                                onProviderTabSelected(prov.id)
                            }
                        )
                    )

                    if (selectedTabProvider == "custom") {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.label_custom_url)) },
                            placeholder = { Text(stringResource(Res.string.placeholder_custom_url_example), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(CustomUrlVectorIcon, null, Modifier.size(20.dp), tint = Lipi.Primary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = inputCustomModel,
                            onValueChange = { inputCustomModel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.label_custom_model)) },
                            placeholder = { Text(stringResource(Res.string.placeholder_custom_model_example), fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(CustomUrlVectorIcon, null, Modifier.size(20.dp), tint = Lipi.Primary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (selectedTabProvider == "on_device") {
                        InfoNote(
                            text = stringResource(Res.string.on_device_info_banner),
                            icon = Icons.Default.Info
                        )
                    } else {
                        // API key: masked, with Paste / Clear actions.
                        OutlinedTextField(
                            value = displayKeyText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.label_api_key)) },
                            placeholder = {
                                Text(stringResource(Res.string.placeholder_paste_key), fontSize = 12.sp)
                            },
                            leadingIcon = {
                                Icon(ApiKeyVectorIcon, null, Modifier.size(20.dp), tint = Lipi.Primary)
                            },
                            trailingIcon = {
                                Row {
                                    if (inputKey.isNotBlank()) {
                                        IconButton(onClick = { inputKey = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(Res.string.content_desc_clear_api_key),
                                                modifier = Modifier.size(20.dp),
                                                tint = Lipi.Muted
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val text = clipboardManager.getText()?.text?.trim() ?: ""
                                            if (text.isNotBlank()) inputKey = text
                                        }
                                    ) {
                                        Icon(
                                            imageVector = PasteVectorIcon,
                                            contentDescription = stringResource(Res.string.content_desc_paste_api_key),
                                            modifier = Modifier.size(22.dp),
                                            tint = Lipi.Primary
                                        )
                                    }
                                }
                            },
                            isError = !apiKeyError.isNullOrBlank(),
                            supportingText = if (!apiKeyError.isNullOrBlank()) {
                                { Text(apiKeyError) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        StatusLine(
                            ok = keyForSelectedProvider.isNotBlank(),
                            text = if (keyForSelectedProvider.isNotBlank()) {
                                stringResource(Res.string.label_api_key_saved)
                            } else {
                                stringResource(Res.string.label_no_api_key_saved)
                            }
                        )

                        if (!apiKeySuccess.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Lipi.PrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        fontWeight = FontWeight.SemiBold,
                                        color = Lipi.OnPrimaryContainer
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onSaveProviderConfig(selectedTabProvider, inputUrl, inputKey, inputCustomModel)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isTestingApiKey,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Lipi.Primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isTestingApiKey) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(Res.string.btn_connecting_saving), fontSize = 14.sp)
                            } else {
                                Text(
                                    stringResource(Res.string.btn_save_config),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── 4. PER-AGENT ASSIGNMENTS (collapsible cards) ─────────────────
            SettingsSection(
                icon = PipelineMatrixVectorIcon,
                title = stringResource(Res.string.section_per_agent_assignments)
            ) {
                RxAgentType.ALL.forEach { agentType ->
                    AgentAssignmentCard(
                        agentType = agentType,
                        config = agentConfigs[agentType],
                        providerModelsMap = providerModelsMap,
                        onDeviceStrategy = onDeviceStrategy,
                        onProviderSelected = { onAgentProviderSelected(agentType, it) },
                        onModelSelected = { onAgentModelSelected(agentType, it) },
                        onFallbackProviderSelected = { onAgentFallbackProviderSelected(agentType, it) },
                        onFallbackModelSelected = { onAgentFallbackModelSelected(agentType, it) }
                    )
                }
            }

            // ── 5. INTEGRATIONS / DEVELOPER ──────────────────────────────────
            SettingsSection(
                icon = McpServerVectorIcon,
                title = stringResource(Res.string.section_mcp)
            ) {
                var localMcpState by remember(isMcpServerEnabled) { mutableStateOf(isMcpServerEnabled) }

                SettingsCard(contentPadding = 0.dp, spacing = 0.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(icon = McpEndpointVectorIcon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.section_mcp_endpoint),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Lipi.Ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (localMcpState) stringResource(Res.string.mcp_server_running)
                                else stringResource(Res.string.mcp_server_description),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
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

                    if (onViewTelemetry != null) {
                        Divider()
                        Surface(
                            onClick = onViewTelemetry,
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconBadge(icon = PipelineMatrixVectorIcon)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(Res.string.btn_view_telemetry),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Lipi.Ink,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = Lipi.Muted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Agent card: collapsed = one-line summary, expanded = provider/model/fallback.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AgentAssignmentCard(
    agentType: RxAgentType,
    config: AgentConfig?,
    providerModelsMap: Map<String, List<String>>,
    onDeviceStrategy: OnDeviceStrategy,
    onProviderSelected: (AiProvider) -> Unit,
    onModelSelected: (String) -> Unit,
    onFallbackProviderSelected: (AiProvider) -> Unit,
    onFallbackModelSelected: (String) -> Unit,
) {
    var expanded by remember(agentType) { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)

    val provider = config?.provider ?: agentType.defaultProvider
    val assignedModel = config?.selectedModel?.ifBlank { "" } ?: ""
    val liveModels = providerModelsMap[provider.id] ?: emptyList()
    val hasProblem = liveModels.isEmpty() && assignedModel.isBlank()

    val rawModelText = when {
        assignedModel.isNotBlank() -> assignedModel
        liveModels.isNotEmpty() -> liveModels.first()
        provider == AiProvider.OnDevice -> stringResource(Res.string.no_local_models_found)
        else -> stringResource(Res.string.configure_key_to_load_models)
    }
    val modelText = if (provider == AiProvider.OnDevice) formatModelDisplayName(rawModelText) else rawModelText

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, if (expanded) Lipi.Primary.copy(alpha = 0.4f) else Lipi.CardBorder)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            // Header (always visible)
            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(icon = agentType.icon)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(agentType.displayNameRes),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Lipi.Ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${provider.displayName} · $modelText",
                            fontSize = 12.sp,
                            color = if (hasProblem) Lipi.Error else Lipi.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(if (expanded) Res.string.cd_collapse else Res.string.cd_expand),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowRotation),
                        tint = Lipi.Muted
                    )
                }
            }

            if (expanded) {
                Divider()
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(agentType.descriptionRes),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Lipi.Muted
                    )

                    PickerField(
                        label = stringResource(Res.string.label_provider),
                        valueText = provider.displayName,
                        accent = provider.brandColor,
                        leadingIcon = provider.icon,
                        contentDescription = stringResource(Res.string.content_desc_select_provider),
                        options = providerOptions(
                            selectedId = provider.id,
                            onPick = onProviderSelected,
                            isLocked = { prov ->
                                (onDeviceStrategy == OnDeviceStrategy.CLOUD_ONLY && prov == AiProvider.OnDevice) ||
                                        (onDeviceStrategy == OnDeviceStrategy.ON_DEVICE_ONLY && prov != AiProvider.OnDevice)
                            }
                        )
                    )

                    PickerField(
                        label = stringResource(Res.string.label_model),
                        valueText = modelText,
                        accent = provider.brandColor,
                        isError = hasProblem,
                        contentDescription = stringResource(Res.string.content_desc_select_model),
                        options = if (liveModels.isNotEmpty()) {
                            modelOptions(liveModels, assignedModel, provider.brandColor, onModelSelected)
                        } else {
                            listOf(
                                MenuOption(
                                    title = if (provider == AiProvider.OnDevice) {
                                        stringResource(Res.string.no_local_models_found)
                                    } else {
                                        stringResource(Res.string.no_live_models_found, provider.displayName)
                                    },
                                    tint = Lipi.Error,
                                    selected = false,
                                    onClick = {}
                                )
                            )
                        }
                    )

                    if (onDeviceStrategy == OnDeviceStrategy.HYBRID_FALLBACK ||
                        onDeviceStrategy == OnDeviceStrategy.ON_DEVICE_FIRST
                    ) {
                        val fallbackProvider = config?.fallbackProvider
                            ?: if (provider != AiProvider.OnDevice) AiProvider.OnDevice else AiProvider.Gemini
                        val fallbackModel = config?.fallbackModel?.ifBlank { "" } ?: ""
                        val fallbackModels = providerModelsMap[fallbackProvider.id] ?: emptyList()
                        val fallbackList = fallbackModels.ifEmpty { listOf(fallbackProvider.defaultModel) }
                        val rawFallbackModelText = when {
                            fallbackModel.isNotBlank() -> fallbackModel
                            else -> fallbackList.first()
                        }
                        val fallbackModelText = if (fallbackProvider == AiProvider.OnDevice) formatModelDisplayName(rawFallbackModelText) else rawFallbackModelText

                        Divider()
                        Text(
                            text = stringResource(if (provider != AiProvider.OnDevice) Res.string.label_fallback_badge else Res.string.label_escalation_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Lipi.Muted
                        )

                        PickerField(
                            label = stringResource(if (provider != AiProvider.OnDevice) Res.string.label_fallback_provider else Res.string.label_escalation_provider),
                            valueText = fallbackProvider.displayName,
                            accent = fallbackProvider.brandColor,
                            leadingIcon = fallbackProvider.icon,
                            contentDescription = stringResource(Res.string.content_desc_select_fallback_provider),
                            options = providerOptions(
                                selectedId = fallbackProvider.id,
                                onPick = onFallbackProviderSelected
                            )
                        )
                        PickerField(
                            label = stringResource(Res.string.label_fallback_model),
                            valueText = fallbackModelText,
                            accent = fallbackProvider.brandColor,
                            contentDescription = stringResource(Res.string.content_desc_select_fallback_model),
                            options = modelOptions(fallbackList, fallbackModel, fallbackProvider.brandColor, onFallbackModelSelected)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable building blocks
// ─────────────────────────────────────────────────────────────────────────────

private data class MenuOption(
    val title: String,
    val tint: Color,
    val selected: Boolean,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
)

@Composable
private fun providerOptions(
    selectedId: String,
    onPick: (AiProvider) -> Unit,
    isLocked: (AiProvider) -> Boolean = { false },
): List<MenuOption> {
    val lockedText = stringResource(Res.string.label_locked_by_strategy)
    return AiProvider.ALL.map { prov ->
        val locked = isLocked(prov)
        MenuOption(
            title = prov.displayName,
            subtitle = if (locked) lockedText else null,
            icon = prov.icon,
            tint = prov.brandColor,
            selected = prov.id == selectedId,
            enabled = !locked,
            onClick = { onPick(prov) }
        )
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

private fun modelOptions(
    models: List<String>,
    selected: String,
    tint: Color,
    onPick: (String) -> Unit,
): List<MenuOption> = models.mapIndexed { idx, rawName ->
    val displayTitle = formatModelDisplayName(rawName)
    MenuOption(
        title = displayTitle,
        tint = tint,
        // When nothing is explicitly chosen the UI shows the first model, so mark it as selected.
        selected = if (selected.isNotBlank()) selected == rawName || formatModelDisplayName(selected) == displayTitle else idx == 0,
        onClick = { onPick(rawName) }
    )
}

/** A tappable "field" that opens a menu. One implementation for every dropdown on the screen. */
@Composable
private fun PickerField(
    valueText: String,
    accent: Color,
    options: List<MenuOption>,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    contentDescription: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var selectedY by remember(expanded) { mutableStateOf<Int?>(null) }

    // Scroll the open menu so the selected item is visible.
    LaunchedEffect(expanded, selectedY) {
        val y = selectedY
        if (expanded && y != null) {
            scrollState.scrollTo(y.coerceAtMost(scrollState.maxValue))
        }
    }

    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
    val borderColor = when {
        isError -> Lipi.Error
        expanded -> accent
        else -> Lipi.Outline
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            color = Lipi.Background,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = accent
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (label != null) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Lipi.Muted
                        )
                    }
                    Text(
                        text = valueText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isError) Lipi.Error else Lipi.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(arrowRotation),
                    tint = if (isError) Lipi.Error else Lipi.Muted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Lipi.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(scrollState)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        modifier = if (option.selected) {
                            Modifier.onGloballyPositioned { coordinates ->
                                if (selectedY == null) selectedY = coordinates.positionInParent().y.toInt()
                            }
                        } else Modifier,
                        enabled = option.enabled,
                        leadingIcon = if (option.icon != null) {
                            {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (option.enabled) option.tint else option.tint.copy(alpha = 0.4f)
                                )
                            }
                        } else null,
                        trailingIcon = if (option.selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(Res.string.content_desc_selected),
                                    modifier = Modifier.size(16.dp),
                                    tint = option.tint
                                )
                            }
                        } else null,
                        text = {
                            Column {
                                Text(
                                    text = option.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (option.selected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        !option.enabled -> Lipi.Muted.copy(alpha = 0.6f)
                                        option.selected -> option.tint
                                        else -> Lipi.Ink
                                    }
                                )
                                if (option.subtitle != null) {
                                    Text(
                                        text = option.subtitle,
                                        fontSize = 11.sp,
                                        color = Lipi.Muted
                                    )
                                }
                            }
                        },
                        onClick = {
                            option.onClick()
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Lipi.Primary
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink
            )
        }
        content()
    }
}

@Composable
private fun SettingsCard(
    contentPadding: androidx.compose.ui.unit.Dp = 14.dp,
    spacing: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
}

@Composable
private fun ThemeModeSelector(
    themeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = themeMode == mode
                val modeIcon = when (mode) {
                    ThemeMode.SYSTEM -> SystemThemeVectorIcon
                    ThemeMode.DARK -> DarkThemeVectorIcon
                    ThemeMode.LIGHT -> LightThemeVectorIcon
                }
                Surface(
                    onClick = { onSelect(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Lipi.PrimaryContainer else Color.Transparent,
                    border = BorderStroke(1.dp, if (isSelected) Lipi.Primary else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Lipi.Primary else Lipi.Muted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.displayName,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Lipi.OnPrimaryContainer else Lipi.Muted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoNote(text: String, icon: ImageVector = Icons.Default.Info) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Lipi.PrimaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Lipi.Primary
            )
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Lipi.Ink
            )
        }
    }
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (ok) Lipi.StatusActiveGreen else Lipi.Muted
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (ok) Lipi.StatusActiveGreen else Lipi.Muted
        )
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Lipi.PrimaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Lipi.Primary
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Lipi.CardBorder)
    )
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