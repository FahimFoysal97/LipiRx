package com.snehonir.lipirx.domain.mcp

import com.snehonir.lipirx.domain.agent.TranscriptionInput
import com.snehonir.lipirx.domain.agent.koog.RxAgentTools
import com.snehonir.lipirx.domain.agent.koog.ToolExecutionParams
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
            )
        )
    }

    suspend fun executeTool(request: McpToolCallRequest): Result<McpToolCallResult> {
        Napier.d("[McpToolHandler] Executing MCP Tool Call: ${request.name}", tag = TAG)
        return try {
            val args = request.arguments?.jsonObject ?: JsonObject(emptyMap())

            val resultText = when (request.name.trim()) {
                TOOL_VISION_OCR -> {
                    val base64Str = args["imageBase64"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing required parameter 'imageBase64'")
                    val imageBytes = Base64.decode(base64Str)
                    val providerStr = args["provider"]?.jsonPrimitive?.content ?: "gemini"
                    val modelStr = args["model"]?.jsonPrimitive?.content ?: ""
                    val apiKeyStr = args["apiKey"]?.jsonPrimitive?.content ?: ""
                    val customUrlStr = args["customUrl"]?.jsonPrimitive?.content ?: ""

                    val input = TranscriptionInput(
                        imageByteArray = imageBytes,
                        selectedModel = modelStr,
                        provider = AiProvider.fromId(providerStr),
                        providedApiKey = apiKeyStr,
                        customUrl = customUrlStr
                    )
                    val result = rxAgentTools.executeVisionOCR(input).getOrThrow()
                    jsonParser.encodeToString(result)
                }
                TOOL_PARSE_ENTITIES -> {
                    val itemsJson = args["itemsJson"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing required parameter 'itemsJson'")
                    val items = jsonParser.decodeFromString<List<MedicineItem>>(itemsJson)
                    val params = parseParams(args)
                    val result = rxAgentTools.executeEntityParsing(items, params).getOrThrow()
                    jsonParser.encodeToString(result)
                }
                TOOL_GENERIC_SUBSTITUTE -> {
                    val itemsJson = args["itemsJson"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing required parameter 'itemsJson'")
                    val items = jsonParser.decodeFromString<List<MedicineItem>>(itemsJson)
                    val params = parseParams(args)
                    val result = rxAgentTools.executeGenericSubstituteSearch(items, params).getOrThrow()
                    jsonParser.encodeToString(result)
                }
                TOOL_SAFETY_AUDIT -> {
                    val itemsJson = args["itemsJson"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing required parameter 'itemsJson'")
                    val items = jsonParser.decodeFromString<List<MedicineItem>>(itemsJson)
                    val params = parseParams(args)
                    val result = rxAgentTools.executeSafetyAudit(items, params).getOrThrow()
                    jsonParser.encodeToString(result)
                }
                else -> throw IllegalArgumentException("Unknown MCP Tool name: '${request.name}'")
            }

            Result.success(
                McpToolCallResult(
                    content = listOf(McpTextContent(text = resultText)),
                    isError = false
                )
            )
        } catch (ex: Throwable) {
            Napier.e("[McpToolHandler -> Error] Tool execution failed for '${request.name}': ${ex.message}", throwable = ex, tag = TAG)
            Result.success(
                McpToolCallResult(
                    content = listOf(McpTextContent(text = "Error executing tool '${request.name}': ${ex.message}")),
                    isError = true
                )
            )
        }
    }

    private fun parseParams(args: JsonObject): ToolExecutionParams {
        return ToolExecutionParams(
            provider = args["provider"]?.jsonPrimitive?.content ?: "gemini",
            model = args["model"]?.jsonPrimitive?.content ?: "",
            apiKey = args["apiKey"]?.jsonPrimitive?.content ?: "",
            customUrl = args["customUrl"]?.jsonPrimitive?.content ?: "",
            feedbackPrompt = args["feedbackPrompt"]?.jsonPrimitive?.content ?: "",
            memoryContext = args["memoryContext"]?.jsonPrimitive?.content ?: ""
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
    }
}
