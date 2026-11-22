package com.snehonir.lipirx.data.remote.service

import com.snehonir.lipirx.data.remote.prompt.SINGLE_CALL_PROMPT

import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.model.AiProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

class OpenAiAiService(
    httpClient: HttpClient,
    defaultApiKey: String = "",
    jsonParser: Json
) : OpenAiCompatibleAiService(httpClient, defaultApiKey, jsonParser) {

    override val provider: AiProvider = AiProvider.OpenAi
    override val modelsEndpoint: String = AppConstants.OPENAI_DEFAULT_ENDPOINT
    override val completionsEndpoint: String = AppConstants.OPENAI_CHAT_COMPLETIONS_ENDPOINT
}
