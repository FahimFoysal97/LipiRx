package com.snehonir.lipirx.domain.mcp

import com.snehonir.lipirx.data.local.LipiRxLocalDatabase
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class McpScanHistoryItemDto(
    val id: Long,
    val scannedAt: Long,
    val dateTime: String,
    val summary: String
)

@Serializable
data class McpProviderConfigItemDto(
    val providerType: String,
    val customUrl: String,
    val hasApiKey: Boolean,
    val modelName: String
)

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
        return runCatching {
            val contentJson = when (uri.trim()) {
                URI_SCAN_HISTORY -> readScanHistory()
                URI_PROVIDER_CONFIG -> readProviderConfig()
                else -> throw IllegalArgumentException("Unknown MCP Resource URI: '$uri'")
            }

            McpResourceContents(
                uri = uri,
                mimeType = "application/json",
                text = contentJson
            )
        }.onFailure { ex ->
            Napier.e("[McpResourceHandler -> Error] Failed to read resource '$uri': ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    private fun readScanHistory(): String {
        val history = database.getHistory("MCP -> readResource")
        val dtos = history.map { entity ->
            McpScanHistoryItemDto(
                id = entity.id,
                scannedAt = entity.scannedAt,
                dateTime = entity.formattedDateTime,
                summary = entity.medicineSummary
            )
        }
        return jsonParser.encodeToString(dtos)
    }

    private fun readProviderConfig(): String {
        val config = database.getProviderConfig()
        val dto = McpProviderConfigItemDto(
            providerType = config.providerType,
            customUrl = config.customUrl,
            hasApiKey = config.apiKey.isNotBlank(),
            modelName = config.modelName
        )
        return jsonParser.encodeToString(dto)
    }

    companion object {
        private const val TAG = "McpResourceHandler"
        const val URI_SCAN_HISTORY = "lipirx://history/scans"
        const val URI_PROVIDER_CONFIG = "lipirx://config/providers"
    }
}
