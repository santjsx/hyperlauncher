package com.hyprlauncher.domain.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-workspace layout configuration (PRD Section 17 & 18).
 * Allows custom grid dimensions, widget visibility, wallpaper, and assigned apps.
 */
data class WorkspaceLayoutConfig(
    val gridColumns: Int? = null,
    val gridRows: Int? = null,
    val showClock: Boolean? = null,
    val showSearchBar: Boolean? = null,
    val wallpaperUri: String? = null,
    val wallpaperDim: Float? = null,
    val assignedPackageNames: List<String> = emptyList()
) {
    fun toJson(): String {
        val json = JSONObject()
        gridColumns?.let { json.put("gridColumns", it) }
        gridRows?.let { json.put("gridRows", it) }
        showClock?.let { json.put("showClock", it) }
        showSearchBar?.let { json.put("showSearchBar", it) }
        wallpaperUri?.let { json.put("wallpaperUri", it) }
        wallpaperDim?.let { json.put("wallpaperDim", it.toDouble()) }

        if (assignedPackageNames.isNotEmpty()) {
            val array = JSONArray()
            assignedPackageNames.forEach { array.put(it) }
            json.put("assignedPackages", array)
        }
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): WorkspaceLayoutConfig {
            if (jsonStr.isNullOrBlank()) return WorkspaceLayoutConfig()
            return runCatching {
                val json = JSONObject(jsonStr)
                val packages = mutableListOf<String>()
                val array = json.optJSONArray("assignedPackages")
                if (array != null) {
                    for (i in 0 until array.length()) {
                        packages.add(array.getString(i))
                    }
                }

                WorkspaceLayoutConfig(
                    gridColumns = if (json.has("gridColumns")) json.getInt("gridColumns") else null,
                    gridRows = if (json.has("gridRows")) json.getInt("gridRows") else null,
                    showClock = if (json.has("showClock")) json.getBoolean("showClock") else null,
                    showSearchBar = if (json.has("showSearchBar")) json.getBoolean("showSearchBar") else null,
                    wallpaperUri = if (json.has("wallpaperUri")) json.getString("wallpaperUri") else null,
                    wallpaperDim = if (json.has("wallpaperDim")) json.getDouble("wallpaperDim").toFloat() else null,
                    assignedPackageNames = packages
                )
            }.getOrDefault(WorkspaceLayoutConfig())
        }
    }
}

/**
 * Hyprland-inspired Workspace domain model.
 */
data class Workspace(
    val id: Int,
    val name: String,
    val orderIndex: Int,
    val iconName: String = "terminal",
    val wallpaperUri: String? = null,
    val layoutConfig: WorkspaceLayoutConfig = WorkspaceLayoutConfig()
)
