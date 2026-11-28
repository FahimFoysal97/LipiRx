package com.snehonir.lipirx.di

import androidx.room3.RoomDatabase
import com.snehonir.lipirx.data.local.LipiRxRoomDatabase
import com.snehonir.lipirx.data.local.LipiRxRoomRepository
import com.snehonir.lipirx.data.local.getRoomDatabase
import com.snehonir.lipirx.data.remote.service.ClaudeAiService
import com.snehonir.lipirx.data.remote.service.CompositeAiService
import com.snehonir.lipirx.data.remote.service.CustomAiService
import com.snehonir.lipirx.data.remote.service.LipiRxAiService
import com.snehonir.lipirx.data.remote.service.GeminiAiService
import com.snehonir.lipirx.data.remote.service.GroqAiService
import com.snehonir.lipirx.data.remote.service.OnDeviceAiService
import com.snehonir.lipirx.data.remote.service.OpenAiAiService
import com.snehonir.lipirx.domain.agent.RxSupervisorAgent
import com.snehonir.lipirx.domain.agent.SelfHealingAgentExecutor
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.agent.koog.RxAgentTools
import com.snehonir.lipirx.domain.eval.RxAgentEvaluator
import com.snehonir.lipirx.domain.mcp.LipiRxMcpServer
import com.snehonir.lipirx.domain.mcp.McpResourceHandler
import com.snehonir.lipirx.domain.mcp.McpToolHandler
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.session.PrescriptionSessionState
import com.snehonir.lipirx.domain.usecase.DeleteScanHistoryUseCase
import com.snehonir.lipirx.domain.usecase.FetchAvailableModelsUseCase
import com.snehonir.lipirx.domain.usecase.FormatMedicineSummaryUseCase
import com.snehonir.lipirx.domain.usecase.GetAgentConfigsUseCase
import com.snehonir.lipirx.domain.usecase.GetProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import com.snehonir.lipirx.domain.usecase.LaunchPrescriptionPipelineUseCase
import com.snehonir.lipirx.domain.usecase.ManageMcpServerUseCase
import com.snehonir.lipirx.domain.usecase.ManageOnDeviceStrategyUseCase
import com.snehonir.lipirx.domain.usecase.ProcessPrescriptionUseCase
import com.snehonir.lipirx.domain.usecase.SaveAgentConfigUseCase
import com.snehonir.lipirx.domain.usecase.SaveProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.SaveScanSummaryUseCase
import com.snehonir.lipirx.domain.update.UpdateManager
import com.snehonir.lipirx.presentation.history.HistoryViewModel
import com.snehonir.lipirx.presentation.home.HomeViewModel
import com.snehonir.lipirx.presentation.preview.ImagePreviewViewModel
import com.snehonir.lipirx.presentation.results.ScanResultsViewModel
import com.snehonir.lipirx.presentation.settings.SettingsViewModel
import com.snehonir.lipirx.presentation.telemetry.AgentTraceViewModel
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single {
        val json: Json = get()
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(sanitizeLogMessage(message), tag = "KtorClient")
                    }
                }
                level = LogLevel.INFO
                sanitizeHeader { header ->
                    header.equals("Authorization", ignoreCase = true) ||
                            header.contains("api-key", ignoreCase = true) ||
                            header.contains("key", ignoreCase = true) ||
                            header.contains("auth", ignoreCase = true) ||
                            header.contains("token", ignoreCase = true)
                }
            }
        }
    }
}

private val databaseModule = module {
    single<LipiRxRoomDatabase> {
        val builder: RoomDatabase.Builder<LipiRxRoomDatabase> = get()
        getRoomDatabase(builder)
    }

    single<PrescriptionRepository> {
        LipiRxRoomRepository(database = get())
    }
}

private val aiAgentModule = module {
    single<CompositeAiService> {
        val client: HttpClient = get()
        val json: Json = get()
        val repository: PrescriptionRepository = get()

        val services = mapOf<AiProvider, LipiRxAiService>(
            AiProvider.Gemini to GeminiAiService(httpClient = client, jsonParser = json),
            AiProvider.Groq to GroqAiService(httpClient = client, jsonParser = json),
            AiProvider.OpenAi to OpenAiAiService(httpClient = client, jsonParser = json),
            AiProvider.Claude to ClaudeAiService(httpClient = client, jsonParser = json),
            AiProvider.Custom to CustomAiService(httpClient = client, jsonParser = json),
            AiProvider.OnDevice to OnDeviceAiService(jsonParser = json, repository = repository)
        )

        CompositeAiService(services)
    }

    single { SelfHealingAgentExecutor(maxRetries = 2) }
    single { RxSupervisorAgent(aiService = get(), jsonParser = get()) }
    single { RxAgentTools(aiService = get(), jsonParser = get()) }
    single {
        KoogRxSupervisorEngine(
            rxSupervisorAgent = get(),
            rxAgentTools = get(),
            repository = get(),
            selfHealingExecutor = get()
        )
    }
    single { RxAgentEvaluator(supervisorEngine = get()) }
}

