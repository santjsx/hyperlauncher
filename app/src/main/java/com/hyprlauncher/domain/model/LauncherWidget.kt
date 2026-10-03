package com.hyprlauncher.domain.model

import java.util.UUID

/**
 * Domain representation of a configured and placed Android widget on a workspace (PRD Section 25 & Phase 10).
 */
data class LauncherWidget(
    val id: String = UUID.randomUUID().toString(),
    val appWidgetId: Int,
    val workspaceId: Int,
    val providerPackage: String,
    val providerClass: String,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 2,
    val spanY: Int = 2,
    val label: String = "Widget",
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this widget overlaps with another widget on the same workspace grid.
     */
    fun overlaps(other: LauncherWidget): Boolean {
        if (workspaceId != other.workspaceId || id == other.id) return false
        val thisRight = cellX + spanX
        val thisBottom = cellY + spanY
        val otherRight = other.cellX + other.spanX
        val otherBottom = other.cellY + other.spanY

        return cellX < otherRight && thisRight > other.cellX && cellY < otherBottom && thisBottom > other.cellY
    }
}

/**
 * Item representing an available widget provider from an installed application for the Widget Picker.
 */
data class WidgetProviderItem(
    val providerPackage: String,
    val providerClass: String,
    val appLabel: String,
    val widgetLabel: String,
    val minWidthDp: Int = 80,
    val minHeightDp: Int = 80,
    val minSpanX: Int = 2,
    val minSpanY: Int = 2,
    val previewImageRes: Int = 0,
    val iconRes: Int = 0,
    val configureActivity: String? = null
)
