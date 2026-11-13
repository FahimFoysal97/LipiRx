package com.snehonir.lipirx.domain.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class McpRequest(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
data class McpResponse(
    val jsonrpc: String = "2.0",
    val id: String? = null,
    val result: JsonElement? = null,
    val error: McpError? = null
)

@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
) {
    companion object {
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603
    }
}

@Serializable
data class McpServerInfo(
    val name: String = "LipiRx MCP Server",
    val version: String = "1.0.0"
)

@Serializable
data class McpServerCapabilities(
    val resources: Map<String, Boolean> = mapOf("subscribe" to false, "listChanged" to false),
    val tools: Map<String, Boolean> = mapOf("listChanged" to false),
    val prompts: Map<String, Boolean> = mapOf("listChanged" to false)
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String = "2024-11-05",
    val capabilities: McpServerCapabilities = McpServerCapabilities(),
    val serverInfo: McpServerInfo = McpServerInfo()
)

@Serializable
data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

@Serializable
data class McpResourceContents(
    val uri: String,
    val mimeType: String? = "application/json",
    val text: String
)

@Serializable
data class McpResourcesListResult(
    val resources: List<McpResource>
)

@Serializable
data class McpResourceReadResult(
    val contents: List<McpResourceContents>
)

@Serializable
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: JsonElement
)

@Serializable
data class McpToolsListResult(
    val tools: List<McpTool>
)

@Serializable
data class McpToolCallRequest(
    val name: String,
    val arguments: JsonElement? = null
)

@Serializable
data class McpTextContent(
    val type: String = "text",
    val text: String
)

@Serializable
data class McpToolCallResult(
    val content: List<McpTextContent>,
    val isError: Boolean = false
)

@Serializable
data class McpPromptArgument(
    val name: String,
    val description: String? = null,
    val required: Boolean = false
)

@Serializable
data class McpPrompt(
    val name: String,
    val description: String? = null,
    val arguments: List<McpPromptArgument>? = null
)

@Serializable
data class McpPromptsListResult(
    val prompts: List<McpPrompt>
)
