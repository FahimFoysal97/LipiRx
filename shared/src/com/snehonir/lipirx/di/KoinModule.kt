package com.snehonir.lipirx.di

import com.snehonir.lipirx.data.local.DatabaseDriverFactory
import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.data.remote.CustomAiService
import com.snehonir.lipirx.data.remote.GeminiAiService
import com.snehonir.lipirx.data.remote.GroqAiService
import com.snehonir.lipirx.data.remote.OpenAiAiService
import com.snehonir.lipirx.domain.agent.RxSupervisorAgent
import com.snehonir.lipirx.domain.agent.koog.KoogRxSupervisorEngine
import com.snehonir.lipirx.domain.agent.koog.RxAgentTools
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.repository.PrescriptionRepository
import com.snehonir.lipirx.domain.usecase.GetScanHistoryUseCase
import com.snehonir.lipirx.domain.usecase.ManageProviderConfigUseCase
import com.snehonir.lipirx.domain.usecase.ProcessPrescriptionUseCase
import com.snehonir.lipirx.presentation.scan.ScanViewModel
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

val sharedModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    }

    single {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                connectTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(message, tag = "KtorClient")
                    }
                }
                level = LogLevel.INFO
            }
        }
    }

    // AI Services for supported providers
    single {
        val client: HttpClient = get()
        val json: Json = get()

        val services = mapOf(
            AiProvider.Gemini to GeminiAiService(httpClient = client, jsonParser = json),
            AiProvider.Groq to GroqAiService(httpClient = client, jsonParser = json),
            AiProvider.OpenAi to OpenAiAiService(httpClient = client, jsonParser = json),
            AiProvider.Custom to CustomAiService(httpClient = client, jsonParser = json),
            AiProvider.OnDevice to com.snehonir.lipirx.data.remote.OnDeviceAiService(jsonParser = json)
        )

        CompositeAiService(services)
    }

    single<PrescriptionRepository> {
        val driverFactory: DatabaseDriverFactory = get()
        LipiRxLocalDatabase(driverFactory.createDriver())
    }
    single { get<PrescriptionRepository>() as LipiRxLocalDatabase }

    // JetBrains Koog Multi-Agent Architecture Components
    single { RxSupervisorAgent(aiService = get(), jsonParser = get()) }

    single {
        RxAgentTools(
            aiService = get(),
            jsonParser = get()
        )
    }

    single {
        KoogRxSupervisorEngine(
            rxSupervisorAgent = get(),
            rxAgentTools = get(),
            database = get()
        )
    }

    // Use Cases
    single { ProcessPrescriptionUseCase(koogSupervisorEngine = get(), aiService = get()) }
    single { GetScanHistoryUseCase(repository = get()) }
    single { ManageProviderConfigUseCase(repository = get()) }

    factory {
        ScanViewModel(
            processPrescriptionUseCase = get(),
            getScanHistoryUseCase = get(),
            manageProviderConfigUseCase = get()
        )
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule, sharedModule)
}

fun initKoinIos() {
    Napier.base(DebugAntilog())
    initKoin { }
}
