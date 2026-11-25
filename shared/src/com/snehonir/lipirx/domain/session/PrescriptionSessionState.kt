package com.snehonir.lipirx.domain.session

import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.PipelineEvent
import com.snehonir.lipirx.domain.agent.PipelineProgress
import com.snehonir.lipirx.domain.model.MedicineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionData(
    val imageByteArray: ByteArray? = null,
    val isLoading: Boolean = false,
    val loadingStep: Int = 0,
    val loadingStatusText: String = "",
    val activeAgentProgressPercent: Int = 0,
    val currentAgent: RxAgentType = RxAgentType.SUPERVISOR,
    val progress: PipelineProgress = PipelineProgress(),
    val medicines: ImmutableList<MedicineItem> = persistentListOf(),
    val activeScanMedicines: ImmutableList<MedicineItem> = persistentListOf(),
    val isSuspendedForApproval: Boolean = false,
    val approvalReason: String? = null,
    val errorMessage: String? = null,
    val selectedHistoryItem: PrescriptionHistoryEntity? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionData) return false

        if (isLoading != other.isLoading) return false
        if (loadingStep != other.loadingStep) return false
        if (loadingStatusText != other.loadingStatusText) return false
        if (activeAgentProgressPercent != other.activeAgentProgressPercent) return false
        if (currentAgent != other.currentAgent) return false
        if (progress != other.progress) return false
        if (imageByteArray != null) {
            if (other.imageByteArray == null) return false
            if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        } else if (other.imageByteArray != null) return false
        if (medicines != other.medicines) return false
        if (activeScanMedicines != other.activeScanMedicines) return false
        if (isSuspendedForApproval != other.isSuspendedForApproval) return false
        if (approvalReason != other.approvalReason) return false
        if (errorMessage != other.errorMessage) return false
        if (selectedHistoryItem != other.selectedHistoryItem) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + loadingStep.hashCode()
        result = 31 * result + loadingStatusText.hashCode()
        result = 31 * result + activeAgentProgressPercent.hashCode()
        result = 31 * result + currentAgent.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + (imageByteArray?.contentHashCode() ?: 0)
        result = 31 * result + medicines.hashCode()
        result = 31 * result + activeScanMedicines.hashCode()
        result = 31 * result + isSuspendedForApproval.hashCode()
        result = 31 * result + approvalReason.hashCode()
        result = 31 * result + errorMessage.hashCode()
        result = 31 * result + selectedHistoryItem.hashCode()
        return result
    }
}

class PrescriptionSessionState {
    private val _session = MutableStateFlow(SessionData())
    val session: StateFlow<SessionData> = _session.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var activeAnalysisJob: Job? = null

    fun startPipelineAnalysis(block: suspend () -> Unit) {
        cancelActiveAnalysis()
        _session.update { it.copy(isLoading = true, errorMessage = null) }
        activeAnalysisJob = scope.launch {
            block()
        }
    }

    fun cancelActiveAnalysis() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        _session.update {
            it.copy(
                isLoading = false,
                loadingStep = 0,
                loadingStatusText = "",
                activeAgentProgressPercent = 0,
                progress = PipelineProgress()
            )
        }
    }

    /** Entry point for the engine's onEvent callback. */
    fun applyProgress(event: PipelineEvent) {
        _session.update { it.copy(progress = it.progress.reduce(event)) }
    }

    fun setImageBytes(bytes: ByteArray?) {
        _session.update {
            it.copy(
                imageByteArray = bytes,
                errorMessage = null,
                medicines = persistentListOf(),
                activeScanMedicines = persistentListOf(),
                selectedHistoryItem = null
            )
        }
    }

    fun setLoadingState(
        isLoading: Boolean,
        step: Int = 0,
        statusText: String = "",
        progressPercent: Int = 0,
        agent: RxAgentType = RxAgentType.SUPERVISOR
    ) {
        _session.update {
            it.copy(
                isLoading = isLoading,
                loadingStep = step,
                loadingStatusText = statusText,
                activeAgentProgressPercent = progressPercent,
                currentAgent = agent
            )
        }
    }

    fun setResults(
        medicines: List<MedicineItem>,
        isSuspended: Boolean = false,
        approvalReason: String? = null,
        error: String? = null
    ) {
        val immutableList = medicines.toImmutableList()
        _session.update {
            it.copy(
                isLoading = false,
                medicines = immutableList,
                activeScanMedicines = immutableList,
                isSuspendedForApproval = isSuspended,
                approvalReason = approvalReason,
                errorMessage = error
            )
        }
    }

    fun updateMedicineList(medicines: List<MedicineItem>) {
        val immutableList = medicines.toImmutableList()
        _session.update {
            if (it.selectedHistoryItem == null) {
                it.copy(medicines = immutableList, activeScanMedicines = immutableList)
            } else {
                it.copy(medicines = immutableList)
            }
        }
    }

    fun openHistoryRecord(record: PrescriptionHistoryEntity, medicines: List<MedicineItem>) {
        _session.update {
            it.copy(
                activeScanMedicines = if (it.selectedHistoryItem == null) it.medicines else it.activeScanMedicines,
                selectedHistoryItem = record,
                medicines = medicines.toImmutableList(),
                imageByteArray = null,
                isLoading = false,
                isSuspendedForApproval = false,
                approvalReason = null,
                errorMessage = null
            )
        }
    }

    fun clearHistoryRecord() {
        _session.update {
            it.copy(
                selectedHistoryItem = null,
                medicines = it.activeScanMedicines
            )
        }
    }

    fun resetSession() {
        cancelActiveAnalysis()
        _session.update { SessionData() }
    }
}