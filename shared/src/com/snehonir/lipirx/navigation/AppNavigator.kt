package com.snehonir.lipirx.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

class AppNavigator(
    startDestination: AppNavKey = AppNavKey.Home
) {
    val backStack: SnapshotStateList<AppNavKey> = mutableStateListOf(startDestination)

    val currentKey: AppNavKey
        get() = backStack.lastOrNull() ?: AppNavKey.Home

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun goTo(destination: AppNavKey) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    fun goBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    fun replaceTop(destination: AppNavKey) {
        if (backStack.isNotEmpty()) {
            backStack.removeLastOrNull()
        }
        backStack.add(destination)
    }

    fun popToHome() {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}

@Composable
fun rememberAppNavigator(
    startDestination: AppNavKey = AppNavKey.Home
): AppNavigator {
    val saver = Saver<AppNavigator, ArrayList<String>>(
        save = { navigator ->
            ArrayList(navigator.backStack.map { it.toRouteString() })
        },
        restore = { savedRoutes ->
            val keys = savedRoutes.map { AppNavKey.fromRouteString(it) }
            AppNavigator(keys.firstOrNull() ?: startDestination).apply {
                backStack.clear()
                backStack.addAll(keys.ifEmpty { listOf(startDestination) })
            }
        }
    )
    return rememberSaveable(saver = saver) {
        AppNavigator(startDestination)
    }
}
