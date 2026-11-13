package com.snehonir.lipirx

import com.snehonir.lipirx.domain.mcp.LipiRxMcpServer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

class McpHttpServer(
    private val mcpServer: LipiRxMcpServer,
    private val port: Int = 8080,
    private val endpointPath: String = "/lipirx/mcp"
) {
    private var isRunning = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                Napier.i("[McpHttpServer] Started LipiRx MCP HTTP Server on port $port (http://localhost:$port$endpointPath)", tag = TAG)

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleClient(clientSocket) }
                }
            } catch (ex: Throwable) {
                if (isRunning) {
                    Napier.e("[McpHttpServer -> Error] Server error: ${ex.message}", throwable = ex, tag = TAG)
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        runCatching { serverSocket?.close() }
    }

    private suspend fun handleClient(socket: Socket) {
        runCatching {
            socket.use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = OutputStreamWriter(client.getOutputStream())

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: ""

                var contentLength = 0
                var line: String? = reader.readLine()
                while (!line.isNullOrBlank()) {
                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    line = reader.readLine()
                }

                if (method.equals("OPTIONS", ignoreCase = true)) {
                    writer.write("HTTP/1.1 204 No Content\r\n")
                    writer.write("Access-Control-Allow-Origin: *\r\n")
                    writer.write("Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n")
                    writer.write("Access-Control-Allow-Headers: Content-Type, Authorization\r\n")
                    writer.write("\r\n")
                    writer.flush()
                    return
                }

                val bodyChars = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val read = reader.read(bodyChars, readTotal, contentLength - readTotal)
                    if (read == -1) break
                    readTotal += read
                }
                val requestBody = String(bodyChars, 0, readTotal)

                val responseBody = if (requestBody.isNotBlank()) {
                    mcpServer.handleJsonRpcRequest(requestBody)
                } else {
                    """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Empty request body"}}"""
                }

                val responseBytes = responseBody.toByteArray(Charsets.UTF_8)
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: application/json; charset=utf-8\r\n")
                writer.write("Access-Control-Allow-Origin: *\r\n")
                writer.write("Content-Length: ${responseBytes.size}\r\n")
                writer.write("\r\n")
                writer.flush()

                client.getOutputStream().write(responseBytes)
                client.getOutputStream().flush()
            }
        }.onFailure { ex ->
            Napier.w("[McpHttpServer -> Warning] Client connection error: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    companion object {
        private const val TAG = "McpHttpServer"
    }
}
