package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snehonir.lipirx.domain.model.ThemeMode
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_choose_gallery
import com.snehonir.lipirx.resources.btn_take_photo
import com.snehonir.lipirx.resources.empty_description
import com.snehonir.lipirx.resources.empty_headline
import com.snehonir.lipirx.resources.header_history
import com.snehonir.lipirx.resources.header_settings
import com.snehonir.lipirx.ui.theme.Lipi
import com.snehonir.lipirx.ui.theme.LipiRxTheme
import com.snehonir.lipirx.ui.theme.icons.CameraVectorIcon
import com.snehonir.lipirx.ui.theme.icons.GalleryVectorIcon
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmptyStateView(
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onHistoryClick: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenAiStudio: (() -> Unit)? = onOpenSettings
) {
    // Read theme colours here, in composable scope, then use them inside draw lambdas.
    val primaryColor = Lipi.Primary
    val settingsAction = onOpenSettings ?: onOpenAiStudio

    // Gentle "the camera is live" pulse on the viewfinder brackets.
    val pulseTransition = rememberInfiniteTransition(label = "viewfinderPulse")
    val bracketAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "bracketAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Lipi.Background)
            .statusBarsPadding()
    ) {
        AppHeader()

        // ── Viewfinder hero ─────────────────────────────────────────────────
        // Takes whatever vertical space is left, so small phones never overflow.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 220.dp)
                .padding(start = 24.dp, end = 24.dp, top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Lipi.PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val arm = 36.dp.toPx()
                val radius = 18.dp.toPx()
                val margin = 24.dp.toPx()
                val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val color = primaryColor.copy(alpha = bracketAlpha)

                drawCorner(margin, margin, 1f, 1f, arm, radius, color, stroke)                             // top-left
                drawCorner(size.width - margin, margin, -1f, 1f, arm, radius, color, stroke)               // top-right
                drawCorner(margin, size.height - margin, 1f, -1f, arm, radius, color, stroke)              // bottom-left
                drawCorner(size.width - margin, size.height - margin, -1f, -1f, arm, radius, color, stroke) // bottom-right
            }

            // Scale the paper to the space available (max 250dp tall, 0.76 aspect ratio).
            val paperHeight = (maxHeight - 72.dp).coerceIn(120.dp, 250.dp)
            PaperIllustration(paperHeight * 0.76f, paperHeight)
        }

        // ── Headline, description, tip ──────────────────────────────────────
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.empty_headline),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Lipi.Ink,
            )
            Text(
                text = stringResource(Res.string.empty_description),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Lipi.Muted,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Lipi.Primary
                )
                Text(
                    text = "Tip: lay the page flat in good light for the best read.",
                    fontSize = 12.sp,
                    color = Lipi.Muted
                )
            }
        }

        // ── Actions ─────────────────────────────────────────────────────────
        // One clear primary action, one alternative, two quiet shortcuts.
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = stringResource(Res.string.btn_take_photo),
                icon = CameraVectorIcon,
                onClick = onTakePhoto,
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton(
                text = stringResource(Res.string.btn_choose_gallery),
                icon = GalleryVectorIcon,
                onClick = onChooseGallery,
                modifier = Modifier.fillMaxWidth()
            )

            if (onHistoryClick != null || settingsAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onHistoryClick != null) {
                        QuietAction(
                            text = stringResource(Res.string.header_history),
                            icon = Icons.Default.DateRange,
                            tint = Lipi.Primary,
                            onClick = onHistoryClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (settingsAction != null) {
                        QuietAction(
                            text = stringResource(Res.string.header_settings),
                            icon = Icons.Default.Settings,
                            tint = Lipi.Muted,
                            onClick = settingsAction,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Low-emphasis action: icon + label, no border or fill. */
@Composable
private fun QuietAction(
    text: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = tint
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1
            )
        }
    }
}

/**
 * Draws one L-shaped viewfinder corner. (dx, dy) point inward from the corner:
 * (1, 1) top-left, (-1, 1) top-right, (1, -1) bottom-left, (-1, -1) bottom-right.
 */
private fun DrawScope.drawCorner(
    x: Float,
    y: Float,
    dx: Float,
    dy: Float,
    arm: Float,
    radius: Float,
    color: Color,
    stroke: Stroke,
) {
    val path = Path().apply {
        moveTo(x + dx * arm, y)
        lineTo(x + dx * radius, y)
        quadraticTo(x, y, x, y + dy * radius)
        lineTo(x, y + dy * arm)
    }
    drawPath(path = path, color = color, style = stroke)
}

@Preview
@Composable
fun EmptyStateViewPreview() {
    LipiRxTheme {
        EmptyStateView(
            onTakePhoto = {},
            onChooseGallery = {},
            onHistoryClick = {},
            onOpenSettings = {}
        )
    }
}

@Preview
@Composable
fun EmptyStateViewDarkPreview() {
    LipiRxTheme(themeMode = ThemeMode.DARK) {
        EmptyStateView(
            onTakePhoto = {},
            onChooseGallery = {},
            onHistoryClick = {},
            onOpenSettings = {}
        )
    }
}
