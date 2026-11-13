package com.snehonir.lipirx

import android.app.Application
import com.snehonir.lipirx.di.initKoin
import com.snehonir.lipirx.domain.mcp.LipiRxMcpServer
import com.snehonir.lipirx.domain.mcp.McpServerController
import com.snehonir.lipirx.domain.usecase.ManageProviderConfigUseCase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext

class LipiRxApplication : Application() {
    private val mcpServer: LipiRxMcpServer by inject()
    private val manageProviderConfigUseCase: ManageProviderConfigUseCase by inject()
    private var mcpHttpServer: McpHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        initKoin {
            androidContext(this@LipiRxApplication)
        }

        val httpServer = McpHttpServer(mcpServer = mcpServer, port = 8080)
        mcpHttpServer = httpServer

        McpServerController.onToggleListener = { enabled ->
            if (enabled) {
                Napier.i("[LipiRxApplication] Starting MCP HTTP Server on port 8080...", tag = "LipiRxApplication")
                httpServer.start()
            } else {
                Napier.i("[LipiRxApplication] Stopping MCP HTTP Server on port 8080...", tag = "LipiRxApplication")
                httpServer.stop()
            }
        }

        if (manageProviderConfigUseCase.isMcpServerEnabled()) {
            Napier.i("[LipiRxApplication] MCP Server enabled in config, starting HTTP listener...", tag = "LipiRxApplication")
            httpServer.start()
        } else {
            Napier.i("[LipiRxApplication] MCP Server disabled by default in config.", tag = "LipiRxApplication")
        }
    }
}
