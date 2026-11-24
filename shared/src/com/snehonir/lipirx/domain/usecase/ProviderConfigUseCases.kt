package com.snehonir.lipirx.domain.usecase

import com.snehonir.lipirx.data.local.entity.ProviderConfigEntity
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.common.LipiRxNoParams
import com.snehonir.lipirx.domain.common.LipiRxSuspendUseCase
import com.snehonir.lipirx.domain.mcp.McpServerController
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.OnDeviceStrategy
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.domain.repository.PrescriptionRepository

// ── Read Provider Configuration ──────────────────────────────────────────────
class GetProviderConfigUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<LipiRxNoParams, ProviderConfigEntity> {

    override suspend fun invoke(params: LipiRxNoParams): Result<ProviderConfigEntity> {
        val flow = GetProviderConfigUseCase::class.simpleName ?: ""
        return repository.safeDbCall(flow) {
            repository.getProviderConfig(flow)
        }
    }
}

// ── Save Provider Credentials & URLs ─────────────────────────────────────────
data class SaveProviderConfigParams(
    val providerType: String,
    val customUrl: String,
    val apiKey: String,
    val modelName: String,
    val themeMode: String = ThemeMode.SYSTEM.id,
    val flow: String = ""
)

class SaveProviderConfigUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<SaveProviderConfigParams, Unit> {

    override suspend fun invoke(params: SaveProviderConfigParams): Result<Unit> {
        return repository.safeDbCall(params.flow) {
            repository.saveProviderConfig(
                providerType = params.providerType,
                customUrl = params.customUrl,
                apiKey = params.apiKey,
                modelName = params.modelName,
                themeMode = params.themeMode,
                flow = params.flow
            )
        }
    }
}

// ── Read Per-Agent AI Assignments Matrix ─────────────────────────────────────
class GetAgentConfigsUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<LipiRxNoParams, Map<RxAgentType, AgentConfig>> {

    override suspend fun invoke(params: LipiRxNoParams): Result<Map<RxAgentType, AgentConfig>> {
        val flow = GetAgentConfigsUseCase::class.simpleName ?: ""
        return repository.safeDbCall(flow) {
            repository.getAllAgentConfigs()
        }
    }
}

// ── Save Per-Agent AI Assignments ────────────────────────────────────────────
data class SaveAgentConfigParams(
    val agentType: RxAgentType,
    val provider: AiProvider,
    val modelName: String = "",
    val fallbackProvider: AiProvider? = null,
    val fallbackModel: String = "",
    val flow: String = ""
)

class SaveAgentConfigUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<SaveAgentConfigParams, Unit> {

    override suspend fun invoke(params: SaveAgentConfigParams): Result<Unit> {
        return repository.safeDbCall(params.flow) {
            repository.saveAgentConfig(
                agentType = params.agentType,
                provider = params.provider,
                modelName = params.modelName,
                fallbackProvider = params.fallbackProvider,
                fallbackModel = params.fallbackModel,
                flow = params.flow
            )
        }
    }
}

// ── Manage On-Device Strategies & SLM Folder ──────────────────────────────────
data class StrategyParams(
    val strategy: OnDeviceStrategy? = null,
    val slmDirectoryPath: String? = null,
    val flow: String = ""
)

class ManageOnDeviceStrategyUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<StrategyParams, Unit> {

    override suspend fun invoke(params: StrategyParams): Result<Unit> {
        return repository.safeDbCall(params.flow) {
            if (params.strategy != null) {
                repository.saveOnDeviceStrategy(params.strategy, params.flow)
            }
            if (params.slmDirectoryPath != null) {
                repository.saveSlmModelDirectory(params.slmDirectoryPath, params.flow)
            }
        }
    }
}

// ── Manage Embedded MCP HTTP Listener ─────────────────────────────────────────
data class McpServerParams(
    val enabled: Boolean,
    val flow: String = ""
)

class ManageMcpServerUseCase(
    private val repository: PrescriptionRepository
) : LipiRxSuspendUseCase<McpServerParams, Unit> {

    override suspend fun invoke(params: McpServerParams): Result<Unit> {
        return repository.safeDbCall(params.flow) {
            repository.saveMcpServerEnabled(params.enabled, params.flow)
            McpServerController.setEnabled(params.enabled)
        }
    }
}
