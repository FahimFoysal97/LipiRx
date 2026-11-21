@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.snehonir.lipirx.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.domain.update.UpdateState
import com.snehonir.lipirx.presentation.common.AppHeader
import com.snehonir.lipirx.presentation.common.SecondaryButton
import com.snehonir.lipirx.presentation.common.rememberFolderPickerLauncher
import com.snehonir.lipirx.presentation.update.UpdateContent
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.action_hide
import com.snehonir.lipirx.resources.action_show
import com.snehonir.lipirx.resources.app_name
import com.snehonir.lipirx.resources.btn_browse_folder
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_check_for_updates
import com.snehonir.lipirx.resources.btn_connecting_saving
import com.snehonir.lipirx.resources.btn_save_config
import com.snehonir.lipirx.resources.btn_view_telemetry
import com.snehonir.lipirx.resources.cd_collapse
import com.snehonir.lipirx.resources.cd_expand
import com.snehonir.lipirx.resources.configure_key_to_load_models
import com.snehonir.lipirx.resources.content_desc_clear_api_key
import com.snehonir.lipirx.resources.content_desc_select_fallback_model
import com.snehonir.lipirx.resources.content_desc_select_fallback_provider
import com.snehonir.lipirx.resources.content_desc_select_model
import com.snehonir.lipirx.resources.content_desc_select_provider
import com.snehonir.lipirx.resources.content_desc_select_strategy
import com.snehonir.lipirx.resources.content_desc_selected
import com.snehonir.lipirx.resources.default_slm_directory_name
import com.snehonir.lipirx.resources.dialog_discard_body
import com.snehonir.lipirx.resources.dialog_discard_confirm
import com.snehonir.lipirx.resources.dialog_discard_title
import com.snehonir.lipirx.resources.dialog_keep_editing
import com.snehonir.lipirx.resources.error_enter_valid_gemini_key
import com.snehonir.lipirx.resources.error_enter_valid_url
import com.snehonir.lipirx.resources.error_no_models_found
import com.snehonir.lipirx.resources.label_api_key
import com.snehonir.lipirx.resources.label_api_key_saved
import com.snehonir.lipirx.resources.label_app_version
import com.snehonir.lipirx.resources.label_custom_url
import com.snehonir.lipirx.resources.label_slm_directory
import com.snehonir.lipirx.resources.note_unsaved_changes
import com.snehonir.lipirx.resources.label_escalation_badge
import com.snehonir.lipirx.resources.label_escalation_provider
import com.snehonir.lipirx.resources.label_fallback_badge
import com.snehonir.lipirx.resources.label_fallback_model
import com.snehonir.lipirx.resources.label_fallback_provider
import com.snehonir.lipirx.resources.label_locked_by_strategy
import com.snehonir.lipirx.resources.label_model
import com.snehonir.lipirx.resources.label_models_available_count
import com.snehonir.lipirx.resources.label_no_api_key_saved
import com.snehonir.lipirx.resources.label_no_matches
import com.snehonir.lipirx.resources.label_no_models_loaded
import com.snehonir.lipirx.resources.label_provider
import com.snehonir.lipirx.resources.label_provider_models_count
import com.snehonir.lipirx.resources.label_select_provider
import com.snehonir.lipirx.resources.mcp_server_description
import com.snehonir.lipirx.resources.mcp_server_running
import com.snehonir.lipirx.resources.no_live_models_found
import com.snehonir.lipirx.resources.no_local_models_found
import com.snehonir.lipirx.resources.on_device_info_banner
import com.snehonir.lipirx.resources.placeholder_custom_url_example
import com.snehonir.lipirx.resources.placeholder_paste_key
import com.snehonir.lipirx.resources.placeholder_search
import com.snehonir.lipirx.resources.slm_models_found_count
import com.snehonir.lipirx.resources.status_add_api_key_detail
import com.snehonir.lipirx.resources.status_agents_no_model_detail
import com.snehonir.lipirx.resources.status_fix_action
import com.snehonir.lipirx.resources.status_needs_attention
import com.snehonir.lipirx.resources.status_ready_detail
import com.snehonir.lipirx.resources.status_ready_to_scan
import com.snehonir.lipirx.resources.status_setup_needed
import com.snehonir.lipirx.resources.section_about
import com.snehonir.lipirx.resources.section_appearance_theme
import com.snehonir.lipirx.resources.section_mcp
import com.snehonir.lipirx.resources.section_mcp_endpoint
import com.snehonir.lipirx.resources.section_on_device
import com.snehonir.lipirx.resources.section_per_agent_assignments
import com.snehonir.lipirx.resources.section_provider_settings
import com.snehonir.lipirx.resources.studio_subtitle
import com.snehonir.lipirx.resources.studio_title
import com.snehonir.lipirx.resources.theme_dark
import com.snehonir.lipirx.resources.theme_light
import com.snehonir.lipirx.resources.theme_system_default
import com.snehonir.lipirx.resources.update_checking
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.icons.ApiKeyVectorIcon
import com.snehonir.lipirx.ui.theme.icons.CustomUrlVectorIcon
import com.snehonir.lipirx.ui.theme.icons.DarkThemeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.EyeHideVectorIcon
import com.snehonir.lipirx.ui.theme.icons.EyeShowVectorIcon
import com.snehonir.lipirx.ui.theme.icons.LightThemeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.McpEndpointVectorIcon
import com.snehonir.lipirx.ui.theme.icons.McpServerVectorIcon
import com.snehonir.lipirx.ui.theme.icons.OnDeviceStrategyVectorIcon
import com.snehonir.lipirx.ui.theme.icons.PipelineMatrixVectorIcon
import com.snehonir.lipirx.ui.theme.icons.ProviderSettingsVectorIcon
import com.snehonir.lipirx.ui.theme.icons.SystemThemeVectorIcon
import com.snehonir.lipirx.ui.theme.icons.icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

