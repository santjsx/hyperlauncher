package com.hyprlauncher.data.datastore

import com.hyprlauncher.domain.model.PerformanceMode

/**
 * Immutable user configuration model persisted via DataStore (PRD Section 41).
 */
data class LauncherPreferences(
    val activeWorkspaceId: Int = 1,
    val performanceMode: PerformanceMode = PerformanceMode.BALANCED,
    val themeId: String = "arch_dark",
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val showDock: Boolean = true,
    val clock24Hour: Boolean = true,
    val showClockSeconds: Boolean = false,
    val enableBlur: Boolean = true,
    // Phase 3 Declarative Layout & Home Configuration
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showSearchBar: Boolean = true,
    val showAppGrid: Boolean = true,
    val showWaybar: Boolean = true,
    val showAppLabels: Boolean = true,
    val wallpaperDim: Float = 0.2f,
    val wallpaperAmoledMode: Boolean = false,
    val dockPackageNames: List<String> = emptyList()
)
