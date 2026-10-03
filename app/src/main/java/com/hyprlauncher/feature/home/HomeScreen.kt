package com.hyprlauncher.feature.home

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.component.HyprSurfaceCard
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import com.hyprlauncher.feature.home.component.HomeAppGrid
import com.hyprlauncher.feature.home.component.HomeClock
import com.hyprlauncher.core.gesture.GestureType
import com.hyprlauncher.core.gesture.hyprGestureHandler
import com.hyprlauncher.feature.home.component.HomeDock
import com.hyprlauncher.feature.home.component.HomeSearchBar
import com.hyprlauncher.feature.home.component.HomeWallpaperSurface

/**
 * HyprLauncher Home Screen conforming to PRD Phase 3 (Sections 8, 9, 10, 11, 24, 27)
 * and Phase 5 Gesture recognition.
 * Implements a declarative layout engine orchestrating the Waybar, Clock, Search bar, App grid, Dock, and Gestures.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onWorkspaceSelected: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenDrawer: () -> Unit = {},
    onGesture: (GestureType) -> Unit = {},
    onAppClick: (String, String?) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    loadIcon: suspend (String) -> Bitmap? = { null },
    modifier: Modifier = Modifier
) {
    HomeWallpaperSurface(
        amoledMode = uiState.preferences.wallpaperAmoledMode,
        dimLevel = uiState.preferences.wallpaperDim,
        modifier = modifier
            .fillMaxSize()
            .hyprGestureHandler(onGesture = onGesture)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Waybar status bar + Optional default launcher banner
            Column {
                if (uiState.preferences.showWaybar) {
                    WaybarTopBar(
                        workspaces = uiState.workspaces,
                        activeWorkspaceId = uiState.preferences.activeWorkspaceId,
                        onWorkspaceSelected = onWorkspaceSelected,
                        onOpenDrawer = onOpenDrawer,
                        performanceModeName = uiState.preferences.performanceMode.name
                    )
                }

                if (!uiState.isDefaultLauncher) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DefaultLauncherBanner(onClick = onSetDefaultLauncher)
                }
            }

            // Center / Main Content Area: Clock + Search Prompt + App Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clock & Date component
                if (uiState.preferences.showClock) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HomeClock(
                        clock24Hour = uiState.preferences.clock24Hour,
                        showSeconds = uiState.preferences.showClockSeconds,
                        showDate = uiState.preferences.showDate
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Interactive Rofi / Search bar
                if (uiState.preferences.showSearchBar) {
                    HomeSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onSearchSubmit = {
                            val firstMatch = uiState.filteredApps.firstOrNull()
                            if (firstMatch != null) {
                                onAppClick(firstMatch.packageName, firstMatch.activityName)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // App Grid
                if (uiState.preferences.showAppGrid) {
                    HomeAppGrid(
                        apps = uiState.filteredApps,
                        columns = uiState.preferences.gridColumns,
                        showLabels = uiState.preferences.showAppLabels,
                        loadIcon = loadIcon,
                        onAppClick = onAppClick,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            // Footer Area: Bottom Dock + Telemetry Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.preferences.showDock && uiState.dockApps.isNotEmpty()) {
                    HomeDock(
                        dockApps = uiState.dockApps,
                        showLabels = false,
                        loadIcon = loadIcon,
                        onAppClick = onAppClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Waybar Footer Telemetry
                HyprSurfaceCard(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    backgroundColor = HyprTheme.colors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "hyprland ~ v1.0",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary
                        )
                        Text(
                            text = "apps: ${uiState.totalAppsIndexed}",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.accentSecondary
                        )
                        Text(
                            text = "mode: ${uiState.preferences.performanceMode.name.lowercase()}",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.terminalGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultLauncherBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "default launcher: unset",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.warning
            )
            Text(
                text = "[set default]",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WaybarTopBar(
    workspaces: List<WorkspaceEntity>,
    activeWorkspaceId: Int,
    onWorkspaceSelected: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    performanceModeName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Workspace selector pill: [1] [2] [3] [4] [5]
        Surface(
            shape = HyprTheme.shapes.small,
            color = HyprTheme.colors.surface,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayList = if (workspaces.isNotEmpty()) workspaces else (1..5).map {
                    WorkspaceEntity(it, "WS $it", it)
                }

                displayList.forEach { ws ->
                    val isActive = ws.id == activeWorkspaceId
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isActive) HyprTheme.colors.accent else Color.Transparent,
                        modifier = Modifier.clickable { onWorkspaceSelected(ws.id) }
                    ) {
                        Text(
                            text = "${ws.id}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = HyprTheme.typography.statusModule,
                            color = if (isActive) HyprTheme.colors.background else HyprTheme.colors.textSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Status indicator modules + Rofi drawer button
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
                modifier = Modifier.clickable { onOpenDrawer() }
            ) {
                Text(
                    text = "rofi",
                    style = HyprTheme.typography.statusModule,
                    color = HyprTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            HyprStatusBadge(
                text = "arch",
                accentColor = HyprTheme.colors.accent
            )
            HyprStatusBadge(
                text = performanceModeName.take(4).lowercase(),
                accentColor = HyprTheme.colors.accentSecondary
            )
        }
    }
}
