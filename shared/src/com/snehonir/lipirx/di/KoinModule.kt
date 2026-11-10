package com.snehonir.lipirx.di

import com.snehonir.lipirx.data.local.DatabaseDriverFactory
import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.data.remote.CompositeAiService
import com.snehonir.lipirx.data.remote.CustomAiService
import com.snehonir.lipirx.data.remote.GeminiAiService
import com.snehonir.lipirx.data.remote.GroqAiService
import com.snehonir.lipirx.data.remote.OpenAiAiService
import com.snehonir.lipirx.domain.agent.GenericSubstituteAgent
import com.snehonir.lipirx.domain.agent.MedicalEntityParserAgent
import com.snehonir.lipirx.domain.agent.MultiAgentRxOrchestrator
import com.snehonir.lipirx.domain.agent.SafetyAuditorAgent
import com.snehonir.lipirx.domain.agent.VisionTranscriptionAgent
import com.snehonir.lipirx.domain.model.AiProvider
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
            AiProvider.Custom to CustomAiService(httpClient = client, jsonParser = json)
        )

        CompositeAiService(services)
    }

    single {
        val driverFactory: DatabaseDriverFactory = get()
        LipiRxLocalDatabase(driverFactory.createDriver())
    }

    // Multiagent Architecture Components
    single { VisionTranscriptionAgent(aiService = get()) }
    single { MedicalEntityParserAgent() }
    single { GenericSubstituteAgent() }
    single { SafetyAuditorAgent() }

    single {
        MultiAgentRxOrchestrator(
            visionTranscriptionAgent = get(),
            medicalEntityParserAgent = get(),
            genericSubstituteAgent = get(),
            safetyAuditorAgent = get(),
            database = get()
        )
    }

    single { ScanViewModel(aiService = get(), orchestrator = get(), database = get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(platformModule, sharedModule)
}

fun initKoinIos() {
    Napier.base(DebugAntilog())
    initKoin { }
}
