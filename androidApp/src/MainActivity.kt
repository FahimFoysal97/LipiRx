package com.snehonir.lipirx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.snehonir.lipirx.common.AppConstants
import com.snehonir.lipirx.navigation.AppNavKey
import io.github.aakira.napier.Napier

class MainActivity : ComponentActivity() {

    private var currentNavKey by mutableStateOf<AppNavKey>(AppNavKey.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        currentNavKey = parseNavKeyFromIntent(intent)
        setContent {
            App(initialNavKey = currentNavKey)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentNavKey = parseNavKeyFromIntent(intent)
    }

    private fun parseNavKeyFromIntent(intent: Intent?): AppNavKey {
        if (intent == null) return AppNavKey.Home
        val data = (intent.dataString ?: "").lowercase()
        val feature = (intent.getStringExtra(AppConstants.Actions.EXTRA_FEATURE)
            ?: intent.getStringExtra(AppConstants.Actions.EXTRA_QUERY) ?: "").lowercase()

        Napier.i("[$TAG] Parsing Gemini App Action intent: data='$data', feature='$feature'", tag = TAG)

        return when {
            data.contains(AppConstants.Actions.KEYWORD_HISTORY) || feature.contains(AppConstants.Actions.KEYWORD_HISTORY) -> AppNavKey.History
            data.contains(AppConstants.Actions.KEYWORD_SETTINGS) || feature.contains(AppConstants.Actions.KEYWORD_SETTINGS) -> AppNavKey.Settings
            data.contains(AppConstants.Actions.KEYWORD_TELEMETRY) || feature.contains(AppConstants.Actions.KEYWORD_TELEMETRY) || feature.contains(AppConstants.Actions.KEYWORD_TRACE) -> AppNavKey.Telemetry
            data.contains(AppConstants.Actions.KEYWORD_PARSE) || feature.contains(AppConstants.Actions.KEYWORD_PARSE) || feature.contains(AppConstants.Actions.KEYWORD_SCAN) -> AppNavKey.ImagePreview
            data.contains(AppConstants.Actions.KEYWORD_SAFETY) || feature.contains(AppConstants.Actions.KEYWORD_SAFETY) || feature.contains(AppConstants.Actions.KEYWORD_AUDIT) -> AppNavKey.Results
            else -> AppNavKey.Home
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
