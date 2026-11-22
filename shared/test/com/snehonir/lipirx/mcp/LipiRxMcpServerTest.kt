package com.snehonir.lipirx.mcp

import com.snehonir.lipirx.domain.mcp.McpError
import com.snehonir.lipirx.domain.mcp.McpRequest
import com.snehonir.lipirx.domain.mcp.McpResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LipiRxMcpServerTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testJsonRpcRequestDeserialization() {
        val requestJson = """{"jsonrpc":"2.0","id":"101","method":"initialize"}"""
        val request = json.decodeFromString<McpRequest>(requestJson)

        assertEquals("2.0", request.jsonrpc)
        assertEquals("101", request.id)
        assertEquals("initialize", request.method)
    }

    @Test
    fun testJsonRpcResponseSerialization() {
        val response = McpResponse(id = "101", error = McpError(code = -32601, message = "Method not found"))
        val jsonString = json.encodeToString(response)

        assertTrue(jsonString.contains("-32601"))
        assertTrue(jsonString.contains("Method not found"))
    }
}