/** Lists longer than this open a searchable full list instead of a small dropdown. */
private const val SEARCH_THRESHOLD = 8

/** How long the "saved" banner stays visible. */
private const val SUCCESS_BANNER_MS = 4_000L

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// Order: Status → Appearance → AI execution → Provider & key → Agents → Integrations → About.
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
    slmModelDirectory: String = "",
    onSaveSlmDirectoryPath: (String) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onViewTelemetry: (() -> Unit)? = null,
    updateManager: UpdateManager = koinInject(),
    onClose: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var selectedTabProvider by remember(providerType) { mutableStateOf(providerType) }

    val keyFor: (String) -> String = { providerId ->
        when (providerId) {
            AiProvider.Groq.id -> groqApiKey.ifBlank { apiKey }
            AiProvider.OpenAi.id -> openaiApiKey.ifBlank { apiKey }
            AiProvider.Claude.id -> claudeApiKey.ifBlank { apiKey }
            AiProvider.Custom.id -> customApiKey.ifBlank { apiKey }
            else -> geminiApiKey.ifBlank { apiKey }
        }
    }
    val keyForSelectedProvider = keyFor(selectedTabProvider)

    // Setup status for the card at the top: is the app actually ready to scan?
    // A custom endpoint may not need a key, and on-device needs none.
    val activeProvider = AiProvider.fromId(providerType)
    val needsApiKey = activeProvider != AiProvider.OnDevice &&
            activeProvider != AiProvider.Custom &&
            keyFor(providerType).isBlank()

    val agentNeedsModel: (RxAgentType) -> Boolean = { agent ->
        val config = agentConfigs[agent]
        val agentProvider = config?.provider ?: agent.defaultProvider
        providerModelsMap[agentProvider.id].isNullOrEmpty() && config?.selectedModel.isNullOrBlank()
    }
    val agentsNeedingModel = RxAgentType.ALL.count(agentNeedsModel)

    var inputKey by remember(selectedTabProvider, keyForSelectedProvider) { mutableStateOf(keyForSelectedProvider) }
    var inputUrl by remember(customUrl) { mutableStateOf(customUrl) }

    // Pasting a key (or changing the URL) does nothing until Save, so we track it at the top
    // of the screen: it drives the "unsaved" note and the discard-on-back dialog.
    val isKeyBasedProvider = selectedTabProvider != AiProvider.OnDevice.id
    val hasUnsavedChanges = isKeyBasedProvider &&
            (inputKey != keyForSelectedProvider ||
                    (selectedTabProvider == AiProvider.Custom.id && inputUrl != customUrl))

    val saveConfig: () -> Unit = {
        // The custom model is no longer typed here; keep whatever is currently selected
        // so saving the endpoint doesn't wipe it.
        val modelForSave = if (selectedTabProvider == AiProvider.Custom.id) selectedModel else ""
        onSaveProviderConfig(selectedTabProvider, inputUrl, inputKey, modelForSave)
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestClose: () -> Unit = {
        if (hasUnsavedChanges) showDiscardDialog = true else onClose()
    }

    // Used by the status card to jump to whatever is missing.
    val providerSectionRequester = remember { BringIntoViewRequester() }
    val agentsSectionRequester = remember { BringIntoViewRequester() }
    var expandedAgentType by remember { mutableStateOf<RxAgentType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        AppHeader(
            onBack = requestClose,
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

            // ── 0. SETUP STATUS ──────────────────────────────────────────────
            // First thing on screen: is everything configured, and if not, tap to fix it.
            SetupStatusCard(
                providerName = activeProvider.displayName,
                needsApiKey = needsApiKey,
                agentsNeedingModel = agentsNeedingModel,
                totalAgents = RxAgentType.ALL.size,
                onFix = {
                    scope.launch {
                        if (needsApiKey) {
                            selectedTabProvider = providerType
                            providerSectionRequester.bringIntoView()
                        } else {
                            expandedAgentType = RxAgentType.ALL.firstOrNull(agentNeedsModel)
                            agentsSectionRequester.bringIntoView()
                        }
                    }
                }
            )

            // ── 1. APPEARANCE ────────────────────────────────────────────────
            SettingsSection(
                icon = SystemThemeVectorIcon,
                title = stringResource(Res.string.section_appearance_theme)
            ) {
                SettingsCard(contentPadding = 4.dp) {
                    ThemeModeSelector(themeMode = themeMode, onSelect = onSelectThemeMode)
                }
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
                title = stringResource(Res.string.section_provider_settings),
                modifier = Modifier.bringIntoViewRequester(providerSectionRequester)
            ) {
                val activeCardProvider = AiProvider.fromId(selectedTabProvider)
                val activeCardModelsCount = providerModelsMap[activeCardProvider.id]?.size ?: 0
                val activeCardProviderValue = if (activeCardModelsCount > 0) {
                    stringResource(Res.string.label_provider_models_count, activeCardProvider.displayName, activeCardModelsCount)
                } else {
                    activeCardProvider.displayName
                }

                SettingsCard {
                    PickerField(
                        label = stringResource(Res.string.label_select_provider),
                        valueText = activeCardProviderValue,
                        accent = activeCardProvider.brandColor,
                        leadingIcon = activeCardProvider.icon,
                        contentDescription = stringResource(Res.string.label_select_provider),
                        options = providerOptions(
                            selectedId = selectedTabProvider,
                            providerModelsMap = providerModelsMap,
                            onPick = { prov ->
                                selectedTabProvider = prov.id
                                onProviderTabSelected(prov.id)
                            }
                        )
                    )

                    // Custom endpoint: only the URL is needed here. The model is picked
                    // per agent, from the list the endpoint returns.
                    if (selectedTabProvider == AiProvider.Custom.id) {
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
                            // URL keyboard (has "/" and ".com") and a Done key that closes it.
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (selectedTabProvider == AiProvider.OnDevice.id) {
                        InfoNote(
                            text = stringResource(Res.string.on_device_info_banner),
                            icon = Icons.Default.Info
                        )

                        val onDeviceModelsList = providerModelsMap[AiProvider.OnDevice.id] ?: emptyList()

                        val launchFolderPicker = rememberFolderPickerLauncher { selectedPath ->
                            onSaveSlmDirectoryPath(selectedPath)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Lipi.ImageWell,
                                border = BorderStroke(1.dp, Lipi.Outline.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Lipi.Primary
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.label_slm_directory),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Lipi.Muted
                                        )
                                        Text(
                                            text = slmModelDirectory.ifBlank { stringResource(Res.string.default_slm_directory_name) },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Lipi.Ink,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (slmModelDirectory.isNotBlank()) {
                                        // Default IconButton gives a proper 48dp touch target.
                                        IconButton(onClick = { onSaveSlmDirectoryPath("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = stringResource(Res.string.content_desc_clear_api_key),
                                                modifier = Modifier.size(18.dp),
                                                tint = Lipi.Ink
                                            )
                                        }
                                    }
                                }
                            }

                            // Tip: swap Icons.Default.Search for a folder icon
                            // (Icons.Default.Folder needs material-icons-extended).
                            SecondaryButton(
                                text = stringResource(Res.string.btn_browse_folder),
                                icon = Icons.Default.Search,
                                onClick = launchFolderPicker,
                                modifier = Modifier.fillMaxWidth(),
                                height = 44.dp,
                                fontSize = 13
                            )

                            if (onDeviceModelsList.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Lipi.PrimaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                            text = stringResource(Res.string.slm_models_found_count, onDeviceModelsList.size),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Lipi.OnPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // The server error is hidden as soon as the user starts editing again.
                        var keyEdited by remember(apiKeyError) { mutableStateOf(false) }
                        val resolvedError: String? = if (apiKeyError.isNullOrBlank() || keyEdited) {
                            null
                        } else {
                            when (apiKeyError) {
                                AppConstants.ERROR_ENTER_VALID_GEMINI_KEY -> stringResource(Res.string.error_enter_valid_gemini_key)
                                AppConstants.ERROR_ENTER_VALID_URL -> stringResource(Res.string.error_enter_valid_url)
                                AppConstants.ERROR_NO_MODELS_FOUND -> stringResource(Res.string.error_no_models_found)
                                else -> apiKeyError
                            }
                        }

                        // API key: typeable or pasteable, masked by default.
                        ApiKeyField(
                            value = inputKey,
                            onValueChange = {
                                inputKey = it.trim()
                                keyEdited = true
                            },
                            errorText = resolvedError,
                            onDone = {
                                focusManager.clearFocus()
                                if (hasUnsavedChanges && !isTestingApiKey) saveConfig()
                            }
                        )

                        StatusLine(
                            ok = keyForSelectedProvider.isNotBlank(),
                            text = if (keyForSelectedProvider.isNotBlank()) {
                                stringResource(Res.string.label_api_key_saved)
                            } else {
                                stringResource(Res.string.label_no_api_key_saved)
                            }
                        )

                        if (hasUnsavedChanges) {
                            UnsavedNote()
                        }

                        // The "saved" banner clears itself after a few seconds.
                        var successDismissed by remember(apiKeySuccess) { mutableStateOf(false) }
                        LaunchedEffect(apiKeySuccess) {
                            if (!apiKeySuccess.isNullOrBlank()) {
                                delay(SUCCESS_BANNER_MS.milliseconds)
                                successDismissed = true
                            }
                        }

                        if (!apiKeySuccess.isNullOrBlank() && !successDismissed) {
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
                            onClick = saveConfig,
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

            // ── 4. PER-AGENT ASSIGNMENTS (accordion cards) ─────────────────
            SettingsSection(
                icon = PipelineMatrixVectorIcon,
                title = stringResource(Res.string.section_per_agent_assignments),
                modifier = Modifier.bringIntoViewRequester(agentsSectionRequester)
            ) {
                RxAgentType.ALL.forEach { agentType ->
                    AgentAssignmentCard(
                        agentType = agentType,
                        config = agentConfigs[agentType],
                        providerModelsMap = providerModelsMap,
                        onDeviceStrategy = onDeviceStrategy,
                        isExpanded = expandedAgentType == agentType,
                        onToggleExpand = {
                            expandedAgentType = if (expandedAgentType == agentType) null else agentType
                        },
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

            // ── 6. ABOUT LIPIRX & UPDATES ────────────────────────────────────
            SettingsSection(
                icon = Icons.Default.Info,
                title = stringResource(Res.string.section_about)
            ) {
                val updateState by updateManager.updateState.collectAsState()
                val currentVer = updateManager.currentVersionName

                val isChecking = updateState is UpdateState.Checking
                val isBusy = isChecking || updateState is UpdateState.Downloading

                val showInlineStatus = updateState !is UpdateState.Idle && updateState !is UpdateState.Checking

                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(Res.string.app_name),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Ink
                        )
                        Text(
                            text = stringResource(Res.string.label_app_version, currentVer),
                            fontSize = 12.5.sp,
                            color = Lipi.Muted
                        )
                    }

                    // Full width so a long translated label never squeezes the version text.
                    // While checking, the button itself says so (no second spinner row).
                    SecondaryButton(
                        text = stringResource(
                            if (isChecking) Res.string.update_checking else Res.string.btn_check_for_updates
                        ),
                        icon = Icons.Default.Refresh,
                        enabled = !isBusy,
                        onClick = { updateManager.checkForUpdates(silent = false, fromSettings = true) },
                        modifier = Modifier.fillMaxWidth(),
                        height = 48.dp,
                        fontSize = 14
                    )

                    if (showInlineStatus) {
                        HorizontalDivider(color = Lipi.Outline.copy(alpha = 0.5f), thickness = 1.dp)
                        UpdateContent(
                            state = updateState,
                            updateManager = updateManager
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Leaving with an unsaved key/URL asks first instead of silently discarding it.
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(Res.string.dialog_discard_title)) },
            text = { Text(stringResource(Res.string.dialog_discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onClose()
                }) {
                    Text(stringResource(Res.string.dialog_discard_confirm), color = Lipi.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(Res.string.dialog_keep_editing))
                }
            },
            containerColor = Lipi.Surface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// API key field
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String?,
    onDone: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.label_api_key)) },
        placeholder = {
            Text(stringResource(Res.string.placeholder_paste_key), fontSize = 12.sp)
        },
        leadingIcon = {
            Icon(ApiKeyVectorIcon, null, Modifier.size(20.dp), tint = Lipi.Primary)
        },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) EyeHideVectorIcon else EyeShowVectorIcon,
                    contentDescription = stringResource(if (visible) Res.string.action_hide else Res.string.action_show),
                    modifier = Modifier.size(22.dp),
                    tint = Lipi.Primary
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation('•'),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
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
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onProviderSelected: (AiProvider) -> Unit,
    onModelSelected: (String) -> Unit,
    onFallbackProviderSelected: (AiProvider) -> Unit,
    onFallbackModelSelected: (String) -> Unit,
) {
    val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f)

    val provider = config?.provider ?: agentType.defaultProvider
    val assignedModel = config?.selectedModel?.ifBlank { "" } ?: ""
    val liveModels = providerModelsMap[provider.id] ?: emptyList()
    val hasProblem = liveModels.isEmpty() || (assignedModel.isNotBlank() && !liveModels.contains(assignedModel))

    val rawModelText = when {
        assignedModel.isNotBlank() && liveModels.contains(assignedModel) -> assignedModel
        liveModels.isNotEmpty() -> liveModels.first()
        provider == AiProvider.OnDevice -> stringResource(Res.string.no_local_models_found)
        else -> stringResource(Res.string.configure_key_to_load_models)
    }
    val modelText = if (provider == AiProvider.OnDevice) formatModelDisplayName(rawModelText) else rawModelText

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Lipi.Surface,
        border = BorderStroke(1.dp, if (isExpanded) Lipi.Primary.copy(alpha = 0.4f) else Lipi.CardBorder)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            // Header (always visible)
            Surface(
                onClick = onToggleExpand,
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
                        // Problems get an icon too, so they are not signalled by colour alone.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (hasProblem) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(Res.string.status_needs_attention),
                                    modifier = Modifier.size(14.dp),
                                    tint = Lipi.Error
                                )
                            }
                            Text(
                                text = "${provider.displayName} · $modelText",
                                fontSize = 12.sp,
                                color = if (hasProblem) Lipi.Error else Lipi.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(if (isExpanded) Res.string.cd_collapse else Res.string.cd_expand),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowRotation),
                        tint = Lipi.Muted
                    )
                }
            }

            if (isExpanded) {
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

                    val primaryProviderValue = if (liveModels.isNotEmpty()) {
                        stringResource(Res.string.label_provider_models_count, provider.displayName, liveModels.size)
                    } else {
                        provider.displayName
                    }

                    PickerField(
                        label = stringResource(Res.string.label_provider),
                        valueText = primaryProviderValue,
                        accent = provider.brandColor,
                        leadingIcon = provider.icon,
                        contentDescription = stringResource(Res.string.content_desc_select_provider),
                        options = providerOptions(
                            selectedId = provider.id,
                            providerModelsMap = providerModelsMap,
                            onPick = onProviderSelected,
                            isLocked = { prov ->
                                when (onDeviceStrategy) {
                                    OnDeviceStrategy.HYBRID_FALLBACK, OnDeviceStrategy.CLOUD_ONLY -> prov == AiProvider.OnDevice
                                    OnDeviceStrategy.ON_DEVICE_FIRST, OnDeviceStrategy.ON_DEVICE_ONLY -> prov != AiProvider.OnDevice
                                }
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
                        val hasFallbackProblem = fallbackModels.isEmpty() || (fallbackModel.isNotBlank() && !fallbackModels.contains(fallbackModel))

                        val rawFallbackModelText = when {
                            fallbackModel.isNotBlank() && fallbackModels.contains(fallbackModel) -> fallbackModel
                            fallbackModels.isNotEmpty() -> fallbackModels.first()
                            fallbackProvider == AiProvider.OnDevice -> stringResource(Res.string.no_local_models_found)
                            else -> stringResource(Res.string.configure_key_to_load_models)
                        }
                        val fallbackModelText = if (fallbackProvider == AiProvider.OnDevice) formatModelDisplayName(rawFallbackModelText) else rawFallbackModelText

                        val fallbackProviderValue = if (fallbackModels.isNotEmpty()) {
                            stringResource(Res.string.label_provider_models_count, fallbackProvider.displayName, fallbackModels.size)
                        } else {
                            fallbackProvider.displayName
                        }

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
                            valueText = fallbackProviderValue,
                            accent = fallbackProvider.brandColor,
                            leadingIcon = fallbackProvider.icon,
                            contentDescription = stringResource(Res.string.content_desc_select_fallback_provider),
                            options = providerOptions(
                                selectedId = fallbackProvider.id,
                                providerModelsMap = providerModelsMap,
                                onPick = onFallbackProviderSelected,
                                isLocked = { prov ->
                                    when (onDeviceStrategy) {
                                        OnDeviceStrategy.HYBRID_FALLBACK -> prov != AiProvider.OnDevice
                                        OnDeviceStrategy.ON_DEVICE_FIRST -> prov == AiProvider.OnDevice
                                        OnDeviceStrategy.ON_DEVICE_ONLY -> prov != AiProvider.OnDevice
                                        OnDeviceStrategy.CLOUD_ONLY -> prov == AiProvider.OnDevice
                                    }
                                }
                            )
                        )
                        PickerField(
                            label = stringResource(Res.string.label_fallback_model),
                            valueText = fallbackModelText,
                            accent = fallbackProvider.brandColor,
                            isError = hasFallbackProblem,
                            contentDescription = stringResource(Res.string.content_desc_select_fallback_model),
                            options = if (fallbackModels.isNotEmpty()) {
                                modelOptions(fallbackModels, fallbackModel, fallbackProvider.brandColor, onFallbackModelSelected)
                            } else {
                                listOf(
                                    MenuOption(
                                        title = if (fallbackProvider == AiProvider.OnDevice) {
                                            stringResource(Res.string.no_local_models_found)
                                        } else {
                                            stringResource(Res.string.no_live_models_found, fallbackProvider.displayName)
                                        },
                                        tint = Lipi.Error,
                                        selected = false,
                                        onClick = {}
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Setup Status Card (tappable when something is missing)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetupStatusCard(
    providerName: String,
    needsApiKey: Boolean,
    agentsNeedingModel: Int,
    totalAgents: Int,
    onFix: () -> Unit,
) {
    val isReady = !needsApiKey && agentsNeedingModel == 0
    val bg = if (isReady) Lipi.PrimaryContainer else Lipi.AmberContainer
    val fg = if (isReady) Lipi.Primary else Lipi.OnAmber
    val textFg = if (isReady) Lipi.OnPrimaryContainer else Lipi.OnAmber
    val icon = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning

    val title = if (isReady) {
        stringResource(Res.string.status_ready_to_scan)
    } else {
        stringResource(Res.string.status_setup_needed)
    }

    val detail = when {
        isReady -> stringResource(Res.string.status_ready_detail, providerName, totalAgents)
        needsApiKey -> stringResource(Res.string.status_add_api_key_detail, providerName)
        else -> stringResource(Res.string.status_agents_no_model_detail, agentsNeedingModel, totalAgents)
    }

    Surface(
        onClick = onFix,
        enabled = !isReady,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textFg
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = textFg.copy(alpha = 0.88f)
                )
            }
            if (!isReady) {
                Text(
                    text = stringResource(Res.string.status_fix_action),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = textFg
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textFg
                )
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
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    onPick: (AiProvider) -> Unit,
    isLocked: (AiProvider) -> Boolean = { false },
): List<MenuOption> {
    val lockedText = stringResource(Res.string.label_locked_by_strategy)
    val noModelsText = stringResource(Res.string.label_no_models_loaded)
    return AiProvider.ALL.map { prov ->
        val locked = isLocked(prov)
        val count = providerModelsMap[prov.id]?.size ?: 0
        val countSubtitle = if (count > 0) stringResource(Res.string.label_models_available_count, count) else noModelsText
        MenuOption(
            title = if (count > 0) stringResource(Res.string.label_provider_models_count, prov.displayName, count) else prov.displayName,
            subtitle = if (locked) lockedText else countSubtitle,
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

/**
 * A tappable "field" that opens a picker. One implementation for every dropdown on the screen.
 * Short lists open a small dropdown; lists longer than [SEARCH_THRESHOLD] open a searchable
 * full list so people don't have to scroll through dozens of models.
 */
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
    val useSearchDialog = options.size > SEARCH_THRESHOLD
    val scrollState = rememberScrollState()
    var selectedY by remember(expanded) { mutableStateOf<Int?>(null) }

    // Dropdown only: scroll the open menu so the selected item is visible.
    LaunchedEffect(expanded, selectedY) {
        val y = selectedY
        if (!useSearchDialog && expanded && y != null) {
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
                    imageVector = if (useSearchDialog) Icons.Default.Search else Icons.Default.ArrowDropDown,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (useSearchDialog) 0f else arrowRotation),
                    tint = if (isError) Lipi.Error else Lipi.Muted
                )
            }
        }

        if (useSearchDialog) {
            if (expanded) {
                SearchablePickerDialog(
                    title = label ?: contentDescription.orEmpty(),
                    options = options,
                    onDismiss = { expanded = false }
                )
            }
        } else {
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
                                        modifier = Modifier.size(18.dp),
                                        tint = option.tint
                                    )
                                }
                            } else null,
                            text = {
                                Column {
                                    Text(
                                        text = option.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (option.selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (option.enabled) Lipi.Ink else Lipi.Muted.copy(alpha = 0.5f)
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
                                expanded = false
                                option.onClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Full-screen searchable picker used when a list is longer than [SEARCH_THRESHOLD]. */
@Composable
private fun SearchablePickerDialog(
    title: String,
    options: List<MenuOption>,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val filtered = remember(query, options) {
        if (query.isBlank()) options
        else options.filter {
            it.title.contains(query, ignoreCase = true) ||
                    (it.subtitle?.contains(query, ignoreCase = true) == true)
        }
    }

    // Scroll to selected item on open
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val selectedIndex = options.indexOfFirst { it.selected }
        if (selectedIndex > 0) {
            listState.scrollToItem((selectedIndex - 2).coerceAtLeast(0))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(24.dp),
            color = Lipi.Surface,
            border = BorderStroke(1.dp, Lipi.CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title.ifBlank { stringResource(Res.string.label_select_provider) },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.btn_cancel),
                            tint = Lipi.Muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.placeholder_search), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = Lipi.Primary)
                    },
                    trailingIcon = if (query.isNotBlank()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, null, Modifier.size(18.dp), tint = Lipi.Muted)
                            }
                        }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Options list
                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.label_no_matches),
                            fontSize = 13.sp,
                            color = Lipi.Muted
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { option ->
                            Surface(
                                onClick = {
                                    onDismiss()
                                    option.onClick()
                                },
                                enabled = option.enabled,
                                shape = RoundedCornerShape(12.dp),
                                color = if (option.selected) option.tint.copy(alpha = 0.12f) else Color.Transparent,
                                border = if (option.selected) BorderStroke(1.dp, option.tint.copy(alpha = 0.4f)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (option.icon != null) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (option.enabled) option.tint else option.tint.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (option.selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (option.enabled) Lipi.Ink else Lipi.Muted.copy(alpha = 0.5f)
                                        )
                                        if (option.subtitle != null) {
                                            Text(
                                                text = option.subtitle,
                                                fontSize = 11.sp,
                                                color = Lipi.Muted
                                            )
                                        }
                                    }
                                    if (option.selected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(Res.string.content_desc_selected),
                                            modifier = Modifier.size(18.dp),
                                            tint = option.tint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Three pills side by side. No outer wrapper, so there is no double border. */
@Composable
private fun ThemeModeSelector(
    themeMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val modes = remember {
        listOf(
            Triple(ThemeMode.SYSTEM, Res.string.theme_system_default, SystemThemeVectorIcon),
            Triple(ThemeMode.LIGHT, Res.string.theme_light, LightThemeVectorIcon),
            Triple(ThemeMode.DARK, Res.string.theme_dark, DarkThemeVectorIcon)
        )
    }

    val externalIndex = when (themeMode) {
        ThemeMode.SYSTEM -> 0
        ThemeMode.LIGHT -> 1
        ThemeMode.DARK -> 2
    }

    var displayedIndex by remember(externalIndex) { mutableIntStateOf(externalIndex) }
    var isPillAnimating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Lipi.ImageWell, RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val totalSpacing = 8.dp * (modes.size - 1)
        val segmentWidth = (maxWidth - totalSpacing) / modes.size
        val indicatorOffset by animateDpAsState(
            targetValue = (segmentWidth + 8.dp) * displayedIndex,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "indicatorOffset"
        )

        // Sliding selection highlight
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(Lipi.PrimaryContainer, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Lipi.Primary.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
        )

        // Tappable segment options
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEachIndexed { index, (mode, stringRes, icon) ->
                val isSelected = index == displayedIndex

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Lipi.Primary else Lipi.Muted,
                    animationSpec = tween(200),
                    label = "iconTint"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Lipi.OnPrimaryContainer else Lipi.Ink,
                    animationSpec = tween(200),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = !isPillAnimating && index != displayedIndex,
                            role = Role.RadioButton
                        ) {
                            displayedIndex = index
                            isPillAnimating = true
                            scope.launch {
                                delay(220.milliseconds)
                                isPillAnimating = false
                                onSelect(mode)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconTint
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(stringRes),
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Lipi.Primary
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = Lipi.Ink,
                modifier = Modifier.semantics { heading() }
            )
        }
        content()
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 14.dp,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Lipi.PrimaryContainer, CircleShape),
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
private fun InfoNote(
    text: String,
    icon: ImageVector = Icons.Default.Info
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Lipi.ImageWell
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Lipi.Primary
            )
            Text(
                text = text,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Lipi.Muted
            )
        }
    }
}

@Composable
private fun UnsavedNote() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Lipi.AmberContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Lipi.OnAmber
            )
            Text(
                text = stringResource(Res.string.note_unsaved_changes),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Lipi.OnAmber
            )
        }
    }
}

@Composable
private fun StatusLine(ok: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (ok) Lipi.StatusActiveGreen else Lipi.Muted, CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (ok) Lipi.StatusActiveGreen else Lipi.Muted
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = Lipi.Outline.copy(alpha = 0.5f), thickness = 1.dp)
}
