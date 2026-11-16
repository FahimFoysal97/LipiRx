import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snehonir.lipirx.presentation.scan.ScanResultScreen
import com.snehonir.lipirx.presentation.scan.ScanViewModel
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import org.koin.compose.koinInject

object NavigationRoutes {
    const val SCAN = "scan_screen"
}

@Composable
@Preview
fun App() {
    val viewModel: ScanViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    LipiRxTheme(themeMode = uiState.themeMode) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.SCAN
        ) {
            composable(NavigationRoutes.SCAN) {
                ScanResultScreen(viewModel = viewModel)
            }
        }
    }
}
