package com.hyprlauncher.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.hyprlauncher.core.designsystem.theme.HyprTheme

/**
 * A technical, thin-bordered surface container inspired by Hyprland floating panels and Waybar modules.
 */
@Composable
fun HyprSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = HyprTheme.shapes.medium,
    backgroundColor: Color = HyprTheme.colors.surface,
    borderColor: Color = HyprTheme.colors.border,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(HyprTheme.shapes.borderWidth, borderColor)
    ) {
        Box {
            content()
        }
    }
}
