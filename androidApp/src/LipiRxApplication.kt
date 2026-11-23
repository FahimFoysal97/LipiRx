package com.snehonir.lipirx

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.net.toUri
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.di.initKoin
import com.snehonir.lipirx.domain.mcp.LipiRxMcpServer
import com.snehonir.lipirx.domain.mcp.McpServerController
import com.snehonir.lipirx.domain.usecase.ManageProviderConfigUseCase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        registerDynamicShortcuts()

        val httpServer = McpHttpServer(mcpServer = mcpServer, port = AppConstants.DEFAULT_MCP_PORT)
        mcpHttpServer = httpServer

        McpServerController.onToggleListener = { enabled ->
            if (enabled) {
                Napier.i("[$TAG] Starting MCP HTTP Server on port ${AppConstants.DEFAULT_MCP_PORT}...", tag = TAG)
                httpServer.start()
            } else {
                Napier.i("[$TAG] Stopping MCP HTTP Server on port ${AppConstants.DEFAULT_MCP_PORT}...", tag = TAG)
                httpServer.stop()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (manageProviderConfigUseCase.isMcpServerEnabled()) {
                Napier.i("[$TAG] MCP Server enabled in config, starting HTTP listener...", tag = TAG)
                httpServer.start()
            } else {
                Napier.i("[$TAG] MCP Server disabled by default in config.", tag = TAG)
            }
        }
    }

    private fun registerDynamicShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            runCatching {
                val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return

                val historyShortcut = ShortcutInfo.Builder(this, AppConstants.Actions.SHORTCUT_ID_HISTORY)
                    .setShortLabel(getString(R.string.shortcut_scan_history))
                    .setLongLabel(getString(R.string.shortcut_scan_history))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(Intent(Intent.ACTION_VIEW, AppConstants.Actions.URI_FEATURE_HISTORY.toUri()).setPackage(packageName))
                    .build()

                val parseShortcut = ShortcutInfo.Builder(this, AppConstants.Actions.SHORTCUT_ID_PARSE)
                    .setShortLabel(getString(R.string.shortcut_parse_prescription))
                    .setLongLabel(getString(R.string.shortcut_parse_prescription))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(Intent(Intent.ACTION_VIEW, AppConstants.Actions.URI_FEATURE_PARSE.toUri()).setPackage(packageName))
                    .build()

                val genericShortcut = ShortcutInfo.Builder(this, AppConstants.Actions.SHORTCUT_ID_GENERIC)
                    .setShortLabel(getString(R.string.shortcut_generic_substitutes))
                    .setLongLabel(getString(R.string.shortcut_generic_substitutes))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(Intent(Intent.ACTION_VIEW, AppConstants.Actions.URI_FEATURE_GENERIC.toUri()).setPackage(packageName))
                    .build()

                val safetyShortcut = ShortcutInfo.Builder(this, AppConstants.Actions.SHORTCUT_ID_SAFETY)
                    .setShortLabel(getString(R.string.shortcut_safety_audit))
                    .setLongLabel(getString(R.string.shortcut_safety_audit))
                    .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(Intent(Intent.ACTION_VIEW, AppConstants.Actions.URI_FEATURE_SAFETY.toUri()).setPackage(packageName))
                    .build()

                shortcutManager.dynamicShortcuts = listOf(historyShortcut, parseShortcut, genericShortcut, safetyShortcut)
                Napier.i("[$TAG] Published 4 dynamic shortcuts to Android ShortcutManager.", tag = TAG)
            }.onFailure { ex ->
                Napier.w("[$TAG] Failed to register dynamic shortcuts: ${ex.message}", tag = TAG)
            }
        }
    }

    companion object {
        private const val TAG = "LipiRxApplication"
    }
}
