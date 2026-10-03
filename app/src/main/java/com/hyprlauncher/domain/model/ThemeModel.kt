package com.hyprlauncher.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.hyprlauncher.core.designsystem.theme.HyprColors
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.core.designsystem.theme.ThemePresets
import org.json.JSONObject

/**
 * Animation scale setting conforming to PRD Section 29.
 */
enum class AnimationScale(val multiplier: Float, val displayName: String) {
    FULL(1.0f, "Full"),
    REDUCED(0.5f, "Reduced"),
    MINIMAL(0.25f, "Minimal"),
    DISABLED(0.0f, "Disabled")
}

/**
 * Complete Theme Configuration model (PRD Section 21 & 28 & 29).
 * Encapsulates color palettes, typography scaling, corner radius, borders, and animations.
 */
data class ThemeConfig(
    val presetId: ThemePresetId = ThemePresetId.ARCH_DARK,
    val customColors: HyprColors? = null,
    val cornerRadiusDp: Int = 8,
    val borderWidthDp: Float = 1.0f,
    val fontScale: Float = 1.0f,
    val useMonospaceAll: Boolean = false,
    val animationScale: AnimationScale = AnimationScale.FULL
) {
    /**
     * Resolves the active color palette.
     */
    val resolvedColors: HyprColors
        get() = if (presetId == ThemePresetId.CUSTOM && customColors != null) {
            customColors
        } else {
            ThemePresets.forPreset(presetId)
        }

    fun toJson(): String {
        val json = JSONObject()
        json.put("presetId", presetId.name)
        json.put("cornerRadiusDp", cornerRadiusDp)
        json.put("borderWidthDp", borderWidthDp.toDouble())
        json.put("fontScale", fontScale.toDouble())
        json.put("useMonospaceAll", useMonospaceAll)
        json.put("animationScale", animationScale.name)

        if (customColors != null) {
            val colorsJson = JSONObject()
            colorsJson.put("background", customColors.background.toArgb())
            colorsJson.put("surface", customColors.surface.toArgb())
            colorsJson.put("surfaceElevated", customColors.surfaceElevated.toArgb())
            colorsJson.put("border", customColors.border.toArgb())
            colorsJson.put("textPrimary", customColors.textPrimary.toArgb())
            colorsJson.put("textSecondary", customColors.textSecondary.toArgb())
            colorsJson.put("accent", customColors.accent.toArgb())
            colorsJson.put("accentSecondary", customColors.accentSecondary.toArgb())
            colorsJson.put("terminalGreen", customColors.terminalGreen.toArgb())
            json.put("customColors", colorsJson)
        }
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): ThemeConfig {
            if (jsonStr.isNullOrBlank()) return ThemeConfig()
            return runCatching {
                val json = JSONObject(jsonStr)
                val presetId = runCatching {
                    ThemePresetId.valueOf(json.optString("presetId", ThemePresetId.ARCH_DARK.name))
                }.getOrDefault(ThemePresetId.ARCH_DARK)

                val animScale = runCatching {
                    AnimationScale.valueOf(json.optString("animationScale", AnimationScale.FULL.name))
                }.getOrDefault(AnimationScale.FULL)

                val customColors = if (json.has("customColors")) {
                    val cJson = json.getJSONObject("customColors")
                    HyprColors(
                        background = Color(cJson.getInt("background")),
                        surface = Color(cJson.getInt("surface")),
                        surfaceElevated = Color(cJson.getInt("surfaceElevated")),
                        border = Color(cJson.getInt("border")),
                        textPrimary = Color(cJson.getInt("textPrimary")),
                        textSecondary = Color(cJson.getInt("textSecondary")),
                        accent = Color(cJson.getInt("accent")),
                        accentSecondary = Color(cJson.getInt("accentSecondary")),
                        terminalGreen = Color(cJson.optInt("terminalGreen", 0xFF50FA7B.toInt()))
                    )
                } else null

                ThemeConfig(
                    presetId = presetId,
                    customColors = customColors,
                    cornerRadiusDp = json.optInt("cornerRadiusDp", 8),
                    borderWidthDp = json.optDouble("borderWidthDp", 1.0).toFloat(),
                    fontScale = json.optDouble("fontScale", 1.0).toFloat(),
                    useMonospaceAll = json.optBoolean("useMonospaceAll", false),
                    animationScale = animScale
                )
            }.getOrDefault(ThemeConfig())
        }
    }
}
