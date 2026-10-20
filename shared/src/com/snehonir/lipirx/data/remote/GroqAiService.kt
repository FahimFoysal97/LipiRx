package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.AiProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

class GroqAiService(
    httpClient: HttpClient,
    defaultApiKey: String = "",
    jsonParser: Json
) : OpenAiCompatibleAiService(httpClient, defaultApiKey, jsonParser) {

    override val provider: AiProvider = AiProvider.Groq
    override val modelsEndpoint: String = "https://api.groq.com/openai/v1/models"
    override val completionsEndpoint: String = "https://api.groq.com/openai/v1/chat/completions"
}
