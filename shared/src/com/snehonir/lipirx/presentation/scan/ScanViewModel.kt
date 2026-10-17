package com.snehonir.lipirx.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.data.remote.GeminiService
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    private val geminiService: GeminiService,
    private val database: LipiRxLocalDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<ScanUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<ScanUiEvent> = _uiEvent.receiveAsFlow()

    init {
        val initialSavedModel = runCatching {
            database.getLastSelectedModel(flow = "App -> ScanViewModel.init.loadSavedModel")
        }.getOrDefault("").removePrefix("models/").trim()

        if (initialSavedModel.isNotBlank()) {
            Napier.d("[ScanViewModel.init] Pre-setting selectedModel to saved model '$initialSavedModel'", tag = TAG)
            _uiState.update { it.copy(selectedModel = initialSavedModel) }
        }

        fetchModels(flow = "App -> ScanViewModel.init")
    }

    fun selectModel(modelName: String, flow: String = "App -> ScanViewModel.selectModel") {
        val cleanModel = modelName.removePrefix("models/").trim()
        Napier.d("[$flow] User selected model: '$cleanModel'. Saving to SQLite...", tag = TAG)
        _uiState.update { it.copy(selectedModel = cleanModel) }

        runCatching {
            database.saveSelectedModel(cleanModel, flow = "$flow -> LipiRxLocalDatabase.saveSelectedModel")
        }.onFailure { ex ->
            Napier.w("[$flow -> Warning] Failed to save selected model: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    fun fetchModels(flow: String = "App -> ScanViewModel.fetchModels") {
        Napier.d("[$flow] Triggering dynamic fetch of available Gemini models from REST API", tag = TAG)
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true) }
            val serviceFlow = "$flow -> GeminiService.fetchAvailableModels"
            val result = geminiService.fetchAvailableModels(flow = serviceFlow)

            result.fold(
                onSuccess = { modelList ->
                    val names = modelList.map { it.name.removePrefix("models/").trim() }.toImmutableList()
                    Napier.d("[$flow -> Success] Retrieved ${names.size} models directly from API: $names", tag = TAG)

                    if (names.isNotEmpty()) {
                        val savedModel = runCatching {
                            database.getLastSelectedModel(flow = "$flow -> LipiRxLocalDatabase.getLastSelectedModel")
                        }.getOrDefault("").removePrefix("models/").trim()

                        _uiState.update { state ->
                            val currentSelected = state.selectedModel.removePrefix("models/").trim()

                            val matchedSaved = names.firstOrNull { it.equals(savedModel, ignoreCase = true) }
                            val matchedCurrent = names.firstOrNull { it.equals(currentSelected, ignoreCase = true) }

                            val chosenModel = when {
                                matchedSaved != null -> matchedSaved
                                matchedCurrent != null -> matchedCurrent
                                else -> names.first()
                            }

                            Napier.d("[$flow] Selected model '$chosenModel' after checking available list (savedWas: '$savedModel')", tag = TAG)

                            state.copy(
                                isFetchingModels = false,
                                availableModels = names,
                                selectedModel = chosenModel
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isFetchingModels = false) }
                    }
                },
                onFailure = { ex ->
                    Napier.w("[$flow -> Warning] Failed to fetch dynamic models list: ${ex.message}", throwable = ex, tag = TAG)
                    _uiState.update { it.copy(isFetchingModels = false) }
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

        val currentModel = _uiState.value.selectedModel

        _uiState.update {
            it.copy(
                isLoading = true,
                loadingStep = 0,
                loadingStatusText = "Encoding image & payload...",
                errorMessage = null,
                capturedImageByteArray = imageByteArray
            )
        }
        Napier.d("[$flow] ScanUiState updated: isLoading=true, selectedModel=$currentModel", tag = TAG)

        viewModelScope.launch {
            val serviceFlow = "$flow -> GeminiService.processPrescriptionImage"
            Napier.d("[$flow] Delegating to GeminiService with model '$currentModel'...", tag = TAG)
            val result = geminiService.processPrescriptionImage(
                imageByteArray = imageByteArray,
                selectedModel = currentModel,
                flow = serviceFlow,
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

            result.fold(
                onSuccess = { items ->
                    // Show all 4 steps checked off on LoadingView
                    _uiState.update {
                        it.copy(
                            loadingStep = 4,
                            loadingStatusText = "✓ Analysis Complete! Extracting medicines..."
                        )
                    }

                    // Brief delay so user sees full completion on screen
                    delay(1000L.milliseconds)

                    val immutableList = items.toImmutableList()
                    Napier.d("[$flow -> Success] Received ${items.size} medicine items from GeminiService", tag = TAG)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            medicines = immutableList,
                            errorMessage = null
                        )
                    }

                    // Save prescription summary to SQLDelight local database
                    runCatching {
                        val dbFlow = "$flow -> LipiRxLocalDatabase.saveSummary"
                        val summary = items.joinToString(separator = "; ") { item ->
                            "${item.formattedFullName} (${item.genericName}) [${item.schedule}] Qty: ${item.formattedQuantity}"
                        }
                        Napier.d("[$flow] Persisting prescription summary into local database...", tag = TAG)
                        database.saveSummary(summary, flow = dbFlow)
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
                    _uiEvent.send(ScanUiEvent.ShowToast("Error: $errorMsg"))
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
                errorMessage = null
            )
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

    companion object {
        private const val TAG = "ScanViewModel"
    }
}
