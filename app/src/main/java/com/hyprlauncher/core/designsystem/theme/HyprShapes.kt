package com.hyprlauncher.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hyprland-inspired shapes and metrics:
 * Precise, sharp-yet-modern geometry (subtle corner radii, razor thin borders).
 */
@Immutable
data class HyprShapes(
    val small: Shape = RoundedCornerShape(4.dp),
    val medium: Shape = RoundedCornerShape(8.dp),
    val large: Shape = RoundedCornerShape(12.dp),
    val pill: Shape = RoundedCornerShape(50),
    val borderWidth: Dp = 1.dp
)

val DefaultHyprShapes = HyprShapes()

val LocalHyprShapes = staticCompositionLocalOf { DefaultHyprShapes }
