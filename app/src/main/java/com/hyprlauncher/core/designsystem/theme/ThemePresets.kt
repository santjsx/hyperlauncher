package com.hyprlauncher.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Built-in Linux ricing color presets conforming to PRD Section 21.
 * Supported themes: Arch Dark, Hyprland, Tokyo Night, Catppuccin, Nord,
 * Gruvbox, Dracula, AMOLED, Graphite, and Custom.
 */
enum class ThemePresetId(val displayName: String) {
    ARCH_DARK("Arch Dark"),
    HYPRLAND("Hyprland"),
    TOKYO_NIGHT("Tokyo Night"),
    CATPPUCCIN("Catppuccin"),
    NORD("Nord"),
    GRUVBOX("Gruvbox"),
    DRACULA("Dracula"),
    AMOLED("AMOLED"),
    GRAPHITE("Graphite"),
    CUSTOM("Custom")
}

object ThemePresets {

    val ArchDark = HyprColors(
        background = Color(0xFF08090C),
        surface = Color(0xFF111318),
        surfaceElevated = Color(0xFF171A21),
        border = Color(0xFF272C36),
        textPrimary = Color(0xFFF1F5F9),
        textSecondary = Color(0xFF94A3B8),
        accent = Color(0xFF00D9FF),          // Arch Cyan
        accentSecondary = Color(0xFF7C3AED), // Violet
        success = Color(0xFF22C55E),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        terminalGreen = Color(0xFF50FA7B)
    )

    val Hyprland = HyprColors(
        background = Color(0xFF0E0E14),
        surface = Color(0xFF161622),
        surfaceElevated = Color(0xFF1E1E30),
        border = Color(0xFF32324A),
        textPrimary = Color(0xFFF8F8F2),
        textSecondary = Color(0xFFA0A0BA),
        accent = Color(0xFF00FFCC),          // Cyan Neon
        accentSecondary = Color(0xFFFF007F), // Magenta Neon
        success = Color(0xFF5AF78E),
        warning = Color(0xFFF3F99D),
        error = Color(0xFFFF5555),
        terminalGreen = Color(0xFF5AF78E)
    )

    val TokyoNight = HyprColors(
        background = Color(0xFF1A1B26),
        surface = Color(0xFF24283B),
        surfaceElevated = Color(0xFF2F3549),
        border = Color(0xFF414868),
        textPrimary = Color(0xFFC0CAF5),
        textSecondary = Color(0xFF565F89),
        accent = Color(0xFF7AA2F7),          // Tokyo Blue
        accentSecondary = Color(0xFFBB9AF7), // Tokyo Purple
        success = Color(0xFF9ECE6A),
        warning = Color(0xFFE0AF68),
        error = Color(0xFFF7768E),
        terminalGreen = Color(0xFF73DACA)
    )

    val Catppuccin = HyprColors(
        background = Color(0xFF1E1E2E),
        surface = Color(0xFF181825),
        surfaceElevated = Color(0xFF313244),
        border = Color(0xFF45475A),
        textPrimary = Color(0xFFCDD6F4),
        textSecondary = Color(0xFFA6ADC8),
        accent = Color(0xFFCBA6F7),          // Mauve
        accentSecondary = Color(0xFF89B4FA), // Blue
        success = Color(0xFFA6E3A1),
        warning = Color(0xFFF9E2AF),
        error = Color(0xFFF38BA8),
        terminalGreen = Color(0xFFA6E3A1)
    )

    val Nord = HyprColors(
        background = Color(0xFF2E3440),
        surface = Color(0xFF3B4252),
        surfaceElevated = Color(0xFF434C5E),
        border = Color(0xFF4C566A),
        textPrimary = Color(0xFFECEFF4),
        textSecondary = Color(0xFFD8DEE9),
        accent = Color(0xFF88C0D0),          // Frost Cyan
        accentSecondary = Color(0xFF81A1C1), // Frost Blue
        success = Color(0xFFA3BE8C),
        warning = Color(0xFFEBCB8B),
        error = Color(0xFFBF616A),
        terminalGreen = Color(0xFFA3BE8C)
    )

    val Gruvbox = HyprColors(
        background = Color(0xFF282828),
        surface = Color(0xFF32302F),
        surfaceElevated = Color(0xFF3C3836),
        border = Color(0xFF504945),
        textPrimary = Color(0xFFEBDBB2),
        textSecondary = Color(0xFFA89984),
        accent = Color(0xFFFABD2F),          // Gruvbox Yellow
        accentSecondary = Color(0xFFFE8019), // Gruvbox Orange
        success = Color(0xFFB8BB26),
        warning = Color(0xFFFABD2F),
        error = Color(0xFFFB4934),
        terminalGreen = Color(0xFFB8BB26)
    )

    val Dracula = HyprColors(
        background = Color(0xFF282A36),
        surface = Color(0xFF21222C),
        surfaceElevated = Color(0xFF343746),
        border = Color(0xFF44475A),
        textPrimary = Color(0xFFF8F8F2),
        textSecondary = Color(0xFF6272A4),
        accent = Color(0xFFFF79C6),          // Dracula Pink
        accentSecondary = Color(0xFFBD93F9), // Dracula Purple
        success = Color(0xFF50FA7B),
        warning = Color(0xFFF1FA8C),
        error = Color(0xFFFF5555),
        terminalGreen = Color(0xFF50FA7B)
    )

    val Amoled = HyprColors(
        background = Color(0xFF000000),      // Pure True Black
        surface = Color(0xFF080808),
        surfaceElevated = Color(0xFF121212),
        border = Color(0xFF222222),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF888888),
        accent = Color(0xFF00FF66),          // Matrix High-contrast green
        accentSecondary = Color(0xFF00E5FF),
        success = Color(0xFF00FF66),
        warning = Color(0xFFFFD600),
        error = Color(0xFFFF1744),
        terminalGreen = Color(0xFF00FF66)
    )

    val Graphite = HyprColors(
        background = Color(0xFF18181B),
        surface = Color(0xFF27272A),
        surfaceElevated = Color(0xFF3F3F46),
        border = Color(0xFF52525B),
        textPrimary = Color(0xFFFAFAFA),
        textSecondary = Color(0xFF71717A),
        accent = Color(0xFFE4E4E7),          // Minimal Slate
        accentSecondary = Color(0xFFA1A1AA),
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFBBF24),
        error = Color(0xFFF87171),
        terminalGreen = Color(0xFF4ADE80)
    )

    fun forPreset(presetId: ThemePresetId): HyprColors {
        return when (presetId) {
            ThemePresetId.ARCH_DARK -> ArchDark
            ThemePresetId.HYPRLAND -> Hyprland
            ThemePresetId.TOKYO_NIGHT -> TokyoNight
            ThemePresetId.CATPPUCCIN -> Catppuccin
            ThemePresetId.NORD -> Nord
            ThemePresetId.GRUVBOX -> Gruvbox
            ThemePresetId.DRACULA -> Dracula
            ThemePresetId.AMOLED -> Amoled
            ThemePresetId.GRAPHITE -> Graphite
            ThemePresetId.CUSTOM -> ArchDark
        }
    }
}
