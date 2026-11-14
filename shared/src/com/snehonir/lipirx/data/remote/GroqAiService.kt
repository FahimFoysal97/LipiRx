package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.model.AiProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

class GroqAiService(
    httpClient: HttpClient,
    defaultApiKey: String = "",
    jsonParser: Json
) : OpenAiCompatibleAiService(httpClient, defaultApiKey, jsonParser) {

    override val provider: AiProvider = AiProvider.Groq
    override val modelsEndpoint: String = AppConstants.GROQ_DEFAULT_ENDPOINT
    override val completionsEndpoint: String = AppConstants.GROQ_CHAT_COMPLETIONS_ENDPOINT
}