private val mcpModule = module {
    single { McpResourceHandler(repository = get(), jsonParser = get()) }
    single {
        McpToolHandler(
            rxAgentTools = get(),
            repository = get(),
            rxAgentEvaluator = get(),
            jsonParser = get()
        )
    }
    single {
        LipiRxMcpServer(
            resourceHandler = get(),
            toolHandler = get(),
            jsonParser = get()
        )
    }
}

private val domainModule = module {
    single { ProcessPrescriptionUseCase(koogSupervisorEngine = get()) }
    single { GetScanHistoryUseCase(repository = get()) }
    single { SaveScanSummaryUseCase(repository = get()) }
    single { DeleteScanHistoryUseCase(repository = get()) }
    single { FetchAvailableModelsUseCase(aiService = get()) }
    single { GetProviderConfigUseCase(repository = get()) }
    single { SaveProviderConfigUseCase(repository = get()) }
    single { GetAgentConfigsUseCase(repository = get()) }
    single { SaveAgentConfigUseCase(repository = get()) }
    single { ManageOnDeviceStrategyUseCase(repository = get()) }
    single { ManageMcpServerUseCase(repository = get()) }
    single { FormatMedicineSummaryUseCase() }
    single { PrescriptionSessionState() }
    single {
        LaunchPrescriptionPipelineUseCase(
            sessionState = get(),
            processPrescriptionUseCase = get(),
            getAgentConfigsUseCase = get(),
            getProviderConfigUseCase = get(),
            saveScanSummaryUseCase = get(),
            repository = get(),
            jsonParser = get()
        )
    }
    single {
        UpdateManager(
            httpClient = get(),
            platformUpdater = get(),
            repository = get(),
            jsonParser = get()
        )
    }
}

private val presentationModule = module {
    factory { HomeViewModel(sessionState = get(), getScanHistoryUseCase = get()) }
    factory {
        ImagePreviewViewModel(
            sessionState = get(),
            getAgentConfigsUseCase = get(),
            launchPrescriptionPipelineUseCase = get()
        )
    }
    factory {
        ScanResultsViewModel(
            sessionState = get(),
            saveScanSummaryUseCase = get(),
            formatMedicineSummaryUseCase = get(),
            launchPrescriptionPipelineUseCase = get(),
            jsonParser = get()
        )
    }
    factory {
        HistoryViewModel(
            getScanHistoryUseCase = get(),
            deleteScanHistoryUseCase = get(),
            sessionState = get(),
            jsonParser = get()
        )
    }
    single {
        SettingsViewModel(
            getProviderConfigUseCase = get(),
            saveProviderConfigUseCase = get(),
            getAgentConfigsUseCase = get(),
            saveAgentConfigUseCase = get(),
            manageOnDeviceStrategyUseCase = get(),
            manageMcpServerUseCase = get(),
            fetchAvailableModelsUseCase = get(),
            repository = get()
        )
    }
    factory {
        AgentTraceViewModel(
            repository = get(),
            rxAgentEvaluator = get()
        )
    }
}

val sharedModules = listOf(
    networkModule,
    databaseModule,
    aiAgentModule,
    mcpModule,
    domainModule,
    presentationModule
)

val sharedModule = module {
    includes(sharedModules)
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(listOf(platformModule) + sharedModules)
}

fun initKoinIos() {
    Napier.base(DebugAntilog())
    initKoin()
}

/**
 * Sanitizes Ktor log messages to prevent logging sensitive API keys in query parameters,
 * headers, or JSON body payloads.
 */
private fun sanitizeLogMessage(message: String): String {
    if (message.isBlank()) return message
    return message
        .replace(Regex("""([?&]key=)[^&\s"]+""", RegexOption.IGNORE_CASE), "$1***")
        .replace(Regex("""([?&]api_key=)[^&\s"]+""", RegexOption.IGNORE_CASE), "$1***")
        .replace(Regex("""("api_key"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE), "$1***$2")
        .replace(Regex("""("apiKey"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE), "$1***$2")
        .replace(Regex("""("key"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE), "$1***$2")
        .replace(Regex("""(Bearer\s+)[^\s"]+""", RegexOption.IGNORE_CASE), "$1***")
}
