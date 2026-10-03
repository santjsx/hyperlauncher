package com.hyprlauncher.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val HyprDarkColorScheme = darkColorScheme(
    primary = DefaultHyprColors.accent,
    onPrimary = DefaultHyprColors.background,
    secondary = DefaultHyprColors.accentSecondary,
    onSecondary = DefaultHyprColors.textPrimary,
    background = DefaultHyprColors.background,
    onBackground = DefaultHyprColors.textPrimary,
    surface = DefaultHyprColors.surface,
    onSurface = DefaultHyprColors.textPrimary,
    surfaceVariant = DefaultHyprColors.surfaceElevated,
    onSurfaceVariant = DefaultHyprColors.textSecondary,
    outline = DefaultHyprColors.border,
    error = DefaultHyprColors.error,
    onError = DefaultHyprColors.textPrimary
)

@Composable
fun HyprTheme(
    colors: HyprColors = DefaultHyprColors,
    typography: HyprTypography = DefaultHyprTypography,
    shapes: HyprShapes = DefaultHyprShapes,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalHyprColors provides colors,
        LocalHyprTypography provides typography,
        LocalHyprShapes provides shapes
    ) {
        MaterialTheme(
            colorScheme = HyprDarkColorScheme,
            content = content
        )
    }
}

object HyprTheme {
    val colors: HyprColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHyprColors.current

    val typography: HyprTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalHyprTypography.current

    val shapes: HyprShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalHyprShapes.current
}
