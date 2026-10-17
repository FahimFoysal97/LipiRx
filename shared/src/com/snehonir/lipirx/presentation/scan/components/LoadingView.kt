package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.snehonir.lipirx.ui.theme.Lipi

/** @param activeStep 0 = encoding payload, 1 = reading handwriting, 2 = matching brands, 3 = finding substitutes */
@Composable
fun LoadingView(
    selectedModel: String,
    imageByteArray: ByteArray? = null,
    activeStep: Int = 1,
    statusText: String = ""
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
        verticalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(150.dp, 190.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            /*if (imageByteArray != null && imageByteArray.isNotEmpty()) {
                AsyncImage(
                    model = imageByteArray,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                PaperIllustration(120.dp, 160.dp)
            }*/
            PaperIllustration(120.dp, 160.dp)
            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * progress
                val glow = 36.dp.toPx()
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Lipi.Primary.copy(alpha = 0.35f)),
                        startY = y - glow, endY = y,
                    ),
                    topLeft = Offset(0f, y - glow),
                    size = Size(size.width, glow),
                )
                drawLine(Lipi.Primary, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
            }
        }

        CircularProgressIndicator(
            modifier = Modifier.size(84.dp),
            color = Lipi.Primary,
            trackColor = Lipi.Outline,
            strokeWidth = 7.dp,
            strokeCap = StrokeCap.Round,
        )

        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Analyzing Prescription", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Lipi.Ink)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText.ifBlank { "using ${selectedModel.ifBlank { "Gemini AI" }}..." },
                fontSize = 14.sp,
                color = Lipi.Muted,
                textAlign = TextAlign.Center,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "Encoding image & payload",
                "Reading handwriting",
                "Matching brand names",
                "Finding similar brands"
            ).forEachIndexed { i, label ->
                val (mark, color) = when {
                    i < activeStep -> "✓" to Lipi.Primary
                    i == activeStep -> "●" to Lipi.Ink
                    else -> "○" to Color(0xFF7C8F94)
                }
                Text("$mark  $label", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
        }
    }
}
