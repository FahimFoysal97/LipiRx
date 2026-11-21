package com.snehonir.lipirx.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.app_name
import com.snehonir.lipirx.resources.btn_close
import com.snehonir.lipirx.resources.header_history
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

// The paper illustration should look like paper in BOTH themes, so it uses fixed colours
// instead of theme tokens (Lipi.Surface turns dark navy in dark mode).
private val PaperColor = Color(0xFFFCFDFC)
private val PaperBrand = Color(0xFF267A70) // same as the light-theme primary
private val PaperInkLine = Color(0xFF9FB0C8)

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
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Lipi.Surface,
                    border = BorderStroke(1.dp, Lipi.CardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backContentDescription.ifBlank { stringResource(Res.string.btn_close) },
                            modifier = Modifier.size(20.dp),
                            tint = Lipi.Ink
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Lipi.Primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Rx",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            if (subtitle != null) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Lipi.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Lipi.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = title,
                    fontSize = if (onBack != null) 20.sp else 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Lipi.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .semantics { heading() }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

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
        onClick = onClick,
        modifier = Modifier.heightIn(min = 40.dp),
        shape = CircleShape,
        color = Lipi.Surface,
        border = BorderStroke(1.dp, Lipi.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Decorative: the label next to it already says "History".
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Lipi.Primary
            )
            Text(
                text = stringResource(Res.string.header_history),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Lipi.Ink
            )
        }
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Lipi.Primary,
            // White on the bright dark-mode teal is hard to read; onPrimary is white in light, dark teal in dark.
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
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
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Lipi.Surface, contentColor = contentColor),
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
            overflow = TextOverflow.Ellipsis,
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

    // Only draw as many text lines as fit, so a short paper is not squashed when the hero is small.
    // Fixed part ≈ 78dp (padding + "Rx" + gap), each line = 18dp (12 gap + 6 bar).
    val lineWidths = remember(height) {
        val fit = ((height.value - 78f) / 18f).toInt().coerceIn(0, 6)
        listOf(0.85f, 0.65f, 0.76f, 0.52f, 0.8f, 0.58f).take(fit)
    }

    Column(
        modifier = modifier
            .size(width, height)
            .rotate(rotation)
            // Black shadow: Lipi.Ink is near-white in dark mode and would glow instead of shade.
            .shadow(20.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.25f))
            .background(PaperColor, shape)
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Text("Rx", color = PaperBrand, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Spacer(modifier = Modifier.height(6.dp))
        lineWidths.forEach { w ->
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(w)
                    .height(6.dp)
                    .background(PaperInkLine, CircleShape)
            )
        }
    }
}
