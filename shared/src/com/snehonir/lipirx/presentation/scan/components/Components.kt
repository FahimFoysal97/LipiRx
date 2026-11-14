package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.app_name
import com.snehonir.lipirx.resources.header_ai_studio
import com.snehonir.lipirx.resources.header_history
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "",
    title: String = stringResource(Res.string.app_name),
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                Surface(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(1.dp, Lipi.CardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = backContentDescription.ifBlank { stringResource(Res.string.header_history) },
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Ink
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Lipi.Primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Rx", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            if (subtitle != null) {
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Lipi.Muted
                    )
                }
            } else {
                Text(
                    text = title,
                    fontSize = if (onBack != null) 20.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            trailing()
        }
    }
}

@Composable
fun HistoryChip(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(Res.string.header_history),
                modifier = Modifier.size(15.dp),
                tint = Lipi.Primary
            )
            Text(
                text = stringResource(Res.string.header_history),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink
            )
        }
    }
}

@Composable
fun ModelChip(
    selectedModel: String,
    apiKey: String = "",
    geminiApiKey: String = "",
    groqApiKey: String = "",
    openaiApiKey: String = "",
    customApiKey: String = "",
    providerType: String = AiProvider.Gemini.id,
    customUrl: String = "",
    apiKeyError: String? = null,
    apiKeySuccess: String? = null,
    isTestingApiKey: Boolean = false,
    onDeviceStrategy: com.snehonir.lipirx.domain.model.OnDeviceStrategy = com.snehonir.lipirx.domain.model.OnDeviceStrategy.HYBRID_FALLBACK,
    isMcpServerEnabled: Boolean = false,
    onToggleMcpServer: (Boolean) -> Unit = {},
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onSelectOnDeviceStrategy: (com.snehonir.lipirx.domain.model.OnDeviceStrategy) -> Unit = {}
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { showSheet = true },
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Lipi.Success, CircleShape)
            )
            Text(
                text = stringResource(Res.string.header_ai_studio),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink,
                maxLines = 1
            )
        }
    }

    if (showSheet) {
        ModelSelectionBottomSheet(
            selectedModel = selectedModel,
            apiKey = apiKey,
            geminiApiKey = geminiApiKey,
            groqApiKey = groqApiKey,
            openaiApiKey = openaiApiKey,
            customApiKey = customApiKey,
            providerType = providerType,
            customUrl = customUrl,
            apiKeyError = apiKeyError,
            apiKeySuccess = apiKeySuccess,
            isTestingApiKey = isTestingApiKey,
            onDeviceStrategy = onDeviceStrategy,
            isMcpServerEnabled = isMcpServerEnabled,
            onToggleMcpServer = onToggleMcpServer,
            agentConfigs = agentConfigs,
            providerModelsMap = providerModelsMap,
            onAgentProviderSelected = onAgentProviderSelected,
            onAgentModelSelected = onAgentModelSelected,
            onProviderTabSelected = onProviderTabSelected,
            onSaveProviderConfig = onSaveProviderConfig,
            onSelectOnDeviceStrategy = onSelectOnDeviceStrategy,
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        shape = CircleShape,
        color = Lipi.PrimaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Lipi.Primary,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lipi.Primary, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 58.dp,
    contentColor: Color = Lipi.Ink,
    fontSize: Int = 16,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Lipi.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** Decorative prescription sheet used in the home hero and as a loading placeholder. */
@Composable
fun PaperIllustration(
    width: Dp,
    height: Dp,
    rotation: Float = -5f,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .size(width, height)
            .rotate(rotation)
            .shadow(20.dp, shape, ambientColor = Lipi.Ink.copy(0.15f), spotColor = Lipi.Ink.copy(0.2f))
            .background(Color.White, shape)
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("Rx", color = Lipi.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(6.dp))
        listOf(0.85f, 0.65f, 0.76f, 0.52f, 0.8f, 0.58f).forEach { w ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(w)
                    .height(6.dp)
                    .background(Lipi.InkLine, CircleShape)
            )
        }
    }
}
