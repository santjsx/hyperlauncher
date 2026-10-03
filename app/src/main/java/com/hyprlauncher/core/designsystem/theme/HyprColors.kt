package com.hyprlauncher.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * HyprLauncher design system color palette.
 * Based on Section 6.2 of the PRD (Arch / Hyprland / Linux ricing aesthetic).
 */
@Immutable
data class HyprColors(
    val background: Color = Color(0xFF08090C),
    val surface: Color = Color(0xFF111318),
    val surfaceElevated: Color = Color(0xFF171A21),
    val border: Color = Color(0xFF272C36),
    val textPrimary: Color = Color(0xFFF1F5F9),
    val textSecondary: Color = Color(0xFF94A3B8),
    val accent: Color = Color(0xFF00D9FF),          // Cyan accent
    val accentSecondary: Color = Color(0xFF7C3AED), // Violet accent
    val success: Color = Color(0xFF22C55E),
    val warning: Color = Color(0xFFF59E0B),
    val error: Color = Color(0xFFEF4444),
    val terminalGreen: Color = Color(0xFF50FA7B)
)

val DefaultHyprColors = HyprColors()

val LocalHyprColors = staticCompositionLocalOf { DefaultHyprColors }
