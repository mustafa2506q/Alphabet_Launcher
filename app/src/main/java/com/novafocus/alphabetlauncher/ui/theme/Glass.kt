package com.novafocus.alphabetlauncher.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Frosted-glass card style: a translucent tinted fill (light for dark theme,
 * dark for light theme — glass always reads as a lighter/blurred pane
 * against its background, regardless of the current theme), a soft gradient
 * highlight along the top edge, and a thin light border, all inside a
 * rounded shape.
 *
 * True background blur (Modifier.blur/RenderEffect) only exists on API 31+
 * and looks inconsistent across OEM skins, so this favours the classic,
 * device-independent glass look: translucency + border + highlight.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.White,
    alpha: Float = 0.16f,
    borderAlpha: Float = 0.28f,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fillTint = if (isDark) tint else Color.Black
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    fillTint.copy(alpha = alpha * 1.6f),
                    fillTint.copy(alpha = alpha),
                ),
            ),
        )
        .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}