package com.hyprlauncher.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Customization configuration models for Phase 8.
 * Covers:
 * - Layout editor
 * - Dock customization
 * - Search customization
 * - App grid customization
 * - Typography editor
 * - Icon customization
 */

enum class SearchRankingMode {
    DETERMINISTIC_HYBRID,
    FREQUENCY_FIRST,
    RECENCY_FIRST,
    ALPHABETICAL
}

enum class FontFamilyPreference {
    SANS_SERIF,
    MONOSPACE,
    SERIF
}

enum class IconShape {
    SYSTEM_DEFAULT,
    ROUNDED_SQUARE,
    CIRCLE,
    SQUIRCLE
}

enum class IconTint {
    NONE,
    THEME_ACCENT,
    THEME_PRIMARY
}

data class LayoutConfig(
    val showWaybar: Boolean = true,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showSearchBar: Boolean = true,
    val showAppGrid: Boolean = true,
    val showDock: Boolean = true,
    val clockAtTop: Boolean = true,
    val wallpaperDim: Float = 0.2f,
    val wallpaperAmoledMode: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("showWaybar", showWaybar)
        put("showClock", showClock)
        put("showDate", showDate)
        put("showSearchBar", showSearchBar)
        put("showAppGrid", showAppGrid)
        put("showDock", showDock)
        put("clockAtTop", clockAtTop)
        put("wallpaperDim", wallpaperDim.toDouble())
        put("wallpaperAmoledMode", wallpaperAmoledMode)
    }

    companion object {
        fun fromJson(json: JSONObject?): LayoutConfig {
            if (json == null) return LayoutConfig()
            return LayoutConfig(
                showWaybar = json.optBoolean("showWaybar", true),
                showClock = json.optBoolean("showClock", true),
                showDate = json.optBoolean("showDate", true),
                showSearchBar = json.optBoolean("showSearchBar", true),
                showAppGrid = json.optBoolean("showAppGrid", true),
                showDock = json.optBoolean("showDock", true),
                clockAtTop = json.optBoolean("clockAtTop", true),
                wallpaperDim = json.optDouble("wallpaperDim", 0.2).toFloat(),
                wallpaperAmoledMode = json.optBoolean("wallpaperAmoledMode", false)
            )
        }
    }
}

data class DockConfig(
    val enabled: Boolean = true,
    val iconSizeDp: Int = 40,
    val showLabels: Boolean = false,
    val spacingDp: Int = 10,
    val cornerRadiusDp: Int = 16,
    val backgroundAlpha: Float = 0.95f,
    val pinnedPackages: List<String> = emptyList(),
    val maxItems: Int = 6
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("enabled", enabled)
        put("iconSizeDp", iconSizeDp)
        put("showLabels", showLabels)
        put("spacingDp", spacingDp)
        put("cornerRadiusDp", cornerRadiusDp)
        put("backgroundAlpha", backgroundAlpha.toDouble())
        put("pinnedPackages", JSONArray(pinnedPackages))
        put("maxItems", maxItems)
    }

    companion object {
        fun fromJson(json: JSONObject?): DockConfig {
            if (json == null) return DockConfig()
            val pinnedList = mutableListOf<String>()
            val arr = json.optJSONArray("pinnedPackages")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    pinnedList.add(arr.optString(i))
                }
            }
            return DockConfig(
                enabled = json.optBoolean("enabled", true),
                iconSizeDp = json.optInt("iconSizeDp", 40),
                showLabels = json.optBoolean("showLabels", false),
                spacingDp = json.optInt("spacingDp", 10),
                cornerRadiusDp = json.optInt("cornerRadiusDp", 16),
                backgroundAlpha = json.optDouble("backgroundAlpha", 0.95).toFloat(),
                pinnedPackages = pinnedList,
                maxItems = json.optInt("maxItems", 6)
            )
        }
    }
}

data class AppGridConfig(
    val columns: Int = 4,
    val rows: Int = 5,
    val iconSizeDp: Int = 36,
    val spacingHorizontalDp: Int = 8,
    val spacingVerticalDp: Int = 8,
    val showLabels: Boolean = true,
    val labelFontSizeSp: Int = 11
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("columns", columns)
        put("rows", rows)
        put("iconSizeDp", iconSizeDp)
        put("spacingHorizontalDp", spacingHorizontalDp)
        put("spacingVerticalDp", spacingVerticalDp)
        put("showLabels", showLabels)
        put("labelFontSizeSp", labelFontSizeSp)
    }

    companion object {
        fun fromJson(json: JSONObject?): AppGridConfig {
            if (json == null) return AppGridConfig()
            return AppGridConfig(
                columns = json.optInt("columns", 4),
                rows = json.optInt("rows", 5),
                iconSizeDp = json.optInt("iconSizeDp", 36),
                spacingHorizontalDp = json.optInt("spacingHorizontalDp", 8),
                spacingVerticalDp = json.optInt("spacingVerticalDp", 8),
                showLabels = json.optBoolean("showLabels", true),
                labelFontSizeSp = json.optInt("labelFontSizeSp", 11)
            )
        }
    }
}

