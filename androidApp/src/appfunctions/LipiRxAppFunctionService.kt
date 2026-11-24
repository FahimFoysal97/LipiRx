package com.snehonir.lipirx.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.snehonir.lipirx.data.local.entity.PrescriptionHistoryEntity
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.agent.koog.RxAgentTools
import com.snehonir.lipirx.domain.agent.koog.dto.ToolExecutionParams
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.usecase.GetHistoryParams
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Represents a single prescription scan history entry.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PrescriptionHistoryItem(
    /** The unique database ID of the prescription history entry. */
    val id: Long,
    /** Summary text of the scanned prescription and prescribed medicines. */
    val summaryText: String,
    /** Timestamp in milliseconds when the prescription was recorded. */
    val timestampMillis: Long
)

/**
 * Result payload containing a list of prescription scan history records.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PrescriptionHistoryResult(
    /** List of prescription scan history entries. */
    val items: List<PrescriptionHistoryItem>
)

/**
 * Represents a medicine item structured for AppFunctions integration.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionMedicineItem(
    /** The brand or medicine name. */
    val medicineName: String,
    /** Active generic salt name. */
    val genericName: String,
    /** Dosage form such as tablet, capsule, or syrup. */
    val dosageForm: String,
    /** Strength numerical value such as 500. */
    val strengthValue: String,
    /** Strength unit such as mg or ml. */
    val strengthUnit: String,
    /** Administration schedule such as 1+0+1. */
    val schedule: String,
    /** Total unit quantity prescribed. */
    val units: Int,
    /** List of recommended Bangladeshi generic substitute brand names. */
    val similarBrands: List<String>,
    /** Flag indicating if pharmacist clinical review is recommended. */
    val needsReview: Boolean,
    /** Clinical warning or suspected misreading message. */
    val possibleMisreading: String,
    /** Audit confidence level: high, medium, or low. */
    val confidence: String
)

/**
 * Result payload for medical entity parsing.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ParsedEntitiesResult(
    /** List of parsed structured medicine items. */
    val medicines: List<AppFunctionMedicineItem>
)

/**
 * Result payload for generic substitute search.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class GenericSubstituteResult(
    /** List of medicine items enriched with generic active ingredients and brand substitutes. */
    val medicines: List<AppFunctionMedicineItem>
)

/**
 * Result payload for patient safety auditing.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SafetyAuditResult(
    /** The audited medicine item. */
    val medicine: AppFunctionMedicineItem,
    /** True if clinical audit flags this item for pharmacist review. */
    val needsReview: Boolean,
    /** Description of suspected dosage misreading if flagged. */
    val possibleMisreading: String,
    /** Safety audit confidence rating: high, medium, or low. */
    val confidence: String
)

/**
 * Represents an AgentOps forensic execution trace record.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AgentTraceItem(
    /** Unique database ID of the execution trace. */
    val id: Long,
    /** Supervisor turn loop index. */
    val turnIndex: Int,
    /** Agent identifier string. */
    val agentType: String,
    /** Supervisor decision action or provider name. */
    val decision: String,
    /** Agent reasoning explanation text. */
    val reasoning: String,
    /** Execution timestamp in milliseconds. */
    val timestampMillis: Long
)

/**
 * Result payload containing AgentOps execution traces.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AgentTraceResult(
    /** List of agent execution trace records. */
    val traces: List<AgentTraceItem>
)

private fun MedicineItem.toAppFunctionMedicineItem(): AppFunctionMedicineItem {
    return AppFunctionMedicineItem(
        medicineName = medicineName,
        genericName = genericName,
        dosageForm = dosageForm,
        strengthValue = strengthValue,
        strengthUnit = strengthUnit,
        schedule = schedule,
        units = units,
        similarBrands = similarBrands,
        needsReview = needsReview,
        possibleMisreading = possibleMisreading,
        confidence = confidence
    )
}

/**
 * Entry point for LipiRx AppFunctions integration with system agents and shortcuts.
 */
@RequiresApi(36)
@Suppress("unused")
@AppFunctionServiceEntryPoint(
    serviceName = "LipiRxAppFunctionService",
    appFunctionXmlFileName = "lipirx_app_function_service"
)
abstract class BaseLipiRxAppFunctionService : AppFunctionService(), KoinComponent {

    private val getScanHistoryUseCase: GetScanHistoryUseCase by inject()
    private val rxAgentTools: RxAgentTools by inject()
    private val repository: PrescriptionRepository by inject()

