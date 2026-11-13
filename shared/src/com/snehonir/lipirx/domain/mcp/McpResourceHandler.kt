package com.snehonir.lipirx.domain.mcp

import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

class McpResourceHandler(
    private val database: LipiRxLocalDatabase,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) {
    fun listResources(): List<McpResource> {
        return listOf(
            McpResource(
                uri = URI_SCAN_HISTORY,
                name = "Prescription Scan History",
                description = "SQLDelight database records of past scanned prescriptions and extracted items",
                mimeType = "application/json"
            ),
            McpResource(
                uri = URI_PROVIDER_CONFIG,
                name = "AI Provider Configuration",
                description = "Active AI provider configurations and assigned models",
                mimeType = "application/json"
            )
        )
    }

    fun readResource(uri: String): Result<McpResourceContents> {
        Napier.d("[McpResourceHandler] Reading MCP Resource: $uri", tag = TAG)
        return try {
            val contentJson = when (uri.trim()) {
                URI_SCAN_HISTORY -> {
                    val history = database.getHistory("MCP -> readResource")
                    val mapped = history.map { entity ->
                        mapOf(
                            "id" to entity.id,
                            "scannedAt" to entity.scannedAt,
                            "dateTime" to entity.formattedDateTime,
                            "summary" to entity.medicineSummary
                        )
                    }
                    jsonParser.encodeToString(mapped)
                }
                URI_PROVIDER_CONFIG -> {
                    val config = database.getProviderConfig()
                    val mapped = mapOf(
                        "providerType" to config.providerType,
                        "customUrl" to config.customUrl,
                        "hasApiKey" to config.apiKey.isNotBlank(),
                        "modelName" to config.modelName
                    )
                    jsonParser.encodeToString(mapped)
                }
                else -> throw IllegalArgumentException("Unknown MCP Resource URI: '$uri'")
            }

            Result.success(
                McpResourceContents(
                    uri = uri,
                    mimeType = "application/json",
                    text = contentJson
                )
            )
        } catch (ex: Throwable) {
            Napier.e("[McpResourceHandler -> Error] Failed to read resource '$uri': ${ex.message}", throwable = ex, tag = TAG)
            Result.failure(ex)
        }
    }

    companion object {
        private const val TAG = "McpResourceHandler"
        const val URI_SCAN_HISTORY = "lipirx://history/scans"
        const val URI_PROVIDER_CONFIG = "lipirx://config/providers"
    }
}
