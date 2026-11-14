package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_stop_analysis
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.content_desc_stop_analysis
import com.snehonir.lipirx.resources.loading_analyzing_title
import com.snehonir.lipirx.resources.loading_default_subtitle
import com.snehonir.lipirx.resources.step_entity_parser
import com.snehonir.lipirx.resources.step_manager
import com.snehonir.lipirx.resources.step_safety_auditor
import com.snehonir.lipirx.resources.step_substitute_finder
import com.snehonir.lipirx.resources.step_vision_ocr
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingView(
    selectedModel: String,
    imageByteArray: ByteArray? = null,
    activeStep: Int = 1,
    statusText: String = "",
    onStopAnalysis: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanY",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(190.dp, 250.dp)
                .shadow(20.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            if (imageByteArray != null && imageByteArray.isNotEmpty()) {
                AsyncImage(
                    model = imageByteArray,
                    contentDescription = stringResource(Res.string.content_desc_prescription),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                PaperIllustration(150.dp, 200.dp)
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * progress
                val glow = 42.dp.toPx()
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Lipi.Primary.copy(alpha = 0.40f)),
                        startY = y - glow, endY = y,
                    ),
                    topLeft = Offset(0f, y - glow),
                    size = Size(size.width, glow),
                )
                drawLine(Lipi.Primary, Offset(0f, y), Offset(size.width, y), 3.5.dp.toPx(), StrokeCap.Round)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(Res.string.loading_analyzing_title), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText.ifBlank { stringResource(Res.string.loading_default_subtitle) },
                fontSize = 14.sp,
                color = Lipi.Muted,
                textAlign = TextAlign.Center,
            )
        }

        val stepLabels = listOf(
            stringResource(Res.string.step_manager),
            stringResource(Res.string.step_vision_ocr),
            stringResource(Res.string.step_entity_parser),
            stringResource(Res.string.step_substitute_finder),
            stringResource(Res.string.step_safety_auditor)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stepLabels.forEachIndexed { i, label ->
                val (mark, color) = when {
                    i < activeStep -> "✓" to Lipi.Primary
                    i == activeStep -> "●" to Lipi.Ink
                    else -> "○" to Color(0xFF7C8F94)
                }
                Text("$mark  $label", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
        }

        OutlinedButton(
            onClick = onStopAnalysis,
            modifier = Modifier.padding(top = 4.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Lipi.Error.copy(alpha = 0.35f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Lipi.Error.copy(alpha = 0.08f),
                contentColor = Lipi.Error
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.content_desc_stop_analysis),
                    modifier = Modifier.size(16.dp),
                    tint = Lipi.Error
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.btn_stop_analysis),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Lipi.Error
                )
            }
        }
    }
}

@Preview
@Composable
fun LoadingViewVisionOcrPreview() {
    LipiRxTheme {
        LoadingView(
            selectedModel = "gemini-2.0-flash",
            activeStep = 1,
            statusText = "Reading handwriting & Vision OCR..."
        )
    }
}
