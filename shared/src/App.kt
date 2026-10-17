package com.snehonir.lipirx

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snehonir.lipirx.presentation.scan.ScanResultScreen
import com.snehonir.lipirx.presentation.scan.ScanViewModel
import org.koin.compose.viewmodel.koinViewModel

object NavigationRoutes {
    const val SCAN = "scan_screen"
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.SCAN
        ) {
            composable(NavigationRoutes.SCAN) {
                val viewModel: ScanViewModel = koinViewModel()
                ScanResultScreen(viewModel = viewModel)
            }
        }
    }
}
