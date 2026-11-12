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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.agent.AgentConfig
import com.snehonir.lipirx.domain.agent.RxAgentType
import com.snehonir.lipirx.domain.model.AiProvider
import com.snehonir.lipirx.ui.theme.OnDeviceVectorIcon
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.coroutines.launch

val CameraVectorIcon = ImageVector.Builder(
    name = "CameraOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(23f, 19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 21f)
        lineTo(3f, 21f)
        arcTo(2f, 2f, 0f, false, true, 1f, 19f)
        lineTo(1f, 8f)
        arcTo(2f, 2f, 0f, false, true, 3f, 6f)
        lineTo(7f, 6f)
        lineTo(9f, 3f)
        lineTo(15f, 3f)
        lineTo(17f, 6f)
        lineTo(21f, 6f)
        arcTo(2f, 2f, 0f, false, true, 23f, 8f)
        close()
    }
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 17f)
        arcTo(3.5f, 3.5f, 0f, true, false, 12f, 10f)
        arcTo(3.5f, 3.5f, 0f, false, false, 12f, 17f)
        close()
    }
}.build()

val GalleryVectorIcon = ImageVector.Builder(
    name = "GalleryOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        arcTo(2f, 2f, 0f, false, false, 3f, 5f)
        lineTo(3f, 19f)
        arcTo(2f, 2f, 0f, false, false, 5f, 21f)
        lineTo(19f, 21f)
        arcTo(2f, 2f, 0f, false, false, 21f, 19f)
        lineTo(21f, 5f)
        arcTo(2f, 2f, 0f, false, false, 19f, 3f)
        close()
    }
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(8.5f, 10f)
        arcTo(1.5f, 1.5f, 0f, true, false, 8.5f, 7f)
        arcTo(1.5f, 1.5f, 0f, false, false, 8.5f, 10f)
        close()
    }
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(21f, 15f)
        lineTo(16f, 10f)
        lineTo(5f, 21f)
    }
}.build()

val MultiAgentPipelineIcon = ImageVector.Builder(
    name = "MultiAgentPipeline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Primary),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        lineTo(20f, 7f)
        lineTo(20f, 17f)
        lineTo(12f, 22f)
        lineTo(4f, 17f)
        lineTo(4f, 7f)
        close()
        moveTo(12f, 6f)
        lineTo(12f, 18f)
        moveTo(6f, 9f)
        lineTo(18f, 15f)
        moveTo(18f, 9f)
        lineTo(6f, 15f)
    }
}.build()

val CopyVectorIcon = ImageVector.Builder(
    name = "CopyOutline",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Lipi.Ink),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(16f, 1f)
        lineTo(4f, 1f)
        arcTo(2f, 2f, 0f, false, false, 2f, 3f)
        lineTo(2f, 15f)
        moveTo(8f, 5f)
        lineTo(20f, 5f)
        arcTo(2f, 2f, 0f, false, true, 22f, 7f)
        lineTo(22f, 21f)
        arcTo(2f, 2f, 0f, false, true, 20f, 23f)
        lineTo(8f, 23f)
        arcTo(2f, 2f, 0f, false, true, 6f, 21f)
        lineTo(6f, 7f)
        arcTo(2f, 2f, 0f, false, true, 8f, 5f)
        close()
    }
}.build()

val PasteVectorIcon = ImageVector.Builder(
    name = "ContentPaste",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    // Clipboard Board Frame
    path(
        stroke = SolidColor(Lipi.Primary),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 2f)
        lineTo(16f, 2f)
        arcTo(2f, 2f, 0f, false, false, 12f, 0.5f)
        arcTo(2f, 2f, 0f, false, false, 8f, 2f)
        lineTo(5f, 2f)
        arcTo(2f, 2f, 0f, false, false, 3f, 4f)
        lineTo(3f, 20f)
        arcTo(2f, 2f, 0f, false, false, 5f, 22f)
        lineTo(19f, 22f)
        arcTo(2f, 2f, 0f, false, false, 21f, 20f)
        lineTo(21f, 4f)
        arcTo(2f, 2f, 0f, false, false, 19f, 2f)
        close()
    }
    // Clipboard Top Clip
    path(
        fill = SolidColor(Lipi.Primary)
    ) {
        moveTo(9f, 1.8f)
        lineTo(15f, 1.8f)
        lineTo(15f, 5f)
        lineTo(9f, 5f)
        close()
    }
    // Clipboard Document Lines
    path(
        stroke = SolidColor(Lipi.Primary),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(7.5f, 9.5f)
        lineTo(16.5f, 9.5f)
        moveTo(7.5f, 13.5f)
        lineTo(16.5f, 13.5f)
        moveTo(7.5f, 17.5f)
        lineTo(13f, 17.5f)
    }
}.build()

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backContentDescription: String = "Back",
    title: String = "LipiRx",
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
                            contentDescription = backContentDescription,
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
                contentDescription = "Scan History",
                modifier = Modifier.size(15.dp),
                tint = Lipi.Primary
            )
            Text(
                text = "Scan History",
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
    providerType: String = "gemini",
    customUrl: String = "",
    apiKeyError: String? = null,
    apiKeySuccess: String? = null,
    isTestingApiKey: Boolean = false,
    onDeviceStrategy: com.snehonir.lipirx.domain.model.OnDeviceStrategy = com.snehonir.lipirx.domain.model.OnDeviceStrategy.HYBRID_FALLBACK,
    agentConfigs: Map<RxAgentType, AgentConfig> = emptyMap(),
    providerModelsMap: Map<String, List<String>> = emptyMap(),
    onAgentProviderSelected: (RxAgentType, AiProvider) -> Unit = { _, _ -> },
    onAgentModelSelected: (RxAgentType, String) -> Unit = { _, _ -> },
    onProviderTabSelected: (String) -> Unit = {},
    onSaveProviderConfig: (providerType: String, customUrl: String, apiKey: String, modelName: String) -> Unit = { _, _, _, _ -> },
    onSelectOnDeviceStrategy: (com.snehonir.lipirx.domain.model.OnDeviceStrategy) -> Unit = {}
) {
    var showSheet by remember { mutableStateOf(false) }
    val agentCount = if (agentConfigs.isNotEmpty()) agentConfigs.size else RxAgentType.ALL.size

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
                text = "⚡ AI Pipeline ($agentCount)",
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
