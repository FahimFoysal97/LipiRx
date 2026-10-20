package com.snehonir.lipirx.data.remote

import com.snehonir.lipirx.domain.model.AiProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

class OpenAiAiService(
    httpClient: HttpClient,
    defaultApiKey: String = "",
    jsonParser: Json
) : OpenAiCompatibleAiService(httpClient, defaultApiKey, jsonParser) {

    override val provider: AiProvider = AiProvider.OpenAi
    override val modelsEndpoint: String = "https://api.openai.com/v1/models"
    override val completionsEndpoint: String = "https://api.openai.com/v1/chat/completions"
}
