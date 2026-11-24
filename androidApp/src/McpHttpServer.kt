package com.snehonir.lipirx

import android.util.Base64
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.domain.mcp.LipiRxMcpServer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds

class McpHttpServer(
    private val mcpServer: LipiRxMcpServer,
    private val port: Int = AppConstants.DEFAULT_MCP_PORT,
    private val endpointPath: String = AppConstants.DEFAULT_MCP_PATH
) {
    private var isRunning = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (isRunning) return
        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                Napier.i("[$TAG] Started LipiRx MCP Server on port $port (http://localhost:$port$endpointPath & ws://localhost:$port$endpointPath)", tag = TAG)

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleClient(clientSocket) }
                }
            } catch (ex: Throwable) {
                if (isRunning) {
                    Napier.e("[$TAG -> Error] Server error: ${ex.message}", throwable = ex, tag = TAG)
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
                val input = client.getInputStream()
                val output = client.getOutputStream()

                val headers = parseHttpHeaders(input)
                if (headers.isEmpty()) return

                val requestLine = headers.firstOrNull() ?: ""
                val parts = requestLine.split(" ")
                val method = parts.getOrNull(0) ?: ""

                // Handle CORS OPTIONS preflight
                if (method.equals("OPTIONS", ignoreCase = true)) {
                    sendCorsPreflight(output)
                    return
                }

                // Check for WebSocket Upgrade request
                val isWebSocket = headers.any { it.startsWith("Upgrade: websocket", ignoreCase = true) }
                if (isWebSocket) {
                    val webSocketKey = headers.firstOrNull { it.startsWith("Sec-WebSocket-Key:", ignoreCase = true) }
                        ?.substringAfter(":")?.trim() ?: ""
                    handleWebSocketSession(input, output, webSocketKey)
                    return
                }

                // Handle standard HTTP POST
                handleHttpPostSession(input, output, headers)
            }
        }.onFailure { ex ->
            Napier.w("[$TAG -> Warning] Client connection error: ${ex.message}", throwable = ex, tag = TAG)
        }
    }

    private fun parseHttpHeaders(input: InputStream): List<String> {
        val headers = mutableListOf<String>()
        val lineBuffer = StringBuilder()
        var byteRead: Int

        while (input.read().also { byteRead = it } != -1) {
            val ch = byteRead.toChar()
            if (ch == '\n') {
                val line = lineBuffer.toString().trimEnd('\r')
                if (line.isEmpty()) break
                headers.add(line)
                lineBuffer.clear()
            } else {
                lineBuffer.append(ch)
            }
        }
        return headers
    }

    private fun sendCorsPreflight(output: OutputStream) {
        val response = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: POST, GET, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization, Sec-WebSocket-Key, Sec-WebSocket-Version\r\n" +
                "\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private suspend fun handleHttpPostSession(input: InputStream, output: OutputStream, headers: List<String>) {
        var contentLength = 0
        headers.forEach { line ->
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
        }

        val bodyBytes = ByteArray(contentLength)
        var readTotal = 0
        while (readTotal < contentLength) {
            val read = withContext(Dispatchers.IO) {
                input.read(bodyBytes, readTotal, contentLength - readTotal)
            }
            if (read == -1) break
            readTotal += read
        }
        val requestBody = String(bodyBytes, 0, readTotal, Charsets.UTF_8)

        val responseBody = if (requestBody.isNotBlank()) {
            mcpServer.handleJsonRpcRequest(requestBody)
        } else {
            """{"jsonrpc":"2.0","error":{"code":-32600,"message":"Empty request body"}}"""
        }

        val responseBytes = responseBody.toByteArray(Charsets.UTF_8)
        val headersStr = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: ${AppConstants.MIME_TYPE_JSON}; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: ${responseBytes.size}\r\n" +
                "\r\n"

        withContext(Dispatchers.IO) {
            output.write(headersStr.toByteArray(Charsets.UTF_8))
            output.write(responseBytes)
            output.flush()
        }
    }

    private suspend fun handleWebSocketSession(input: InputStream, output: OutputStream, clientKey: String) {
        val acceptKey = generateWebSocketAcceptKey(clientKey)
        val handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Accept: $acceptKey\r\n" +
                "\r\n"

        withContext(Dispatchers.IO) {
            output.write(handshakeResponse.toByteArray(Charsets.UTF_8))
            output.flush()
        }

        Napier.i("[$TAG -> WebSocket] Client connected to persistent MCP WebSocket session.", tag = TAG)

        val heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(25_000.milliseconds)
                runCatching {
                    writeWebSocketFrame(output, opcode = 0x9, payload = "ping".toByteArray(Charsets.UTF_8))
                }.onFailure { break }
            }
        }

        try {
            while (isRunning) {
                val frame = readWebSocketFrame(input) ?: break
                when (frame.opcode) {
                    0x1 -> { // Text Frame
                        val text = String(frame.payload, Charsets.UTF_8)
                        val responseJson = mcpServer.handleJsonRpcRequest(text)
                        writeWebSocketFrame(output, opcode = 0x1, payload = responseJson.toByteArray(Charsets.UTF_8))
                    }
                    0x9 -> { // Ping Frame -> Send Pong
                        writeWebSocketFrame(output, opcode = 0xA, payload = frame.payload)
                    }
                    0x8 -> break // Close Frame
                }
            }
        } finally {
            heartbeatJob.cancel()
            Napier.i("[$TAG -> WebSocket] Client disconnected from MCP WebSocket session.", tag = TAG)
        }
    }

    private fun generateWebSocketAcceptKey(key: String): String {
        val magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val sha1 = MessageDigest.getInstance("SHA-1").digest((key + magic).toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sha1, Base64.NO_WRAP)
    }

    private fun readWebSocketFrame(input: InputStream): WebSocketFrame? {
        val b0 = input.read()
        if (b0 == -1) return null
        val opcode = b0 and 0x0F

        val b1 = input.read()
        if (b1 == -1) return null
        val masked = (b1 and 0x80) != 0
        var payloadLength = (b1 and 0x7F).toLong()

        if (payloadLength == 126L) {
            val b2 = input.read()
            val b3 = input.read()
            if (b2 == -1 || b3 == -1) return null
            payloadLength = (((b2 and 0xFF) shl 8) or (b3 and 0xFF)).toLong()
        } else if (payloadLength == 127L) {
            var len = 0L
            repeat(8) {
                val b = input.read()
                if (b == -1) return null
                len = (len shl 8) or (b and 0xFF).toLong()
            }
            payloadLength = len
        }

        val maskKey = ByteArray(4)
        if (masked) {
            if (input.read(maskKey, 0, 4) != 4) return null
        }

        val payload = ByteArray(payloadLength.toInt())
        var totalRead = 0
        while (totalRead < payloadLength.toInt()) {
            val read = input.read(payload, totalRead, payloadLength.toInt() - totalRead)
            if (read == -1) break
            totalRead += read
        }

        if (masked) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
        }

        return WebSocketFrame(opcode = opcode, payload = payload)
    }

    private fun writeWebSocketFrame(output: OutputStream, opcode: Int, payload: ByteArray) {
        val length = payload.size
        val header = ByteArray(if (length < 126) 2 else 4)
        header[0] = (0x80 or (opcode and 0x0F)).toByte()

        if (length < 126) {
            header[1] = length.toByte()
            output.write(header, 0, 2)
        } else {
            header[1] = 126.toByte()
            header[2] = ((length shr 8) and 0xFF).toByte()
            header[3] = (length and 0xFF).toByte()
            output.write(header, 0, 4)
        }

        output.write(payload)
        output.flush()
    }

    private data class WebSocketFrame(
        val opcode: Int,
        val payload: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is WebSocketFrame) return false
            return opcode == other.opcode && payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = opcode
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    companion object {
        private val TAG = McpHttpServer::class.simpleName
    }
}