    /**
     * Retrieves the user's prescription scan history and medicine summaries.
     *
     * @return Summary list of prescription scan history records.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getPrescriptionHistory(): PrescriptionHistoryResult = withContext(Dispatchers.IO) {
        val historyList = getScanHistoryUseCase(GetHistoryParams(flow = "AppFunctions -> getPrescriptionHistory")).getOrDefault(emptyList())
        val items = historyList.map { record: PrescriptionHistoryEntity ->
            PrescriptionHistoryItem(
                id = record.id,
                summaryText = record.medicineSummary,
                timestampMillis = record.scannedAt
            )
        }
        PrescriptionHistoryResult(items = items)
    }

    /**
     * Parses raw medical prescription text into structured medicine items with names, strength, forms, and schedules.
     *
     * @param rawText The raw handwritten or typed prescription text to parse into structured entities.
     * @return Structured medicine items extracted from the input text.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun parseMedicalEntities(
        rawText: String
    ): ParsedEntitiesResult = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) {
            throw AppFunctionInvalidArgumentException("rawText parameter cannot be blank.")
        }
        val initialItem = MedicineItem(medicineName = rawText)
        val config = repository.getAgentConfig(RxAgentType.ENTITY_PARSER)
        val apiKey = repository.getApiKeyForProvider(config.provider.id)
        val providerConfig = repository.getProviderConfig()
        val params = ToolExecutionParams(
            provider = config.provider.id,
            model = config.selectedModel,
            apiKey = apiKey,
            customUrl = providerConfig.customUrl
        )
        val result = rxAgentTools.executeEntityParsing(listOf(initialItem), params)
        val updatedItems = result.getOrElse {
            throw AppFunctionAppUnknownException("Failed to parse medical entities: ${it.message}")
        }
        val mapped = updatedItems.map { it.toAppFunctionMedicineItem() }
        ParsedEntitiesResult(medicines = mapped)
    }

    /**
     * Identifies generic active ingredients and searches for Bangladeshi brand substitutes.
     *
     * @param medicineName Name of the medicine or brand to search generic substitutes for.
     * @return Medicine items enriched with generic active ingredients and brand substitutes.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchGenericSubstitutes(
        medicineName: String
    ): GenericSubstituteResult = withContext(Dispatchers.IO) {
        if (medicineName.isBlank()) {
            throw AppFunctionInvalidArgumentException("medicineName parameter cannot be blank.")
        }
        val initialItem = MedicineItem(medicineName = medicineName)
        val config = repository.getAgentConfig(RxAgentType.GENERIC_SUBSTITUTE)
        val apiKey = repository.getApiKeyForProvider(config.provider.id)
        val providerConfig = repository.getProviderConfig()
        val params = ToolExecutionParams(
            provider = config.provider.id,
            model = config.selectedModel,
            apiKey = apiKey,
            customUrl = providerConfig.customUrl
        )
        val result = rxAgentTools.executeGenericSubstituteSearch(listOf(initialItem), params)
        val updatedItems = result.getOrElse {
            throw AppFunctionAppUnknownException("Failed to search generic substitutes: ${it.message}")
        }
        val mapped = updatedItems.map { it.toAppFunctionMedicineItem() }
        GenericSubstituteResult(medicines = mapped)
    }

    /**
     * Audits medicine items for dosage safety, ambiguous schedules, and contraindications.
     *
     * @param medicineName Name of the medicine to audit.
     * @param dosageForm Form of medicine such as tablet, capsule, or syrup.
     * @param schedule Frequency or schedule such as 1+0+1 or once daily.
     * @param units Total number of units prescribed.
     * @return Safety audit report detailing review flags, misreadings, and confidence scores.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun auditPatientSafety(
        medicineName: String,
        dosageForm: String?,
        schedule: String?,
        units: Int
    ): SafetyAuditResult = withContext(Dispatchers.IO) {
        if (medicineName.isBlank()) {
            throw AppFunctionInvalidArgumentException("medicineName parameter cannot be blank.")
        }
        val item = MedicineItem(
            medicineName = medicineName,
            dosageForm = dosageForm ?: "",
            schedule = schedule ?: "",
            units = if (units < 0) 0 else units
        )
        val config = repository.getAgentConfig(RxAgentType.SAFETY_AUDITOR)
        val apiKey = repository.getApiKeyForProvider(config.provider.id)
        val providerConfig = repository.getProviderConfig()
        val params = ToolExecutionParams(
            provider = config.provider.id,
            model = config.selectedModel,
            apiKey = apiKey,
            customUrl = providerConfig.customUrl
        )
        val result = rxAgentTools.executeSafetyAudit(listOf(item), params)
        val updatedItems = result.getOrElse {
            throw AppFunctionAppUnknownException("Failed to audit patient safety: ${it.message}")
        }
        val auditedItem = updatedItems.firstOrNull()?.toAppFunctionMedicineItem()
            ?: throw AppFunctionElementNotFoundException("No audited item returned.")
        SafetyAuditResult(
            medicine = auditedItem,
            needsReview = auditedItem.needsReview,
            possibleMisreading = auditedItem.possibleMisreading,
            confidence = auditedItem.confidence
        )
    }

    /**
     * Queries AgentOps execution traces and supervisor turn logs for debugging.
     *
     * @param limit Maximum number of recent execution traces to retrieve.
     * @return List of agent execution trace records with turn details and timestamps.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getAgentExecutionTraces(
        limit: Int
    ): AgentTraceResult = withContext(Dispatchers.IO) {
        val maxEntries = if (limit <= 0) 10 else limit
        val traces = repository.getAgentTraces(limit = maxEntries, flow = "AppFunctions -> getAgentExecutionTraces")
        val items = traces.map { trace ->
            AgentTraceItem(
                id = trace.id,
                turnIndex = trace.turnNumber,
                agentType = trace.agentType,
                decision = trace.provider,
                reasoning = trace.reasoning,
                timestampMillis = trace.timestamp
            )
        }
        AgentTraceResult(traces = items)
    }
}
