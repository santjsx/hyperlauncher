package com.hyprlauncher.feature.home

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.component.HyprSurfaceCard
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.domain.model.Workspace
import com.hyprlauncher.feature.home.component.HomeAppGrid
import com.hyprlauncher.feature.home.component.HomeClock
import com.hyprlauncher.core.gesture.GestureType
import com.hyprlauncher.core.gesture.hyprGestureHandler
import com.hyprlauncher.feature.home.component.HomeDock
import com.hyprlauncher.feature.home.component.HomeSearchBar
import com.hyprlauncher.feature.home.component.HomeWallpaperSurface
import com.hyprlauncher.feature.home.component.WorkspaceManagementDialog

import androidx.compose.animation.core.snap
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.domain.model.AnimationScale
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.SearchConfig
import com.hyprlauncher.domain.model.TypographyConfig
import com.hyprlauncher.feature.home.component.CustomizationDialog
import com.hyprlauncher.feature.home.component.ThemePickerDialog
import com.hyprlauncher.core.widget.WidgetHostManager
import com.hyprlauncher.domain.model.WidgetProviderItem
import com.hyprlauncher.feature.widget.WidgetHostContainer
import com.hyprlauncher.feature.widget.WidgetPickerDialog

/**
 * HyprLauncher Home Screen conforming to PRD Phase 3 (Sections 8, 9, 10, 11, 24, 27),
 * Phase 5 Gesture recognition, Phase 6 Workspace Engine, Phase 7 Theme Engine, Phase 8 Customization,
 * and Phase 10 Production Widget System.
 * Implements a declarative layout engine orchestrating the Waybar, Clock, Search bar, App grid, Dock, Gestures,
 * Widgets with crash isolation, Hyprland-inspired animated workspace transitions, live theming, and full system ricing.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onWorkspaceSelected: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    widgetHostManager: WidgetHostManager? = null,
    onPlaceWidget: (WidgetProviderItem) -> Unit = {},
    onResizeWidget: (widgetId: String, spanX: Int, spanY: Int) -> Unit = { _, _, _ -> },
    onRemoveWidget: (widgetId: String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onOpenRiceStudio: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onGesture: (GestureType) -> Unit = {},
    onCreateWorkspace: (String) -> Unit = {},
    onRenameWorkspace: (Int, String) -> Unit = { _, _ -> },
    onDeleteWorkspace: (Int) -> Unit = {},
    onSelectThemePreset: (ThemePresetId) -> Unit = {},
    onSelectAnimationScale: (AnimationScale) -> Unit = {},
    onSelectCornerRadius: (Int) -> Unit = {},
    onUpdateLayout: (LayoutConfig) -> Unit = {},
    onUpdateDock: (DockConfig) -> Unit = {},
    onUpdateGrid: (AppGridConfig) -> Unit = {},
    onUpdateSearch: (SearchConfig) -> Unit = {},
    onUpdateTypography: (TypographyConfig) -> Unit = {},
    onUpdateIcons: (IconConfig) -> Unit = {},
    onPinApp: (String) -> Unit = {},
    onUnpinApp: (String) -> Unit = {},
    onResetCustomizationDefaults: () -> Unit = {},
    onAppClick: (String, String?) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    loadIcon: suspend (String) -> Bitmap? = { null },
    modifier: Modifier = Modifier
) {
    var showWorkspaceManager by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var isDefaultBannerDismissed by rememberSaveable { mutableStateOf(false) }

    val activeWallpaperUri = uiState.activeWorkspace?.wallpaperUri
        ?: uiState.activeWorkspace?.layoutConfig?.wallpaperUri
    val activeDimLevel = uiState.activeWorkspace?.layoutConfig?.wallpaperDim
        ?: uiState.customizationConfig.layout.wallpaperDim
    val amoledMode = uiState.customizationConfig.layout.wallpaperAmoledMode
        || uiState.preferences.wallpaperAmoledMode

    HomeWallpaperSurface(
        amoledMode = amoledMode,
        dimLevel = activeDimLevel,
        wallpaperUri = activeWallpaperUri,
        modifier = modifier
            .fillMaxSize()
            .hyprGestureHandler(onGesture = onGesture)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Waybar status bar + Optional default launcher banner
            Column {
                val showWaybar = uiState.customizationConfig.layout.showWaybar && uiState.preferences.showWaybar
                if (showWaybar) {
                    WaybarTopBar(
                        workspaces = uiState.workspaces,
                        activeWorkspaceId = uiState.preferences.activeWorkspaceId,
                        onWorkspaceSelected = onWorkspaceSelected,
                        onOpenWorkspaceManager = { showWorkspaceManager = true },
                        onOpenThemePicker = { showThemePicker = true },
                        onOpenCustomization = { showCustomizationDialog = true },
                        onOpenRiceStudio = onOpenRiceStudio,
                        onOpenWidgetPicker = { showWidgetPicker = true },
                        onOpenDiagnostics = onOpenDiagnostics,
                        onOpenDrawer = onOpenDrawer,
                        performanceModeName = uiState.preferences.performanceMode.name
                    )
                }

                if (!uiState.isDefaultLauncher && !isDefaultBannerDismissed) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DefaultLauncherBanner(
                        onClick = onSetDefaultLauncher,
                        onDismiss = { isDefaultBannerDismissed = true }
                    )
                }
            }

            // Center / Main Content Area: Clock + Search Prompt + Active Widgets + App Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clock & Date component
                val showClock = uiState.customizationConfig.layout.showClock
                    && (uiState.activeWorkspace?.layoutConfig?.showClock ?: uiState.preferences.showClock)
                if (showClock) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HomeClock(
                        clock24Hour = uiState.preferences.clock24Hour,
                        showSeconds = uiState.preferences.showClockSeconds,
                        showDate = uiState.customizationConfig.layout.showDate && uiState.preferences.showDate
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Interactive Rofi / Search bar
                val searchConf = uiState.customizationConfig.search
                val showSearchBar = uiState.customizationConfig.layout.showSearchBar
                    && (uiState.activeWorkspace?.layoutConfig?.showSearchBar ?: uiState.preferences.showSearchBar)
                if (showSearchBar) {
                    HomeSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholderText = searchConf.placeholderText,
                        onSearchSubmit = {
                            val firstMatch = uiState.filteredApps.firstOrNull()
                            if (firstMatch != null) {
                                onAppClick(firstMatch.packageName, firstMatch.activityName)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Widgets Canvas for Active Workspace (PRD Section 25)
                if (uiState.widgets.isNotEmpty() && widgetHostManager != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.widgets.forEach { widget ->
                            WidgetHostContainer(
                                widget = widget,
                                widgetHostManager = widgetHostManager,
                                onResize = { spanX, spanY -> onResizeWidget(widget.id, spanX, spanY) },
                                onRemove = { onRemoveWidget(widget.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // App Grid with Hyprland Horizontal Workspace Transition (Configurable timing via PRD §29)
                val showAppGrid = uiState.customizationConfig.layout.showAppGrid && uiState.preferences.showAppGrid
                if (showAppGrid) {
                    val gridConf = uiState.customizationConfig.grid
                    val iconsConf = uiState.customizationConfig.icons
                    val effectiveColumns = uiState.activeWorkspace?.layoutConfig?.gridColumns
                        ?: gridConf.columns

                    val animScale = uiState.themeConfig.animationScale
                    val duration = (180 * animScale.multiplier).toInt()

                    AnimatedContent(
                        targetState = uiState.preferences.activeWorkspaceId,
                        transitionSpec = {
                            if (animScale == AnimationScale.DISABLED) {
                                (fadeIn(animationSpec = snap()))
                                    .togetherWith(fadeOut(animationSpec = snap()))
                            } else {
                                val isForward = targetState > initialState
                                val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(duration, easing = FastOutSlowInEasing)
                                val fadeSpec = tween<Float>(duration)
                                if (isForward) {
                                    (slideInHorizontally(animationSpec = slideSpec) { it } + fadeIn(animationSpec = fadeSpec))
                                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { -it } + fadeOut(animationSpec = fadeSpec))
                                } else {
                                    (slideInHorizontally(animationSpec = slideSpec) { -it } + fadeIn(animationSpec = fadeSpec))
                                        .togetherWith(slideOutHorizontally(animationSpec = slideSpec) { it } + fadeOut(animationSpec = fadeSpec))
                                }
                            }
                        },
                        label = "WorkspaceGridTransition",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                    ) { _ ->
                        HomeAppGrid(
                            apps = uiState.filteredApps,
                            columns = effectiveColumns,
                            showLabels = gridConf.showLabels && iconsConf.showAppLabels,
                            iconSizeDp = gridConf.iconSizeDp,
                            horizontalSpacingDp = gridConf.spacingHorizontalDp,
                            verticalSpacingDp = gridConf.spacingVerticalDp,
                            iconShape = iconsConf.shape,
                            iconTint = iconsConf.tint,
                            labelFontSizeSp = gridConf.labelFontSizeSp,
                            loadIcon = loadIcon,
                            onAppClick = onAppClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Footer Area: Bottom Dock + Telemetry Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dockConf = uiState.customizationConfig.dock
                val iconsConf = uiState.customizationConfig.icons
                val showDock = dockConf.enabled
                    && uiState.customizationConfig.layout.showDock
                    && uiState.preferences.showDock
                if (showDock && uiState.dockApps.isNotEmpty()) {
                    HomeDock(
                        dockApps = uiState.dockApps,
                        showLabels = dockConf.showLabels && iconsConf.showAppLabels,
                        iconSizeDp = dockConf.iconSizeDp,
                        spacingDp = dockConf.spacingDp,
                        cornerRadiusDp = dockConf.cornerRadiusDp,
                        backgroundAlpha = dockConf.backgroundAlpha,
                        iconTint = iconsConf.tint,
                        iconShape = iconsConf.shape,
                        loadIcon = loadIcon,
                        onAppClick = onAppClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Waybar Footer Telemetry
                HyprSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .clickable { onOpenDiagnostics() },
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

    if (showWorkspaceManager) {
        WorkspaceManagementDialog(
            workspaces = uiState.workspaces,
            activeWorkspaceId = uiState.preferences.activeWorkspaceId,
            onSelectWorkspace = onWorkspaceSelected,
            onCreateWorkspace = onCreateWorkspace,
            onRenameWorkspace = onRenameWorkspace,
            onDeleteWorkspace = onDeleteWorkspace,
            onDismiss = { showWorkspaceManager = false }
        )
    }

    if (showThemePicker) {
        ThemePickerDialog(
            themeConfig = uiState.themeConfig,
            onSelectPreset = onSelectThemePreset,
            onSelectAnimationScale = onSelectAnimationScale,
            onSelectCornerRadius = onSelectCornerRadius,
            onDismiss = { showThemePicker = false }
        )
    }

    if (showCustomizationDialog) {
        CustomizationDialog(
            config = uiState.customizationConfig,
            installedApps = uiState.apps,
            onUpdateLayout = onUpdateLayout,
            onUpdateDock = onUpdateDock,
            onUpdateGrid = onUpdateGrid,
            onUpdateSearch = onUpdateSearch,
            onUpdateTypography = onUpdateTypography,
            onUpdateIcons = onUpdateIcons,
            onPinApp = onPinApp,
            onUnpinApp = onUnpinApp,
            onResetDefaults = onResetCustomizationDefaults,
            onOpenRiceStudio = onOpenRiceStudio,
            onDismiss = { showCustomizationDialog = false }
        )
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            providers = uiState.availableWidgetProviders,
            onSelectProvider = { provider ->
                onPlaceWidget(provider)
                showWidgetPicker = false
            },
            onDismiss = { showWidgetPicker = false }
        )
    }
}

@Composable
private fun DefaultLauncherBanner(
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
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
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "✕",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textSecondary,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun WaybarTopBar(
    workspaces: List<Workspace>,
    activeWorkspaceId: Int,
    onWorkspaceSelected: (Int) -> Unit,
    onOpenWorkspaceManager: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onOpenCustomization: () -> Unit,
    onOpenRiceStudio: () -> Unit,
    onOpenWidgetPicker: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenDrawer: () -> Unit,
    performanceModeName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Workspace selector pill: [1] [2] [3] [4] [5] [+]
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
                    Workspace(it, "WS $it", it)
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
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    modifier = Modifier.clickable { onOpenWorkspaceManager() }
                ) {
                    Text(
                        text = "+",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = HyprTheme.typography.statusModule,
                        color = HyprTheme.colors.accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Status indicator modules + Rofi drawer button + Theme picker button + Rice customization button + Widget button
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary),
                modifier = Modifier.clickable { onOpenThemePicker() }
            ) {
                Text(
                    text = "theme",
                    style = HyprTheme.typography.statusModule,
                    color = HyprTheme.colors.accentSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.terminalGreen),
                modifier = Modifier.clickable { onOpenCustomization() }
            ) {
                Text(
                    text = "rice",
                    style = HyprTheme.typography.statusModule,
                    color = HyprTheme.colors.terminalGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
                modifier = Modifier.clickable { onOpenRiceStudio() }
            ) {
                Text(
                    text = "studio",
                    style = HyprTheme.typography.statusModule,
                    color = HyprTheme.colors.accent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary),
                modifier = Modifier.clickable { onOpenWidgetPicker() }
            ) {
                Text(
                    text = "widget",
                    style = HyprTheme.typography.statusModule,
                    color = HyprTheme.colors.accentSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary),
                modifier = Modifier.clickable { onOpenDiagnostics() }
            ) {
                Text(
                    text = "perf",
                    style = HyprTheme.typography.statusModule,
                    color = HyprTheme.colors.accentSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
