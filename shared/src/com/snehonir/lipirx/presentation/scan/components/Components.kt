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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.coroutines.launch

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Lipi.Primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Rx", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Text("LipiRx", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
        }
        trailing()
    }
}

@Composable
fun ModelChip(
    selectedModel: String,
    availableModels: List<String>,
    apiKey: String = "",
    apiKeyError: String? = null,
    isTestingApiKey: Boolean = false,
    onSaveApiKey: (String) -> Unit = {},
    onModelSelected: (String) -> Unit
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(if (availableModels.isNotEmpty()) Lipi.Success else Lipi.Error, CircleShape)
            )
            Text(
                text = if (selectedModel.isNotBlank()) selectedModel else "Set API Key",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Lipi.Ink,
                maxLines = 1
            )
        }
    }

    if (showSheet) {
        ModelSelectionBottomSheet(
            selectedModel = selectedModel,
            availableModels = availableModels,
            apiKey = apiKey,
            apiKeyError = apiKeyError,
            isTestingApiKey = isTestingApiKey,
            onSaveApiKey = onSaveApiKey,
            onModelSelected = onModelSelected,
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
    selectedModel: String,
    availableModels: List<String>,
    apiKey: String = "",
    apiKeyError: String? = null,
    isTestingApiKey: Boolean = false,
    onSaveApiKey: (String) -> Unit = {},
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    val isNewKeyPasted = inputKey.isNotBlank() && inputKey != apiKey
    val displayKeyText = remember(inputKey) { formatMaskedApiKey(inputKey) }

    fun dismissWithAnimation(onComplete: () -> Unit = {}) {
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
                onComplete()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            dismissWithAnimation()
        },
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Lipi.PrimaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔑", fontSize = 18.sp)
                    }
                    Column {
                        Text("Gemini API Key & Models", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
                        Text("Set key to fetch models & enable scanning", fontSize = 12.sp, color = Lipi.Muted)
                    }
                }
            }

            // API Key Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Lipi.Background,
                border = BorderStroke(1.dp, Lipi.Outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Gemini API Key",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Lipi.Ink
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = displayKeyText,
                            onValueChange = {}, // Uneditable by soft keyboard
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Tap 'Paste' to insert API key...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        SecondaryButton(
                            text = "Paste",
                            icon = null,
                            onClick = {
                                val text = clipboardManager.getText()?.text?.trim() ?: ""
                                if (text.isNotBlank()) inputKey = text
                            },
                            height = 50.dp,
                            fontSize = 13
                        )
                    }

                    if (!apiKeyError.isNullOrBlank()) {
                        Text(
                            text = apiKeyError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Lipi.Error
                        )
                    }

                    Button(
                        onClick = { onSaveApiKey(inputKey) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isTestingApiKey && isNewKeyPasted,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Lipi.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isTestingApiKey) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Key & Loading Models...", fontSize = 13.sp)
                        } else {
                            Text("🔑 Save Key & Load Models", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (availableModels.isNotEmpty()) {
                Text(
                    text = "Available Models (${availableModels.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Ink
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(availableModels) { model ->
                        val isSelected = model == selectedModel
                        Surface(
                            onClick = {
                                dismissWithAnimation {
                                    onModelSelected(model)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Lipi.PrimaryContainer else Lipi.Background,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Lipi.Primary else Lipi.Outline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (isSelected) Lipi.Success else Lipi.Muted, CircleShape)
                                    )
                                    Text(
                                        text = model,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Lipi.Primary else Lipi.Ink
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Lipi.Primary
                                    ) {
                                        Text(
                                            text = " ✓ ",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun formatMaskedApiKey(key: String): String {
    val trimmed = key.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.length <= 8) return "*".repeat(trimmed.length)
    val firstFour = trimmed.take(4)
    val lastFour = trimmed.takeLast(4)
    val middleMask = "*".repeat((trimmed.length - 8).coerceAtLeast(4))
    return "$firstFour$middleMask$lastFour"
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
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lipi.Primary, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
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
    height: Dp = 50.dp,
    contentColor: Color = Lipi.Ink,
    fontSize: Int = 14,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Lipi.Outline),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
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
    rotation: Float = 0f,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .size(width, height)
            .rotate(rotation)
            .shadow(18.dp, shape, ambientColor = Lipi.Ink.copy(0.2f), spotColor = Lipi.Ink.copy(0.2f))
            .background(Color(0xFFFFFEFA), shape)
            .padding(width * 0.1f),
    ) {
        Text("Rx", color = Lipi.Primary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        listOf(0.82f, 0.64f, 0.74f, 0.5f, 0.78f, 0.58f).forEach { w ->
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(w)
                    .height(6.dp)
                    .background(Lipi.InkLine, RoundedCornerShape(3.dp))
            )
        }
    }
}
