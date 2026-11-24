package com.snehonir.lipirx.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.app_name
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
fun LipiRxScaffold(
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.app_name),
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "",
    topBarActions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Lipi.Background,
    showTopBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                AppHeader(
                    modifier = Modifier.statusBarsPadding(),
                    onBack = onBack,
                    backContentDescription = backContentDescription,
                    title = title,
                    subtitle = subtitle,
                    trailing = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            topBarActions()
                        }
                    }
                )
            }
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor = containerColor,
        content = content
    )
}
