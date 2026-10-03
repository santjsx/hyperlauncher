package com.hyprlauncher.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyprlauncher.core.designsystem.theme.HyprTheme

/**
 * Wallpaper & Surface Canvas conforming to PRD Section 27.
 * Supports AMOLED true black, dimmed background, and subtle technical grid points.
 */
@Composable
fun HomeWallpaperSurface(
    amoledMode: Boolean,
    dimLevel: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (amoledMode) Color.Black else HyprTheme.colors.background
    val gridColor = if (amoledMode) Color.Transparent else HyprTheme.colors.border.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                if (!amoledMode) {
                    val step = 32.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        var y = step
                        while (y < size.height) {
                            drawCircle(
                                color = gridColor,
                                radius = 1.dp.toPx(),
                                center = Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }
                }
            }
    ) {
        // Scrim / Dimming layer
        if (dimLevel > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimLevel.coerceIn(0f, 0.9f)))
            )
        }

        content()
    }
}