data class SearchConfig(
    val fuzzyMatchingEnabled: Boolean = true,
    val prefixMatchingEnabled: Boolean = true,
    val searchHistoryEnabled: Boolean = true,
    val maxHistoryItems: Int = 5,
    val placeholderText: String = "Search apps or run command (rofi)...",
    val showPackageNames: Boolean = false,
    val rankingMode: SearchRankingMode = SearchRankingMode.DETERMINISTIC_HYBRID
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("fuzzyMatchingEnabled", fuzzyMatchingEnabled)
        put("prefixMatchingEnabled", prefixMatchingEnabled)
        put("searchHistoryEnabled", searchHistoryEnabled)
        put("maxHistoryItems", maxHistoryItems)
        put("placeholderText", placeholderText)
        put("showPackageNames", showPackageNames)
        put("rankingMode", rankingMode.name)
    }

    companion object {
        fun fromJson(json: JSONObject?): SearchConfig {
            if (json == null) return SearchConfig()
            val ranking = runCatching {
                SearchRankingMode.valueOf(json.optString("rankingMode", SearchRankingMode.DETERMINISTIC_HYBRID.name))
            }.getOrDefault(SearchRankingMode.DETERMINISTIC_HYBRID)

            return SearchConfig(
                fuzzyMatchingEnabled = json.optBoolean("fuzzyMatchingEnabled", true),
                prefixMatchingEnabled = json.optBoolean("prefixMatchingEnabled", true),
                searchHistoryEnabled = json.optBoolean("searchHistoryEnabled", true),
                maxHistoryItems = json.optInt("maxHistoryItems", 5),
                placeholderText = json.optString("placeholderText", "Search apps or run command (rofi)..."),
                showPackageNames = json.optBoolean("showPackageNames", false),
                rankingMode = ranking
            )
        }
    }
}

data class TypographyConfig(
    val fontFamily: FontFamilyPreference = FontFamilyPreference.SANS_SERIF,
    val fontScale: Float = 1.0f,
    val letterSpacingSp: Float = 0.0f,
    val useMonospaceForStatusOnly: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("fontFamily", fontFamily.name)
        put("fontScale", fontScale.toDouble())
        put("letterSpacingSp", letterSpacingSp.toDouble())
        put("useMonospaceForStatusOnly", useMonospaceForStatusOnly)
    }

    companion object {
        fun fromJson(json: JSONObject?): TypographyConfig {
            if (json == null) return TypographyConfig()
            val family = runCatching {
                FontFamilyPreference.valueOf(json.optString("fontFamily", FontFamilyPreference.SANS_SERIF.name))
            }.getOrDefault(FontFamilyPreference.SANS_SERIF)

            return TypographyConfig(
                fontFamily = family,
                fontScale = json.optDouble("fontScale", 1.0).toFloat(),
                letterSpacingSp = json.optDouble("letterSpacingSp", 0.0).toFloat(),
                useMonospaceForStatusOnly = json.optBoolean("useMonospaceForStatusOnly", true)
            )
        }
    }
}

data class IconConfig(
    val shape: IconShape = IconShape.SYSTEM_DEFAULT,
    val scale: Float = 1.0f,
    val tint: IconTint = IconTint.NONE,
    val showAppLabels: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shape", shape.name)
        put("scale", scale.toDouble())
        put("tint", tint.name)
        put("showAppLabels", showAppLabels)
    }

    companion object {
        fun fromJson(json: JSONObject?): IconConfig {
            if (json == null) return IconConfig()
            val shape = runCatching {
                IconShape.valueOf(json.optString("shape", IconShape.SYSTEM_DEFAULT.name))
            }.getOrDefault(IconShape.SYSTEM_DEFAULT)
            val tint = runCatching {
                IconTint.valueOf(json.optString("tint", IconTint.NONE.name))
            }.getOrDefault(IconTint.NONE)

            return IconConfig(
                shape = shape,
                scale = json.optDouble("scale", 1.0).toFloat(),
                tint = tint,
                showAppLabels = json.optBoolean("showAppLabels", true)
            )
        }
    }
}

/**
 * Root customization model aggregating all 6 customization subsystems.
 */
data class CustomizationConfig(
    val layout: LayoutConfig = LayoutConfig(),
    val dock: DockConfig = DockConfig(),
    val grid: AppGridConfig = AppGridConfig(),
    val search: SearchConfig = SearchConfig(),
    val typography: TypographyConfig = TypographyConfig(),
    val icons: IconConfig = IconConfig()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("layout", layout.toJson())
        root.put("dock", dock.toJson())
        root.put("grid", grid.toJson())
        root.put("search", search.toJson())
        root.put("typography", typography.toJson())
        root.put("icons", icons.toJson())
        return root.toString()
    }

    companion object {
        fun fromJson(jsonString: String?): CustomizationConfig {
            if (jsonString.isNullOrBlank()) return CustomizationConfig()
            return runCatching {
                val root = JSONObject(jsonString)
                CustomizationConfig(
                    layout = LayoutConfig.fromJson(root.optJSONObject("layout")),
                    dock = DockConfig.fromJson(root.optJSONObject("dock")),
                    grid = AppGridConfig.fromJson(root.optJSONObject("grid")),
                    search = SearchConfig.fromJson(root.optJSONObject("search")),
                    typography = TypographyConfig.fromJson(root.optJSONObject("typography")),
                    icons = IconConfig.fromJson(root.optJSONObject("icons"))
                )
            }.getOrDefault(CustomizationConfig())
        }
    }
}
