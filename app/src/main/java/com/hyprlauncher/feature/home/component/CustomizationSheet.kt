package com.hyprlauncher.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.FontFamilyPreference
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.SearchConfig
import com.hyprlauncher.domain.model.SearchRankingMode
import com.hyprlauncher.domain.model.TypographyConfig

private enum class CustomizationTab(val title: String) {
    LAYOUT("LAYOUT"),
    DOCK("DOCK"),
    GRID("GRID"),
    SEARCH("SEARCH"),
    TYPE("TYPE"),
    ICONS("ICONS")
}

@Composable
fun CustomizationDialog(
    config: CustomizationConfig,
    installedApps: List<AppEntity>,
    onUpdateLayout: (LayoutConfig) -> Unit,
    onUpdateDock: (DockConfig) -> Unit,
    onUpdateGrid: (AppGridConfig) -> Unit,
    onUpdateSearch: (SearchConfig) -> Unit,
    onUpdateTypography: (TypographyConfig) -> Unit,
    onUpdateIcons: (IconConfig) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onOpenRiceStudio: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CustomizationTab.LAYOUT) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.88f),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.background,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                // Header: Terminal Window Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "hyprctl ~ config",
                            style = HyprTheme.typography.monospaceLarge,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "system customization & ricing",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HyprTheme.colors.surfaceElevated,
                            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
                            modifier = Modifier.clickable {
                                onDismiss()
                                onOpenRiceStudio()
                            }
                        ) {
                            Text(
                                text = "rice studio ↗",
                                style = HyprTheme.typography.monospaceSmall,
                                color = HyprTheme.colors.accent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HyprTheme.colors.surfaceElevated,
                            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                            modifier = Modifier.clickable { onDismiss() }
                        ) {
                            Text(
                                text = "✕",
                                style = HyprTheme.typography.monospaceMedium,
                                color = HyprTheme.colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Waybar Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CustomizationTab.values()) { tab ->
                        val isSelected = tab == selectedTab
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.surface,
                            border = BorderStroke(
                                HyprTheme.shapes.borderWidth,
                                if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.border
                            ),
                            modifier = Modifier.clickable { selectedTab = tab }
                        ) {
                            Text(
                                text = tab.title,
                                style = HyprTheme.typography.monospaceSmall,
                                color = if (isSelected) HyprTheme.colors.background else HyprTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        CustomizationTab.LAYOUT -> LayoutTabContent(
                            layout = config.layout,
                            onUpdate = onUpdateLayout
                        )
                        CustomizationTab.DOCK -> DockTabContent(
                            dock = config.dock,
                            installedApps = installedApps,
                            onUpdate = onUpdateDock,
                            onPinApp = onPinApp,
                            onUnpinApp = onUnpinApp
                        )
                        CustomizationTab.GRID -> GridTabContent(
                            grid = config.grid,
                            onUpdate = onUpdateGrid
                        )
                        CustomizationTab.SEARCH -> SearchTabContent(
                            search = config.search,
                            onUpdate = onUpdateSearch
                        )
                        CustomizationTab.TYPE -> TypographyTabContent(
                            typography = config.typography,
                            onUpdate = onUpdateTypography
                        )
                        CustomizationTab.ICONS -> IconsTabContent(
                            icons = config.icons,
                            onUpdate = onUpdateIcons
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Bar: Reset & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surface,
                        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.error),
                        modifier = Modifier.clickable { onResetDefaults() }
                    ) {
                        Text(
                            text = "[reset defaults]",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.accent,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "[apply & close]",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.background,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutTabContent(
    layout: LayoutConfig,
    onUpdate: (LayoutConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConfigToggleRow(
                title = "Waybar Status Bar",
                subtitle = "Display status modules & workspaces at top",
                checked = layout.showWaybar,
                onCheckedChange = { onUpdate(layout.copy(showWaybar = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "System Clock",
                subtitle = "Display large digital clock widget",
                checked = layout.showClock,
                onCheckedChange = { onUpdate(layout.copy(showClock = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Date Display",
                subtitle = "Display day of week and calendar date",
                checked = layout.showDate,
                onCheckedChange = { onUpdate(layout.copy(showDate = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Rofi Search Bar",
                subtitle = "Display instant terminal search input",
                checked = layout.showSearchBar,
                onCheckedChange = { onUpdate(layout.copy(showSearchBar = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Home Application Grid",
                subtitle = "Display indexed applications in workspaces",
                checked = layout.showAppGrid,
                onCheckedChange = { onUpdate(layout.copy(showAppGrid = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Bottom Dock",
                subtitle = "Display pinned favorite applications dock",
                checked = layout.showDock,
                onCheckedChange = { onUpdate(layout.copy(showDock = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Pure Black AMOLED Mode",
                subtitle = "Disable wallpapers and render true #000000 black",
                checked = layout.wallpaperAmoledMode,
                onCheckedChange = { onUpdate(layout.copy(wallpaperAmoledMode = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Wallpaper Dimming",
                options = listOf(0.0f to "0%", 0.2f to "20%", 0.4f to "40%", 0.6f to "60%", 0.8f to "80%"),
                selected = layout.wallpaperDim,
                onSelect = { onUpdate(layout.copy(wallpaperDim = it)) }
            )
        }
    }
}

@Composable
private fun DockTabContent(
    dock: DockConfig,
    installedApps: List<AppEntity>,
    onUpdate: (DockConfig) -> Unit,
    onPinApp: (String) -> Unit,
    onUnpinApp: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConfigToggleRow(
                title = "Enable Dock",
                subtitle = "Show bottom pinned application container",
                checked = dock.enabled,
                onCheckedChange = { onUpdate(dock.copy(enabled = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Show App Labels",
                subtitle = "Display text label under dock icons",
                checked = dock.showLabels,
                onCheckedChange = { onUpdate(dock.copy(showLabels = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Icon Size",
                options = listOf(36 to "36dp", 40 to "40dp", 48 to "48dp", 56 to "56dp"),
                selected = dock.iconSizeDp,
                onSelect = { onUpdate(dock.copy(iconSizeDp = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Item Spacing",
                options = listOf(6 to "Tight (6dp)", 10 to "Normal (10dp)", 16 to "Relaxed (16dp)"),
                selected = dock.spacingDp,
                onSelect = { onUpdate(dock.copy(spacingDp = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Corner Radius",
                options = listOf(8 to "8dp", 16 to "16dp", 24 to "24dp", 32 to "32dp"),
                selected = dock.cornerRadiusDp,
                onSelect = { onUpdate(dock.copy(cornerRadiusDp = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Container Opacity",
                options = listOf(0.5f to "50%", 0.75f to "75%", 0.95f to "95%", 1.0f to "100%"),
                selected = dock.backgroundAlpha,
                onSelect = { onUpdate(dock.copy(backgroundAlpha = it)) }
            )
        }
        item {
            Text(
                text = "Pinned Applications (${dock.pinnedPackages.size}/${dock.maxItems})",
                style = HyprTheme.typography.monospaceMedium,
                color = HyprTheme.colors.accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        if (dock.pinnedPackages.isEmpty()) {
            item {
                Text(
                    text = "No applications pinned. Pick apps below to pin to dock.",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary
                )
            }
        } else {
            items(dock.pinnedPackages) { pkg ->
                val app = installedApps.firstOrNull { it.packageName == pkg }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surface,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app?.label ?: pkg,
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HyprTheme.colors.surfaceElevated,
                            modifier = Modifier.clickable { onUnpinApp(pkg) }
                        ) {
                            Text(
                                text = "remove",
                                style = HyprTheme.typography.monospaceSmall,
                                color = HyprTheme.colors.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pin From Installed Apps",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textSecondary
            )
        }
        val unpinned = installedApps.filter { !dock.pinnedPackages.contains(it.packageName) }.take(8)
        items(unpinned) { app ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPinApp(app.packageName) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.label,
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.textPrimary
                    )
                    Text(
                        text = "+ pin",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.accent
                    )
                }
            }
        }
    }
}

@Composable
private fun GridTabContent(
    grid: AppGridConfig,
    onUpdate: (AppGridConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConfigOptionChips(
                title = "Grid Columns",
                options = listOf(2 to "2 cols", 3 to "3 cols", 4 to "4 cols", 5 to "5 cols", 6 to "6 cols"),
                selected = grid.columns,
                onSelect = { onUpdate(grid.copy(columns = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Grid Rows",
                options = listOf(3 to "3 rows", 4 to "4 rows", 5 to "5 rows", 6 to "6 rows", 7 to "7 rows"),
                selected = grid.rows,
                onSelect = { onUpdate(grid.copy(rows = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Grid Icon Size",
                options = listOf(32 to "32dp", 36 to "36dp", 42 to "42dp", 48 to "48dp", 56 to "56dp"),
                selected = grid.iconSizeDp,
                onSelect = { onUpdate(grid.copy(iconSizeDp = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Horizontal Spacing",
                options = listOf(4 to "4dp", 8 to "8dp", 12 to "12dp", 16 to "16dp"),
                selected = grid.spacingHorizontalDp,
                onSelect = { onUpdate(grid.copy(spacingHorizontalDp = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Vertical Spacing",
                options = listOf(4 to "4dp", 8 to "8dp", 12 to "12dp", 16 to "16dp"),
                selected = grid.spacingVerticalDp,
                onSelect = { onUpdate(grid.copy(spacingVerticalDp = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Show App Labels",
                subtitle = "Display text label under app tiles",
                checked = grid.showLabels,
                onCheckedChange = { onUpdate(grid.copy(showLabels = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Label Font Size",
                options = listOf(9 to "9sp", 10 to "10sp", 11 to "11sp", 12 to "12sp", 14 to "14sp"),
                selected = grid.labelFontSizeSp,
                onSelect = { onUpdate(grid.copy(labelFontSizeSp = it)) }
            )
        }
    }
}

@Composable
private fun SearchTabContent(
    search: SearchConfig,
    onUpdate: (SearchConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConfigToggleRow(
                title = "Fuzzy Matching (Levenshtein)",
                subtitle = "Match search results with up to 2 typos",
                checked = search.fuzzyMatchingEnabled,
                onCheckedChange = { onUpdate(search.copy(fuzzyMatchingEnabled = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Prefix & Acronym Matching",
                subtitle = "Match words prefixes and acronyms (e.g. yt -> YouTube)",
                checked = search.prefixMatchingEnabled,
                onCheckedChange = { onUpdate(search.copy(prefixMatchingEnabled = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Match Package Names",
                subtitle = "Include Android package names (e.g. org.mozilla.firefox)",
                checked = search.showPackageNames,
                onCheckedChange = { onUpdate(search.copy(showPackageNames = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Search History",
                subtitle = "Remember and boost recently queried apps",
                checked = search.searchHistoryEnabled,
                onCheckedChange = { onUpdate(search.copy(searchHistoryEnabled = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Ranking Mode",
                options = listOf(
                    SearchRankingMode.DETERMINISTIC_HYBRID to "Hybrid (PRD)",
                    SearchRankingMode.FREQUENCY_FIRST to "Frequency First",
                    SearchRankingMode.RECENCY_FIRST to "Recency First",
                    SearchRankingMode.ALPHABETICAL to "A-Z"
                ),
                selected = search.rankingMode,
                onSelect = { onUpdate(search.copy(rankingMode = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Search Bar Prompt",
                options = listOf(
                    "Search apps or run command (rofi)..." to "Rofi Default",
                    "> type to search..." to "Minimal",
                    "launch application..." to "Direct",
                    "$ hyprctl search" to "Hyprctl"
                ),
                selected = search.placeholderText,
                onSelect = { onUpdate(search.copy(placeholderText = it)) }
            )
        }
    }
}

@Composable
private fun TypographyTabContent(
    typography: TypographyConfig,
    onUpdate: (TypographyConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConfigOptionChips(
                title = "Primary Font Family",
                options = listOf(
                    FontFamilyPreference.SANS_SERIF to "Modern Sans",
                    FontFamilyPreference.MONOSPACE to "Monospace (Hacker)",
                    FontFamilyPreference.SERIF to "Serif (Editorial)"
                ),
                selected = typography.fontFamily,
                onSelect = { onUpdate(typography.copy(fontFamily = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Typography Scale",
                options = listOf(0.85f to "85%", 1.0f to "100%", 1.15f to "115%", 1.30f to "130%"),
                selected = typography.fontScale,
                onSelect = { onUpdate(typography.copy(fontScale = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Letter Spacing",
                options = listOf(0.0f to "Normal (0sp)", 0.5f to "Wide (+0.5sp)", 1.0f to "Ultra (+1.0sp)"),
                selected = typography.letterSpacingSp,
                onSelect = { onUpdate(typography.copy(letterSpacingSp = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Monospace for Status Only",
                subtitle = "Keep Waybar & clock monospace while body uses primary font",
                checked = typography.useMonospaceForStatusOnly,
                onCheckedChange = { onUpdate(typography.copy(useMonospaceForStatusOnly = it)) }
            )
        }
        item {
            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = HyprTheme.colors.surface,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "LIVE TYPOGRAPHY PREVIEW",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.accentSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "13:37 ~ Arch Linux",
                        style = HyprTheme.typography.displayMedium,
                        color = HyprTheme.colors.accent
                    )
                    Text(
                        text = "The quick brown fox jumps over the lazy dog.",
                        style = HyprTheme.typography.bodyLarge,
                        color = HyprTheme.colors.textPrimary
                    )
                    Text(
                        text = "[hyprland] 4 workspaces active | mem: 42%",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.terminalGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun IconsTabContent(
    icons: IconConfig,
    onUpdate: (IconConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ConfigOptionChips(
                title = "Icon Shape Mask",
                options = listOf(
                    IconShape.SYSTEM_DEFAULT to "System",
                    IconShape.ROUNDED_SQUARE to "Rounded",
                    IconShape.CIRCLE to "Circle",
                    IconShape.SQUIRCLE to "Squircle"
                ),
                selected = icons.shape,
                onSelect = { onUpdate(icons.copy(shape = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Icon Scaling",
                options = listOf(0.8f to "80%", 1.0f to "100%", 1.2f to "120%"),
                selected = icons.scale,
                onSelect = { onUpdate(icons.copy(scale = it)) }
            )
        }
        item {
            ConfigOptionChips(
                title = "Icon Tinting Mode",
                options = listOf(
                    IconTint.NONE to "Original Colors",
                    IconTint.THEME_ACCENT to "Theme Accent",
                    IconTint.THEME_PRIMARY to "High Contrast"
                ),
                selected = icons.tint,
                onSelect = { onUpdate(icons.copy(tint = it)) }
            )
        }
        item {
            ConfigToggleRow(
                title = "Show Application Labels",
                subtitle = "Display readable application titles across launcher",
                checked = icons.showAppLabels,
                onCheckedChange = { onUpdate(icons.copy(showAppLabels = it)) }
            )
        }
        item {
            // Live Icon Preview Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = HyprTheme.colors.surface,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LIVE ICON SHAPE PREVIEW",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clipShape = when (icons.shape) {
                            IconShape.ROUNDED_SQUARE -> RoundedCornerShape(8.dp)
                            IconShape.CIRCLE -> CircleShape
                            IconShape.SQUIRCLE -> RoundedCornerShape(30)
                            IconShape.SYSTEM_DEFAULT -> RoundedCornerShape(10.dp)
                        }

                        val sampleColors = listOf(
                            HyprTheme.colors.accent,
                            HyprTheme.colors.accentSecondary,
                            HyprTheme.colors.terminalGreen
                        )
                        val sampleChars = listOf("A", "H", "F")

                        sampleChars.forEachIndexed { idx, char ->
                            Surface(
                                modifier = Modifier
                                    .size((44 * icons.scale).toInt().dp)
                                    .clip(clipShape),
                                color = when (icons.tint) {
                                    IconTint.THEME_ACCENT -> HyprTheme.colors.accent
                                    IconTint.THEME_PRIMARY -> HyprTheme.colors.textPrimary
                                    IconTint.NONE -> sampleColors[idx]
                                },
                                shape = clipShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = char,
                                        style = HyprTheme.typography.statusModule,
                                        color = HyprTheme.colors.background,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = HyprTheme.colors.surface,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = HyprTheme.typography.monospaceSmall.copy(fontSize = 10.sp),
                    color = HyprTheme.colors.textSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HyprTheme.colors.background,
                    checkedTrackColor = HyprTheme.colors.accent,
                    uncheckedThumbColor = HyprTheme.colors.textSecondary,
                    uncheckedTrackColor = HyprTheme.colors.surfaceElevated
                )
            )
        }
    }
}

@Composable
private fun <T> ConfigOptionChips(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = HyprTheme.typography.monospaceSmall,
            color = HyprTheme.colors.accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { (value, label) ->
                val isSelected = value == selected
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.surface,
                    border = BorderStroke(
                        HyprTheme.shapes.borderWidth,
                        if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.border
                    ),
                    modifier = Modifier.clickable { onSelect(value) }
                ) {
                    Text(
                        text = label,
                        style = HyprTheme.typography.monospaceSmall,
                        color = if (isSelected) HyprTheme.colors.background else HyprTheme.colors.textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
