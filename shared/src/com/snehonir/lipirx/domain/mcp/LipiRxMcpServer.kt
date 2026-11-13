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
        val request = try {
            jsonParser.decodeFromString<McpRequest>(rawJsonRequest)
        } catch (ex: Throwable) {
            Napier.e("[$TAG -> Parse Error] Invalid JSON-RPC request syntax", throwable = ex, tag = TAG)
            val errorResponse = McpResponse(
                id = null,
                error = McpError(code = McpError.PARSE_ERROR, message = "Parse error: ${ex.message}")
            )
            return jsonParser.encodeToString(errorResponse)
        }

        val id = request.id
        val method = request.method.trim()

        return try {
            val response = when (method) {
                METHOD_INITIALIZE -> {
                    val initResult = McpInitializeResult()
                    McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpInitializeResult.serializer(), initResult))
                }
                METHOD_TOOLS_LIST -> {
                    val toolsList = McpToolsListResult(tools = toolHandler.listTools())
                    McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpToolsListResult.serializer(), toolsList))
                }
                METHOD_TOOLS_CALL -> {
                    val paramsObj = request.params?.let { jsonParser.decodeFromJsonElement(McpToolCallRequest.serializer(), it) }
                        ?: throw IllegalArgumentException("Missing required params for tools/call")
                    val toolResult = toolHandler.executeTool(paramsObj).getOrThrow()
                    McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpToolCallResult.serializer(), toolResult))
                }
                METHOD_RESOURCES_LIST -> {
                    val resourcesList = McpResourcesListResult(resources = resourceHandler.listResources())
                    McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpResourcesListResult.serializer(), resourcesList))
                }
                METHOD_RESOURCES_READ -> {
                    val paramsObj = request.params as? JsonObject
                    val uri = paramsObj?.get("uri")?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("Missing required parameter 'uri' for resources/read")
                    val readResult = resourceHandler.readResource(uri).getOrThrow()
                    val resultObj = McpResourceReadResult(contents = listOf(readResult))
                    McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpResourceReadResult.serializer(), resultObj))
                }
                METHOD_PROMPTS_LIST -> {
                    val promptsList = McpPromptsListResult(
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
                    McpResponse(id = id, result = jsonParser.encodeToJsonElement(McpPromptsListResult.serializer(), promptsList))
                }
                else -> {
                    McpResponse(
                        id = id,
                        error = McpError(code = McpError.METHOD_NOT_FOUND, message = "Method '$method' not found")
                    )
                }
            }

            jsonParser.encodeToString(response)
        } catch (ex: Throwable) {
            Napier.e("[$TAG -> Execution Error] Request handling failed for method '$method': ${ex.message}", throwable = ex, tag = TAG)
            val errorResponse = McpResponse(
                id = id,
                error = McpError(code = McpError.INTERNAL_ERROR, message = "Internal error: ${ex.message}")
            )
            jsonParser.encodeToString(errorResponse)
        }
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
