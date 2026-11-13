package com.snehonir.lipirx.domain.mcp

import io.github.aakira.napier.Napier

object McpServerController {
    var onToggleListener: ((Boolean) -> Unit)? = null

    fun setEnabled(enabled: Boolean) {
        Napier.d("[McpServerController] MCP Server lifecycle toggle triggered: enabled = $enabled", tag = "McpServerController")
        onToggleListener?.invoke(enabled)
    }
}
