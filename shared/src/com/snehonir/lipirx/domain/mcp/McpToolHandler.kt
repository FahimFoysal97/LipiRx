package com.snehonir.lipirx.domain.mcp

import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.RxAgentTools
import com.snehonir.lipirx.domain.agent.koog.dto.ToolExecutionParams
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.domain.model.MedicineItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64

class McpToolHandler(
    private val rxAgentTools: RxAgentTools,
    private val database: LipiRxLocalDatabase,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) {
    fun listTools(): List<McpTool> {
        return listOf(
            McpTool(
                name = TOOL_VISION_OCR,
                description = "Encodes handwritten prescription image bytes and executes Multimodal Vision OCR to transcribe text",
                inputSchema = buildSchema(
                    properties = mapOf(
                        "imageBase64" to "Base64 encoded string of prescription image",
                        "provider" to "AI Provider string ('gemini', 'groq', 'openai', 'custom', 'on_device')",
                        "model" to "Selected AI model name",
                        "apiKey" to "API Key (optional if stored in local database)",
                        "customUrl" to "Custom REST URL endpoint (optional)"
                    ),
                    required = listOf("imageBase64")
                )
            ),
            McpTool(
                name = TOOL_PARSE_ENTITIES,
                description = "Parses raw OCR medicine items into normalized brand names, dosage forms, strengths, and schedules",
                inputSchema = buildSchema(
                    properties = mapOf(
                        "itemsJson" to "JSON array string of MedicineItem objects",
                        "provider" to "AI Provider string",
                        "model" to "Selected AI model name",
                        "apiKey" to "API Key (optional)"
                    ),
                    required = listOf("itemsJson")
                )
            ),
            McpTool(
                name = TOOL_GENERIC_SUBSTITUTE,
                description = "Cross-references DGDA generic compositions and matches alternative Bangladeshi brand substitutes",
                inputSchema = buildSchema(
                    properties = mapOf(
                        "itemsJson" to "JSON array string of MedicineItem objects",
                        "provider" to "AI Provider string",
                        "model" to "Selected AI model name"
                    ),
                    required = listOf("itemsJson")
                )
            ),
            McpTool(
                name = TOOL_SAFETY_AUDIT,
                description = "Audits medicine items for dosage safety, interaction risks, unreadable spellings, and sets pharmacist review flags",
                inputSchema = buildSchema(
                    properties = mapOf(
                        "itemsJson" to "JSON array string of MedicineItem objects",
                        "provider" to "AI Provider string",
                        "model" to "Selected AI model name"
                    ),
                    required = listOf("itemsJson")
                )
            ),
            McpTool(
                name = TOOL_GET_TRACES,
                description = "Queries forensic execution traces (supervisor reasoning steps, tool choices, timing, success/error status) from SQLite",
                inputSchema = buildSchema(
                    properties = mapOf(
                        "limit" to "Maximum number of recent trace records to retrieve (default: 50)"
                    ),
                    required = emptyList()
                )
            )
        )
    }

    suspend fun executeTool(request: McpToolCallRequest): Result<McpToolCallResult> {
        Napier.d("[McpToolHandler] Executing MCP Tool Call: ${request.name}", tag = TAG)
        return runCatching {
            val args = request.arguments?.jsonObject ?: JsonObject(emptyMap())
            val resultText = dispatchToolExecution(request.name.trim(), args)

            McpToolCallResult(
                content = listOf(McpTextContent(text = resultText)),
                isError = false
            )
        }.recoverCatching { ex ->
            Napier.e("[McpToolHandler -> Error] Tool execution failed for '${request.name}': ${ex.message}", throwable = ex, tag = TAG)
            McpToolCallResult(
                content = listOf(McpTextContent(text = "Error executing tool '${request.name}': ${ex.message}")),
                isError = true
            )
        }
    }

    private suspend fun dispatchToolExecution(toolName: String, args: JsonObject): String {
        return when (toolName) {
            TOOL_VISION_OCR -> executeVisionOcrTool(args)
            TOOL_PARSE_ENTITIES -> executeEntityParsingTool(args)
            TOOL_GENERIC_SUBSTITUTE -> executeGenericSubstituteTool(args)
            TOOL_SAFETY_AUDIT -> executeSafetyAuditTool(args)
            TOOL_GET_TRACES -> executeGetTracesTool(args)
            else -> throw IllegalArgumentException("Unknown MCP Tool name: '$toolName'")
        }
    }

    private suspend fun executeVisionOcrTool(args: JsonObject): String {
        val base64Str = getRequiredArg(args, "imageBase64")
        val imageBytes = Base64.decode(base64Str)
        val input = TranscriptionInput(
            imageByteArray = imageBytes,
            selectedModel = getOptionalArg(args, "model"),
            provider = AiProvider.fromId(getOptionalArg(args, "provider", "gemini")),
            providedApiKey = getOptionalArg(args, "apiKey"),
            customUrl = getOptionalArg(args, "customUrl")
        )
        val result = rxAgentTools.executeVisionOCR(input).getOrThrow()
        return jsonParser.encodeToString(result)
    }

    private suspend fun executeEntityParsingTool(args: JsonObject): String {
        val items = getMedicineItemsArg(args)
        val params = parseParams(args)
        val result = rxAgentTools.executeEntityParsing(items, params).getOrThrow()
        return jsonParser.encodeToString(result)
    }

    private suspend fun executeGenericSubstituteTool(args: JsonObject): String {
        val items = getMedicineItemsArg(args)
        val params = parseParams(args)
        val result = rxAgentTools.executeGenericSubstituteSearch(items, params).getOrThrow()
        return jsonParser.encodeToString(result)
    }

    private suspend fun executeSafetyAuditTool(args: JsonObject): String {
        val items = getMedicineItemsArg(args)
        val params = parseParams(args)
        val result = rxAgentTools.executeSafetyAudit(items, params).getOrThrow()
        return jsonParser.encodeToString(result)
    }

    private fun executeGetTracesTool(args: JsonObject): String {
        val limitStr = getOptionalArg(args, "limit", "50")
        val limit = limitStr.toIntOrNull() ?: 50
        val traces = database.getAgentTraces(limit = limit, flow = "MCP -> executeGetTracesTool")
        return jsonParser.encodeToString(traces)
    }

    private fun getMedicineItemsArg(args: JsonObject): List<MedicineItem> {
        val itemsJson = getRequiredArg(args, "itemsJson")
        return jsonParser.decodeFromString(itemsJson)
    }

    private fun getRequiredArg(args: JsonObject, key: String): String {
        return args[key]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing required parameter '$key'")
    }

    private fun getOptionalArg(args: JsonObject, key: String, default: String = ""): String {
        return args[key]?.jsonPrimitive?.content ?: default
    }

    private fun parseParams(args: JsonObject): ToolExecutionParams {
        return ToolExecutionParams(
            provider = getOptionalArg(args, "provider", "gemini"),
            model = getOptionalArg(args, "model"),
            apiKey = getOptionalArg(args, "apiKey"),
            customUrl = getOptionalArg(args, "customUrl"),
            feedbackPrompt = getOptionalArg(args, "feedbackPrompt"),
            memoryContext = getOptionalArg(args, "memoryContext")
        )
    }

    private fun buildSchema(properties: Map<String, String>, required: List<String>): JsonObject {
        val propsObj = JsonObject(
            properties.mapValues { (_, desc) ->
                JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive(desc)))
            }
        )
        return JsonObject(
            mapOf(
                "type" to JsonPrimitive("object"),
                "properties" to propsObj,
                "required" to JsonArray(required.map { JsonPrimitive(it) })
            )
        )
    }

    companion object {
        private const val TAG = "McpToolHandler"
        const val TOOL_VISION_OCR = "scan_prescription_vision_ocr"
        const val TOOL_PARSE_ENTITIES = "parse_medical_entities"
        const val TOOL_GENERIC_SUBSTITUTE = "search_generic_substitutes"
        const val TOOL_SAFETY_AUDIT = "audit_patient_safety"
        const val TOOL_GET_TRACES = "get_agent_execution_traces"
    }
}
