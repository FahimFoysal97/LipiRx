package com.snehonir.lipirx.domain.mcp

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class LipiRxMcpServer(
    private val resourceHandler: McpResourceHandler,
    private val toolHandler: McpToolHandler,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) {
    suspend fun handleJsonRpcRequest(rawJsonRequest: String): String {
        Napier.d("[$TAG] Processing incoming JSON-RPC Request (${rawJsonRequest.length} chars)...", tag = TAG)

        val request = runCatching {
            jsonParser.decodeFromString<McpRequest>(rawJsonRequest)
        }.getOrElse { ex ->
            Napier.e("[$TAG -> Parse Error] Invalid JSON-RPC syntax", throwable = ex, tag = TAG)
            return buildErrorResponse(null, McpError.PARSE_ERROR, "Parse error: ${ex.message}")
        }

        return runCatching {
            dispatchMethod(request)
        }.getOrElse { ex ->
            Napier.e("[$TAG -> Execution Error] Method '${request.method}' failed: ${ex.message}", throwable = ex, tag = TAG)
            buildErrorResponse(request.id, McpError.INTERNAL_ERROR, "Internal error: ${ex.message}")
        }
    }

    private suspend fun dispatchMethod(request: McpRequest): String {
        val id = request.id
        val response = when (request.method.trim()) {
            METHOD_INITIALIZE -> handleInitialize(id)
            METHOD_TOOLS_LIST -> handleToolsList(id)
            METHOD_TOOLS_CALL -> handleToolsCall(id, request)
            METHOD_RESOURCES_LIST -> handleResourcesList(id)
            METHOD_RESOURCES_READ -> handleResourcesRead(id, request)
            METHOD_PROMPTS_LIST -> handlePromptsList(id)
            else -> McpResponse(id = id, error = McpError(McpError.METHOD_NOT_FOUND, "Method '${request.method}' not found"))
        }
        return jsonParser.encodeToString(response)
    }

    private fun handleInitialize(id: String?): McpResponse {
        val result = McpInitializeResult()
        return McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpInitializeResult.serializer(), result))
    }

    private fun handleToolsList(id: String?): McpResponse {
        val result = McpToolsListResult(tools = toolHandler.listTools())
        return McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpToolsListResult.serializer(), result))
    }

    private suspend fun handleToolsCall(id: String?, request: McpRequest): McpResponse {
        val paramsObj = request.params?.let { jsonParser.decodeFromJsonElement(McpToolCallRequest.serializer(), it) }
            ?: throw IllegalArgumentException("Missing required params for tools/call")
        val toolResult = toolHandler.executeTool(paramsObj).getOrThrow()
        return McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpToolCallResult.serializer(), toolResult))
    }

    private fun handleResourcesList(id: String?): McpResponse {
        val result = McpResourcesListResult(resources = resourceHandler.listResources())
        return McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpResourcesListResult.serializer(), result))
    }

    private fun handleResourcesRead(id: String?, request: McpRequest): McpResponse {
        val paramsObj = request.params as? JsonObject
        val uri = paramsObj?.get("uri")?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing required parameter 'uri' for resources/read")
        val readResult = resourceHandler.readResource(uri).getOrThrow()
        val resultObj = McpResourceReadResult(contents = listOf(readResult))
        return McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpResourceReadResult.serializer(), resultObj))
    }

    private fun handlePromptsList(id: String?): McpResponse {
        val result = McpPromptsListResult(
            prompts = listOf(
                McpPrompt(
                    name = "prescription_safety_audit",
                    description = "Clinical prompt template for auditing dosage safety and drug contraindications",
                    arguments = listOf(
                        McpPromptArgument(name = "prescriptionText", description = "Raw prescription text", required = true)
                    )
                )
            )
        )
        return McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpPromptsListResult.serializer(), result))
    }

    private fun buildErrorResponse(id: String?, code: Int, message: String): String {
        val errorResponse = McpResponse(id = id, error = McpError(code = code, message = message))
        return jsonParser.encodeToString(errorResponse)
    }

    companion object {
        private const val TAG = "LipiRxMcpServer"
        const val METHOD_INITIALIZE = "initialize"
        const val METHOD_TOOLS_LIST = "tools/list"
        const val METHOD_TOOLS_CALL = "tools/call"
        const val METHOD_RESOURCES_LIST = "resources/list"
        const val METHOD_RESOURCES_READ = "resources/read"
        const val METHOD_PROMPTS_LIST = "prompts/list"
    }
}
