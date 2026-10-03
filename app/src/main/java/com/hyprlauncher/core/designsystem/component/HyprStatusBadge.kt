package com.hyprlauncher.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyprlauncher.core.designsystem.theme.HyprTheme

/**
 * Waybar-style status badge with monospace text and subtle border.
 */
@Composable
fun HyprStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = HyprTheme.colors.accent,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Text(
                    text = " ",
                    style = HyprTheme.typography.statusModule
                )
            }
            Text(
                text = text,
                style = HyprTheme.typography.statusModule,
                color = accentColor
            )
        }
    }
}
