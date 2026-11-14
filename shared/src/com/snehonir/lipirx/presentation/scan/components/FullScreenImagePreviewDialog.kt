package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_close
import com.snehonir.lipirx.resources.btn_reset
import com.snehonir.lipirx.resources.content_desc_prescription
import com.snehonir.lipirx.resources.zoom_hint
import com.snehonir.lipirx.ui.theme.Lipi
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val MAX_ZOOM = 6f
private const val DOUBLE_TAP_ZOOM = 2.5f

@Composable
fun FullScreenImagePreviewDialog(
    imageByteArray: ByteArray,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = box.width * (s - 1f) / 2f
        val maxY = box.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun animateZoomTo(target: Float) {
        scope.launch {
            val from = scale
            val fromOffset = offset
            animate(from, target, animationSpec = tween(250)) { v, _ ->
                scale = v
                val t = if (target == from) 1f else (v - from) / (target - from)
                offset = clamp(fromOffset * (1f - t), v)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Lipi.ViewerBg)
        ) {
            AsyncImage(
                model = imageByteArray,
                contentDescription = stringResource(Res.string.content_desc_prescription),
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { box = it }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { animateZoomTo(if (scale > 1.05f) 1f else DOUBLE_TAP_ZOOM) })
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            offset = if (newScale <= 1f) Offset.Zero else clamp(offset + pan, newScale)
                            scale = newScale
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit,
            )

            // Top controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                    Text(
                        text = "Zoom: ${(scale * 100).roundToInt()}%",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { animateZoomTo(1f) },
                    modifier = Modifier.height(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Lipi.Ink),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(Res.string.btn_reset), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.btn_close), tint = Color.White)
                }
            }

            // Bottom hint
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
            ) {
                Text(
                    text = stringResource(Res.string.zoom_hint),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
