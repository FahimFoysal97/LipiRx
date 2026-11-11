package com.snehonir.lipirx.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.data.local.ProviderConfigEntity
import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.data.local.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.MultiAgentRxOrchestrator
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ScanViewModel(
    private val aiService: CompositeAiService,
    private val orchestrator: MultiAgentRxOrchestrator,
    private val database: LipiRxLocalDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<ScanUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<ScanUiEvent> = _uiEvent.receiveAsFlow()

    init {
        val geminiKey = runCatching { database.getApiKeyForProvider("gemini") }.getOrDefault("")
        val groqKey = runCatching { database.getApiKeyForProvider("groq") }.getOrDefault("")
        val openaiKey = runCatching { database.getApiKeyForProvider("openai") }.getOrDefault("")
        val customKey = runCatching { database.getApiKeyForProvider("custom") }.getOrDefault("")

        val config = runCatching {
            database.getProviderConfig(flow = "App -> ScanViewModel.init.getProviderConfig")
        }.getOrDefault(ProviderConfigEntity())

        val providerObj = AiProvider.fromId(config.providerType)
        val savedModelForProvider = runCatching {
            database.getLastSelectedModel(providerObj, flow = "App -> ScanViewModel.init.getLastSelectedModel")
        }.getOrDefault("").removePrefix("models/").trim()
        val cleanModel =
            savedModelForProvider.ifBlank { config.modelName.removePrefix("models/").trim().ifBlank { providerObj.defaultModel } }

        val loadedAgentConfigs = runCatching {
            database.getAllAgentConfigs()
        }.getOrDefault(emptyMap()).toImmutableMap()

        val loadedProviderModelsMap = loadProviderModelsMap()

        _uiState.update {
            it.copy(
                provider = providerObj,
                customUrl = config.customUrl,
                geminiApiKey = geminiKey,
                groqApiKey = groqKey,
                openaiApiKey = openaiKey,
                customApiKey = customKey,
                selectedModel = cleanModel,
                agentConfigs = loadedAgentConfigs,
                providerModelsMap = loadedProviderModelsMap
            )
        }

        val activeKey = when (providerObj) {
            AiProvider.Groq -> groqKey
            AiProvider.OpenAi -> openaiKey
            AiProvider.Custom -> customKey
            AiProvider.Gemini -> geminiKey
        }

        if (activeKey.isNotBlank() || providerObj != AiProvider.Gemini) {
            fetchModels(flow = "App -> ScanViewModel.init")
        }
    }

    private fun loadProviderModelsMap(): ImmutableMap<String, ImmutableList<String>> {
        val rawMap = runCatching { database.getAllProviderModelsMap() }.getOrDefault(emptyMap())
        val resultMap = mutableMapOf<String, ImmutableList<String>>()

        AiProvider.ALL.forEach { prov ->
            val savedList = rawMap[prov.id] ?: emptyList()
            resultMap[prov.id] = savedList.toImmutableList()
        }
        return resultMap.toImmutableMap()
    }

    fun fetchModelsForProvider(
        targetProvider: AiProvider,
        flow: String = "App -> ScanViewModel.fetchModelsForProvider"
    ) {
        val apiKey = database.getApiKeyForProvider(targetProvider.id)
        val customUrl = if (targetProvider == AiProvider.Custom) database.getProviderConfig().customUrl else ""

        if (apiKey.isBlank() && targetProvider == AiProvider.Gemini) {
            Napier.d("[$flow] Skipping fetch for ${targetProvider.id} because API key is blank", tag = TAG)
            return
        }
        if (targetProvider == AiProvider.Custom && customUrl.isBlank()) {
            Napier.d("[$flow] Skipping fetch for Custom provider because customUrl is blank", tag = TAG)
            return
        }

        Napier.d("[$flow] Auto-fetching models for ${targetProvider.displayName}...", tag = TAG)

        viewModelScope.launch {
            val result = aiService.fetchAvailableModels(
                providedApiKey = apiKey,
                provider = targetProvider,
                customUrl = customUrl,
                flow = "$flow -> CompositeAiService.fetchAvailableModels"
            )

            result.fold(
                onSuccess = { modelList ->
                    val names = modelList.map { it.name.removePrefix("models/").trim() }.toImmutableList()
                    if (names.isNotEmpty()) {
                        runCatching {
                            database.saveProviderModels(targetProvider.id, names, flow = flow)
                        }

                        _uiState.update { currentState ->
                            val updatedMap = currentState.providerModelsMap.toMutableMap()
                            updatedMap[targetProvider.id] = names
                            currentState.copy(providerModelsMap = updatedMap.toImmutableMap())
                        }
                    }
                },
                onFailure = { ex ->
                    Napier.w("[$flow -> Warning] Failed to fetch models for ${targetProvider.displayName}: ${ex.message}", throwable = ex, tag = TAG)
                }
            )
        }
    }

    fun onAgentProviderSelected(
        agentType: RxAgentType,
        newProvider: AiProvider,
        modelName: String = "",
        flow: String = "App -> ScanViewModel.onAgentProviderSelected"
    ) {
        val savedDbModels = runCatching { database.getProviderModels(newProvider.id) }.getOrDefault(emptyList()).toImmutableList()
        val currentModelsMap = _uiState.value.providerModelsMap.toMutableMap()
        if (savedDbModels.isNotEmpty()) {
            currentModelsMap[newProvider.id] = savedDbModels
        }

        val modelsForNewProvider = currentModelsMap[newProvider.id] ?: savedDbModels
        val chosenModel = when {
            modelName.isNotBlank() -> modelName
            modelsForNewProvider.isNotEmpty() -> modelsForNewProvider.first()
            else -> ""
        }

        Napier.d("[$flow] User assigned agent '${agentType.id}' to provider '${newProvider.id}' (model: $chosenModel, modelsCount=${modelsForNewProvider.size})", tag = TAG)

        runCatching {
            database.saveAgentConfig(
                agentType = agentType,
                provider = newProvider,
                modelName = chosenModel,
                flow = flow
            )
        }

        _uiState.update { currentState ->
            val updatedAgentMap = currentState.agentConfigs.toMutableMap()
            updatedAgentMap[agentType] = AgentConfig(
                agentType = agentType,
                provider = newProvider,
                selectedModel = chosenModel
            )
            currentState.copy(
                agentConfigs = updatedAgentMap.toImmutableMap(),
                providerModelsMap = currentModelsMap.toImmutableMap()
            )
        }

        if (modelsForNewProvider.isEmpty()) {
            fetchModelsForProvider(newProvider, flow = "$flow -> autoFetch")
        }
    }

    fun onAgentModelSelected(
        agentType: RxAgentType,
        modelName: String,
        flow: String = "App -> ScanViewModel.onAgentModelSelected"
    ) {
        val currentConfig = _uiState.value.agentConfigs[agentType]
        val currentProvider = currentConfig?.provider ?: agentType.defaultProvider

        Napier.d("[$flow] User selected model '$modelName' for agent '${agentType.id}' (${currentProvider.id})", tag = TAG)

        runCatching {
            database.saveAgentConfig(
                agentType = agentType,
                provider = currentProvider,
                modelName = modelName,
                flow = flow
            )
        }

        _uiState.update { currentState ->
            val updatedMap = currentState.agentConfigs.toMutableMap()
            updatedMap[agentType] = AgentConfig(
                agentType = agentType,
                provider = currentProvider,
                selectedModel = modelName
            )
            currentState.copy(agentConfigs = updatedMap.toImmutableMap())
        }
    }

    fun onProviderTabSelected(newProviderType: String, flow: String = "App -> ScanViewModel.onProviderTabSelected") {
        val providerObj = AiProvider.fromId(newProviderType)
        Napier.d("[$flow] User switched provider tab to '${providerObj.id}'", tag = TAG)

        val savedModelForProvider = runCatching {
            database.getLastSelectedModel(providerObj, flow = "$flow -> LipiRxLocalDatabase.getLastSelectedModel")
        }.getOrDefault("").removePrefix("models/").trim().ifBlank { providerObj.defaultModel }

        _uiState.update {
            it.copy(
                provider = providerObj,
                selectedModel = savedModelForProvider,
                apiKeyError = null,
                apiKeySuccess = null
            )
        }

        fetchModels(flow = "$flow -> fetchModels")
    }

    fun saveAndTestProviderConfig(
        providerType: String,
        customUrl: String,
        apiKey: String,
        modelName: String = "",
        flow: String = "App -> ScanViewModel.saveAndTestProviderConfig"
    ) {
        val providerObj = AiProvider.fromId(providerType)
        val cleanKey = apiKey.trim()
        val cleanUrl = customUrl.trim()
        val cleanModelInput = modelName.trim()

        if (providerObj == AiProvider.Gemini && cleanKey.isBlank()) {
            _uiState.update { it.copy(apiKeyError = "Please enter a valid Gemini API Key.", apiKeySuccess = null) }
            return
        }
        if (providerObj == AiProvider.Custom && cleanUrl.isBlank()) {
            _uiState.update { it.copy(apiKeyError = "Please enter a valid Endpoint URL.", apiKeySuccess = null) }
            return
        }

        Napier.d("[$flow] Testing Provider Config: provider=${providerObj.id}, url=$cleanUrl, model=$cleanModelInput", tag = TAG)
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingApiKey = true, apiKeyError = null, apiKeySuccess = null) }
            val result = aiService.fetchAvailableModels(
                providedApiKey = cleanKey,
                provider = providerObj,
                customUrl = cleanUrl,
                flow = "$flow -> CompositeAiService.fetchAvailableModels"
            )

            result.fold(
                onSuccess = { modelList ->
                    val names = modelList.map { it.name.removePrefix("models/").trim() }.toImmutableList()
                    Napier.d("[$flow -> Success] Connection successful! Retrieved ${names.size} models.", tag = TAG)

                    if (names.isNotEmpty()) {
                        val chosenModel = when {
                            cleanModelInput.isNotBlank() -> cleanModelInput
                            names.contains("gemini-2.0-flash") -> "gemini-2.0-flash"
                            else -> names.first()
                        }

                        runCatching {
                            database.saveProviderConfig(
                                providerType = providerObj.id,
                                customUrl = cleanUrl,
                                apiKey = cleanKey,
                                modelName = chosenModel,
                                flow = "$flow -> LipiRxLocalDatabase.saveProviderConfig"
                            )
                            database.saveProviderModels(providerObj.id, names, flow = "$flow -> LipiRxLocalDatabase.saveProviderModels")
                        }

                        val successMsg = "✓ ${providerObj.displayName} verified & saved! (${names.size} models loaded)"

                        _uiState.update { currentState ->
                            val newGeminiKey = if (providerObj == AiProvider.Gemini) cleanKey else currentState.geminiApiKey
                            val newGroqKey = if (providerObj == AiProvider.Groq) cleanKey else currentState.groqApiKey
                            val newOpenAiKey = if (providerObj == AiProvider.OpenAi) cleanKey else currentState.openaiApiKey
                            val newCustomKey = if (providerObj == AiProvider.Custom) cleanKey else currentState.customApiKey

                            val updatedProviderModels = currentState.providerModelsMap.toMutableMap()
                            updatedProviderModels[providerObj.id] = names

                            currentState.copy(
                                provider = providerObj,
                                customUrl = cleanUrl,
                                geminiApiKey = newGeminiKey,
                                groqApiKey = newGroqKey,
                                openaiApiKey = newOpenAiKey,
                                customApiKey = newCustomKey,
                                isTestingApiKey = false,
                                apiKeyError = null,
                                apiKeySuccess = successMsg,
                                availableModels = names,
                                selectedModel = chosenModel,
                                providerModelsMap = updatedProviderModels.toImmutableMap()
                            )
                        }

                        _uiEvent.send(ScanUiEvent.ShowToast(successMsg))
                    } else {
                        _uiState.update {
                            it.copy(
                                isTestingApiKey = false,
                                apiKeyError = "No supported models found for this endpoint.",
                                apiKeySuccess = null
                            )
                        }
                    }
                },
                onFailure = { ex ->
                    val errorMsg = ex.message ?: "Connection failed. Please check your credentials and URL."
                    Napier.w("[$flow -> Failure] Provider test failed: $errorMsg", throwable = ex, tag = TAG)
                    _uiState.update {
                        it.copy(
                            isTestingApiKey = false,
                            apiKeyError = errorMsg,
                            apiKeySuccess = null
                        )
                    }
                }
            )
        }
    }

    fun selectModel(modelName: String, flow: String = "App -> ScanViewModel.selectModel") {
        val cleanModel = modelName.removePrefix("models/").trim()
        val currentProvider = _uiState.value.provider
        Napier.d("[$flow] User selected model: '$cleanModel' for provider '${currentProvider.id}'. Saving to SQLite...", tag = TAG)
        _uiState.update { it.copy(selectedModel = cleanModel) }

        runCatching {
            database.saveSelectedModel(cleanModel, currentProvider, flow = "$flow -> LipiRxLocalDatabase.saveSelectedModel")
        }.onFailure { ex ->
            Napier.w("[$flow -> Warning] Failed to save selected model: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun fetchModels(flow: String = "App -> ScanViewModel.fetchModels") {
        val state = _uiState.value
        val currentApiKey = state.apiKey
        val currentProvider = state.provider
        val currentCustomUrl = state.customUrl

        Napier.d("[$flow] Triggering dynamic fetch of available models for ${currentProvider.displayName}", tag = TAG)

        if (currentProvider == AiProvider.Gemini && currentApiKey.isBlank()) {
            _uiState.update { it.copy(availableModels = persistentListOf(), isFetchingModels = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true) }
            val serviceFlow = "$flow -> CompositeAiService.fetchAvailableModels"
            val result = aiService.fetchAvailableModels(
                providedApiKey = currentApiKey,
                provider = currentProvider,
                customUrl = currentCustomUrl,
                flow = serviceFlow
            )

            result.fold(
                onSuccess = { modelList ->
                    val names = modelList.map { it.name.removePrefix("models/").trim() }.toImmutableList()
                    Napier.d("[$flow -> Success] Retrieved ${names.size} models directly from API: $names", tag = TAG)

                    if (names.isNotEmpty()) {
                        runCatching {
                            database.saveProviderModels(currentProvider.id, names, flow = "$flow -> LipiRxLocalDatabase.saveProviderModels")
                        }

                        val savedModel = runCatching {
                            database.getLastSelectedModel(currentProvider, flow = "$flow -> LipiRxLocalDatabase.getLastSelectedModel")
                        }.getOrDefault("").removePrefix("models/").trim()

                        _uiState.update { currentState ->
                            val currentSelected = currentState.selectedModel.removePrefix("models/").trim()

                            val matchedSaved = names.firstOrNull { it.equals(savedModel, ignoreCase = true) }
                            val matchedCurrent = names.firstOrNull { it.equals(currentSelected, ignoreCase = true) }

                            val chosenModel = when {
                                matchedSaved != null -> matchedSaved
                                matchedCurrent != null -> matchedCurrent
                                else -> names.first()
                            }

                            val updatedProviderModels = currentState.providerModelsMap.toMutableMap()
                            updatedProviderModels[currentProvider.id] = names

                            currentState.copy(
                                isFetchingModels = false,
                                availableModels = names,
                                selectedModel = chosenModel,
                                providerModelsMap = updatedProviderModels.toImmutableMap()
                            )
                        }
                    } else {
                        _uiState.update { it.copy(availableModels = persistentListOf(), isFetchingModels = false) }
                    }
                },
                onFailure = { ex ->
                    Napier.w("[$flow -> Warning] Failed to fetch dynamic models list: ${ex.message}", throwable = ex, tag = TAG)
                    _uiState.update { it.copy(availableModels = persistentListOf(), isFetchingModels = false) }
                }
            )
        }
    }

    fun onImageSelected(imageByteArray: ByteArray, flow: String = "App -> ScanViewModel.onImageSelected") {
        Napier.d("[$flow] Prescription image selected (${imageByteArray.size} bytes). Storing preview state.", tag = TAG)
        _uiState.update {
            it.copy(
                capturedImageByteArray = imageByteArray,
                errorMessage = null,
                medicines = persistentListOf()
            )
        }
    }

    fun processSelectedImage(flow: String = "App -> ScanViewModel.processSelectedImage") {
        val bytes = _uiState.value.capturedImageByteArray
        if (bytes == null || bytes.isEmpty()) {
            Napier.w("[$flow -> Warning] Attempted to process empty image bytes", tag = TAG)
            _uiState.update { it.copy(errorMessage = "Please capture or select an image first.") }
            return
        }
        Napier.d("[$flow] User confirmed process button. Initiating Vision AI execution...", tag = TAG)
        processPrescriptionImage(bytes, flow = flow)
    }

    fun clearSelectedImage(flow: String = "App -> ScanViewModel.clearSelectedImage") {
        Napier.d("[$flow] User cleared selected image preview.", tag = TAG)
        _uiState.update {
            it.copy(
                capturedImageByteArray = null,
                errorMessage = null,
                medicines = persistentListOf()
            )
        }
    }

    private var processingJob: kotlinx.coroutines.Job? = null

    fun stopAnalysis(flow: String = "App -> ScanViewModel.stopAnalysis") {
        Napier.d("[$flow] User requested analysis cancellation. Cancelling processing job...", tag = TAG)
        processingJob?.cancel()
        processingJob = null
        _uiState.update {
            it.copy(
                isLoading = false,
                loadingStep = 0,
                loadingStatusText = "",
                errorMessage = "Analysis stopped by user."
            )
        }
    }

    fun processPrescriptionImage(
        imageByteArray: ByteArray,
        flow: String = "App -> ScanViewModel.processPrescriptionImage"
    ) {
        Napier.d("[$flow] Starting image processing flow. Image size: ${imageByteArray.size} bytes", tag = TAG)

        if (imageByteArray.isEmpty()) {
            Napier.w("[$flow -> Warning] Empty image byte array received", tag = TAG)
            _uiState.update { it.copy(errorMessage = "Image data is empty.") }
            return
        }

        val state = _uiState.value
        val visionConfig = state.agentConfigs[RxAgentType.TRANSCRIPTION]
        val visionProvider = visionConfig?.provider ?: state.provider
        val visionModel = visionConfig?.selectedModel?.ifBlank { visionProvider.defaultModel } ?: state.selectedModel.ifBlank { visionProvider.defaultModel }
        val visionApiKey = database.getApiKeyForProvider(visionProvider.id).ifBlank { state.apiKey }
        val customUrl = if (visionProvider == AiProvider.Custom) database.getProviderConfig().customUrl else state.customUrl

        if (visionProvider.requiresApiKey && visionApiKey.isBlank()) {
            Napier.w("[$flow -> Warning] API key is missing for ${visionProvider.displayName}", tag = TAG)
            _uiState.update { it.copy(errorMessage = "${visionProvider.displayName} API key is required. Please set your API key in Model Settings.") }
            return
        }

        val analyzeStartTime = com.snehonir.lipirx.data.local.getPlatformEpochMillis()

        _uiState.update {
            it.copy(
                isLoading = true,
                loadingStep = 0,
                loadingStatusText = "Encoding image & payload...",
                errorMessage = null,
                capturedImageByteArray = imageByteArray
            )
        }
        Napier.d("[$flow] ScanUiState updated: isLoading=true, visionProvider=${visionProvider.id}, visionModel=$visionModel", tag = TAG)

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            Napier.d("[$flow] Executing Multiagent Pipeline with Stage 1 Vision provider '${visionProvider.id}' & model '$visionModel'...", tag = TAG)

            val input = TranscriptionInput(
                imageByteArray = imageByteArray,
                selectedModel = visionModel,
                provider = visionProvider,
                providedApiKey = visionApiKey,
                customUrl = customUrl,
                onProgress = { step, statusText ->
                    Napier.d("[$flow -> Progress] Step $step: $statusText", tag = TAG)
                    _uiState.update {
                        it.copy(
                            loadingStep = step,
                            loadingStatusText = statusText
                        )
                    }
                }
            )

            val result = orchestrator.executePipeline(
                input = input,
                agentConfigs = state.agentConfigs,
                onProgress = { step ->
                    _uiState.update {
                        it.copy(
                            loadingStep = step.stepIndex,
                            loadingStatusText = step.statusText
                        )
                    }
                }
            )

            result.fold(
                onSuccess = { orchResult ->
                    _uiState.update {
                        it.copy(
                            loadingStep = 4,
                            loadingStatusText = if (orchResult.isSuspendedForApproval) "⚠️ Pharmacist Sign-Off Required" else "✓ Analysis Complete! Extracting medicines..."
                        )
                    }

                    delay(1000L.milliseconds)

                    val immutableList = orchResult.items.toImmutableList()
                    Napier.d("[$flow -> Success] Received ${orchResult.items.size} medicine items (SuspendedForApproval=${orchResult.isSuspendedForApproval})", tag = TAG)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            medicines = immutableList,
                            isSuspendedForApproval = orchResult.isSuspendedForApproval,
                            approvalReason = orchResult.approvalReason.ifBlank { null },
                            errorMessage = null
                        )
                    }

                    runCatching {
                        val dbFlow = "$flow -> LipiRxLocalDatabase.saveSummary"
                        val summary = orchResult.items.joinToString(separator = "; ") { item ->
                            "${item.formattedFullName} (${item.genericName}) [${item.schedule}] Qty: ${item.formattedQuantity}"
                        }
                        database.saveSummary(summary, timestampMillis = analyzeStartTime, flow = dbFlow)
                        database.saveDispensedHistory(orchResult.items, flow = dbFlow)
                    }.onFailure { dbEx ->
                        Napier.w("[$flow -> Warning] Database save failed: ${dbEx.message}", throwable = dbEx, tag = TAG)
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to extract prescription data."
                    Napier.e("[$flow -> Failure] Prescription processing failed: $errorMsg", throwable = error, tag = TAG)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            )
        }
    }

    fun clearResults(flow: String = "App -> ScanViewModel.clearResults") {
        Napier.d("[$flow] Resetting results while preserving API models list and selected model", tag = TAG)
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                capturedImageByteArray = null,
                medicines = persistentListOf(),
                selectedHistoryItem = null,
                isSuspendedForApproval = false,
                approvalReason = null,
                errorMessage = null
            )
        }
    }

    fun clearSelectedHistoryRecord(flow: String = "App -> ScanViewModel.clearSelectedHistoryRecord") {
        Napier.d("[$flow] Clearing selected history record", tag = TAG)
        _uiState.update { state ->
            state.copy(
                selectedHistoryItem = null,
                medicines = persistentListOf()
            )
        }
    }

    fun approveAndResumeAnalysis(flow: String = "App -> ScanViewModel.approveAndResumeAnalysis") {
        Napier.d("[$flow] Pharmacist signed off and approved prescription summary.", tag = TAG)
        val currentMedicines = _uiState.value.medicines
        _uiState.update {
            it.copy(
                isSuspendedForApproval = false,
                approvalReason = null
            )
        }
        runCatching {
            database.saveDispensedHistory(currentMedicines, flow = "$flow -> LipiRxLocalDatabase.saveDispensedHistory")
        }
        viewModelScope.launch {
            _uiEvent.send(ScanUiEvent.ShowToast("Pharmacist sign-off complete. Dispensing verified."))
        }
    }

    fun copyToClipboard(flow: String = "App -> ScanViewModel.copyToClipboard"): String {
        val medicines = _uiState.value.medicines
        Napier.d("[$flow] Generating text summary for ${medicines.size} medicine items", tag = TAG)

        if (medicines.isEmpty()) {
            Napier.w("[$flow -> Warning] No medicines available to copy", tag = TAG)
            return ""
        }

        val textSummary = buildString {
            appendLine("--- Lipi Rx Prescription Summary ---")
            medicines.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.formattedFullName}")
                if (item.genericName.isNotBlank()) appendLine("   Generic: ${item.genericName}")
                appendLine("   Schedule: ${item.schedule}")
                appendLine("   Total Qty: ${item.formattedQuantity}")
                if (item.similarBrand.isNotBlank()) appendLine("   Similar Brand: ${item.similarBrand}")
            }
        }

        viewModelScope.launch {
            Napier.d("[$flow] Emitting ScanUiEvent.CopyToClipboardSuccess side effect", tag = TAG)
            _uiEvent.send(ScanUiEvent.CopyToClipboardSuccess)
        }
        return textSummary
    }

    fun getLocalHistoryCount(flow: String = "App -> ScanViewModel.getLocalHistoryCount"): Int {
        val dbFlow = "$flow -> LipiRxLocalDatabase.getHistory"
        val count = runCatching { database.getHistory(flow = dbFlow).size }.getOrDefault(0)
        Napier.d("[$flow -> Result] Local prescription history count: $count", tag = TAG)
        return count
    }

    fun loadHistoryList(flow: String = "App -> ScanViewModel.loadHistoryList") {
        Napier.d("[$flow] Querying SQLite for prescription scan history records...", tag = TAG)
        val historyRecords = runCatching {
            database.getHistory(flow = "$flow -> LipiRxLocalDatabase.getHistory")
        }.getOrDefault(emptyList()).toImmutableList()

        _uiState.update { it.copy(historyList = historyRecords) }
    }

    fun deleteHistoryRecord(id: Long, flow: String = "App -> ScanViewModel.deleteHistoryRecord") {
        Napier.d("[$flow] Deleting single history record ID=$id", tag = TAG)
        database.deleteHistoryRecord(id, flow = flow)
        loadHistoryList(flow = "$flow -> loadHistoryList")
        viewModelScope.launch {
            _uiEvent.send(ScanUiEvent.ShowToast("History item deleted."))
        }
    }

    fun clearAllHistory(flow: String = "App -> ScanViewModel.clearAllHistory") {
        Napier.d("[$flow] Clearing all prescription history records", tag = TAG)
        database.clearAllHistory(flow = flow)
        loadHistoryList(flow = "$flow -> loadHistoryList")
        viewModelScope.launch {
            _uiEvent.send(ScanUiEvent.ShowToast("Scan history cleared."))
        }
    }

    fun openHistoryRecord(
        record: PrescriptionHistoryEntity,
        flow: String = "App -> ScanViewModel.openHistoryRecord"
    ) {
        Napier.d("[$flow] Opening history record ID=${record.id}, scannedAt=${record.formattedDateTime}", tag = TAG)
        val items = parseSummaryToMedicineItems(record.medicineSummary)
        _uiState.update {
            it.copy(
                medicines = items.toImmutableList(),
                selectedHistoryItem = record,
                capturedImageByteArray = null,
                isLoading = false,
                isSuspendedForApproval = false,
                approvalReason = null,
                errorMessage = null
            )
        }
    }

    private fun parseSummaryToMedicineItems(summaryText: String): List<MedicineItem> {
        if (summaryText.isBlank()) return emptyList()
        return summaryText.split("; ").mapIndexed { _, part ->
            val clean = part.trim()
            val namePart = clean.substringBefore(" (").trim().ifBlank { clean.substringBefore(" [").trim() }
            val genericPart = if (clean.contains("(") && clean.contains(")")) clean.substringAfter("(").substringBefore(")").trim() else ""
            val schedulePart = if (clean.contains("[") && clean.contains("]")) clean.substringAfter("[").substringBefore("]").trim() else ""
            val qtyPart = if (clean.contains("Qty: ")) clean.substringAfter("Qty: ").trim() else "1 pcs"
            val units = qtyPart.substringBefore(" ").toIntOrNull() ?: 1
            val unitType = qtyPart.substringAfter(" ", "pcs").trim()

            MedicineItem(
                medicineName = namePart,
                genericName = genericPart,
                schedule = schedulePart,
                units = units,
                unitType = unitType,
                confidence = "high",
                brandRecognized = true
            )
        }
    }

    companion object {
        private const val TAG = "ScanViewModel"
    }
}
