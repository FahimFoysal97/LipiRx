package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.ui.theme.Lipi

@Composable
fun EmptyStateView(
    selectedModel: String,
    availableModels: List<String>,
    onModelSelected: (String) -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
    ) {
        AppHeader {
            ModelChip(
                selectedModel = selectedModel,
                availableModels = availableModels,
                onModelSelected = onModelSelected
            )
        }

        // Viewfinder hero
        Box(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Lipi.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val s = 34.dp.toPx()
                val m = 22.dp.toPx()
                val sw = 3.dp.toPx()
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(Lipi.Primary, Offset(x, y), Offset(x + dx * s, y), sw, StrokeCap.Round)
                    drawLine(Lipi.Primary, Offset(x, y), Offset(x, y + dy * s), sw, StrokeCap.Round)
                }
                corner(m, m, 1f, 1f)
                corner(size.width - m, m, -1f, 1f)
                corner(m, size.height - m, 1f, -1f)
                corner(size.width - m, size.height - m, -1f, -1f)
            }
            PaperIllustration(186.dp, 240.dp, rotation = -4f)
        }

        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
            Text(
                text = "Read any prescription, instantly.",
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Lipi.Ink,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Snap or upload a handwritten prescription. Get every medicine, schedule and alternative brand in seconds.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Lipi.Muted,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton("Take Prescription Photo", Icons.Default.Add, onTakePhoto)
            SecondaryButton(
                text = "Choose from Gallery",
                icon = Icons.Default.Search,
                onClick = onChooseGallery,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                fontSize = 16,
            )
        }
    }
}
