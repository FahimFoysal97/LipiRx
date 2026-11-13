package com.snehonir.lipirx

import android.app.Application
import com.snehonir.lipirx.di.initKoin
import com.snehonir.lipirx.domain.mcp.LipiRxMcpServer
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext

class LipiRxApplication : Application() {
    private val mcpServer: LipiRxMcpServer by inject()
    private var mcpHttpServer: McpHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        initKoin {
            androidContext(this@LipiRxApplication)
        }

        mcpHttpServer = McpHttpServer(mcpServer = mcpServer, port = 8080).apply {
            start()
        }
    }
}
