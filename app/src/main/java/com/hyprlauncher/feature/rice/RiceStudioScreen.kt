package com.hyprlauncher.feature.rice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyprlauncher.core.designsystem.theme.HyprColors
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.core.designsystem.theme.ThemePresets
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.FontFamilyPreference
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.RiceDraft
import com.hyprlauncher.domain.model.RiceProfile
import com.hyprlauncher.domain.model.ThemeConfig
import com.hyprlauncher.domain.model.TypographyConfig

/**
 * Rice Studio primary creation environment conforming to PRD Section 23 & Phase 9.
 * Coordinates:
 * - Interactive miniature live preview canvas
 * - In-memory draft state (does not mutate permanent storage until applied)
 * - Profile save, duplicate, rename, delete, export, and import
 */
@Composable
fun RiceStudioScreen(
    viewModel: RiceStudioViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showProfileListModal by remember { mutableStateOf(false) }
    var showSaveAsModal by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = HyprTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Header Bar
            StudioHeader(
                activeName = uiState.draft.name,
                isDirty = uiState.draft.isDirty,
                onOpenProfiles = { showProfileListModal = true },
                onOpenImport = { viewModel.openImportModal() },
                onOpenExport = {
                    val profile = uiState.activeProfile ?: uiState.draft.toProfile()
                    viewModel.openExportModal(profile)
                },
                onClose = onNavigateBack
            )

            // Status message toast
            if (uiState.statusMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                StatusBanner(
                    message = uiState.statusMessage!!,
                    isError = false,
                    onDismiss = { viewModel.clearMessages() }
                )
            }
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                StatusBanner(
                    message = uiState.errorMessage!!,
                    isError = true,
                    onDismiss = { viewModel.clearMessages() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Editor Body: Live Preview Canvas (Top half) + Controls (Bottom half)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Interactive Miniature Live Preview Canvas (PRD Section 23)
                LivePreviewCanvas(
                    draft = uiState.draft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Studio Section Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(RiceStudioSection.values()) { section ->
                        val isSelected = section == uiState.currentSection
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.surface,
                            border = BorderStroke(
                                HyprTheme.shapes.borderWidth,
                                if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.border
                            ),
                            modifier = Modifier.clickable { viewModel.selectSection(section) }
                        ) {
                            Text(
                                text = section.title,
                                style = HyprTheme.typography.monospaceSmall,
                                color = if (isSelected) HyprTheme.colors.background else HyprTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Section Editor Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (uiState.currentSection) {
                        RiceStudioSection.COLORS -> StudioColorsTab(
                            themeConfig = uiState.draft.themeConfig,
                            onUpdate = { viewModel.updateDraftTheme(it) }
                        )
                        RiceStudioSection.LAYOUT -> StudioLayoutTab(
                            layoutConfig = uiState.draft.customizationConfig.layout,
                            onUpdate = { viewModel.updateDraftLayout(it) }
                        )
                        RiceStudioSection.GRID_DOCK -> StudioGridDockTab(
                            gridConfig = uiState.draft.customizationConfig.grid,
                            dockConfig = uiState.draft.customizationConfig.dock,
                            onUpdateGrid = { viewModel.updateDraftGrid(it) },
                            onUpdateDock = { viewModel.updateDraftDock(it) }
                        )
                        RiceStudioSection.TYPOGRAPHY -> StudioTypographyTab(
                            typographyConfig = uiState.draft.customizationConfig.typography,
                            onUpdate = { viewModel.updateDraftTypography(it) }
                        )
                        RiceStudioSection.ICONS -> StudioIconsTab(
                            iconConfig = uiState.draft.customizationConfig.icons,
                            onUpdate = { viewModel.updateDraftIcons(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Action Bar: Discard, Save As, Apply to Launcher
            StudioFooterBar(
                isDirty = uiState.draft.isDirty,
                onDiscard = { viewModel.discardDraft() },
                onSave = { viewModel.saveDraft() },
                onSaveAsNew = { showSaveAsModal = true },
                onApply = { viewModel.applyDraftToLauncher() }
            )
        }
    }

    if (showProfileListModal) {
        ProfileListDialog(
            profiles = uiState.profiles,
            activeProfileId = uiState.activeProfile?.id,
            onSelect = {
                viewModel.selectProfile(it)
                showProfileListModal = false
            },
            onActivate = { viewModel.applyProfile(it) },
            onDuplicate = { id, name -> viewModel.duplicateProfile(id, name) },
            onRename = { id, name -> viewModel.renameProfile(id, name) },
            onDelete = { viewModel.deleteProfile(it) },
            onDismiss = { showProfileListModal = false }
        )
    }

    if (showSaveAsModal) {
        SaveAsDialog(
            initialName = "${uiState.draft.name} (Custom)",
            onSave = {
                viewModel.saveDraftAsNew(it)
                showSaveAsModal = false
            },
            onDismiss = { showSaveAsModal = false }
        )
    }

    if (uiState.isExportModalVisible && uiState.exportedJson != null) {
        ExportRiceDialog(
            jsonString = uiState.exportedJson!!,
            onDismiss = { viewModel.closeExportModal() }
        )
    }

    if (uiState.isImportModalVisible) {
        ImportRiceDialog(
            onImport = { viewModel.importRiceProfile(it) },
            onDismiss = { viewModel.closeImportModal() }
        )
    }
}

@Composable
private fun StudioHeader(
    activeName: String,
    isDirty: Boolean,
    onOpenProfiles: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenExport: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "hyprctl ~ rice studio",
                    style = HyprTheme.typography.monospaceLarge,
                    color = HyprTheme.colors.accent,
                    fontWeight = FontWeight.Bold
                )
                if (isDirty) {
                    Text(
                        text = " [*]",
                        style = HyprTheme.typography.monospaceMedium,
                        color = HyprTheme.colors.warning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "profile: $activeName",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textSecondary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderChip(text = "profiles", color = HyprTheme.colors.accentSecondary, onClick = onOpenProfiles)
            HeaderChip(text = "export", color = HyprTheme.colors.terminalGreen, onClick = onOpenExport)
            HeaderChip(text = "import", color = HyprTheme.colors.warning, onClick = onOpenImport)
            HeaderChip(text = "✕", color = HyprTheme.colors.textSecondary, onClick = onClose)
        }
    }
}

@Composable
private fun HeaderChip(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, color),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = HyprTheme.typography.monospaceSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LivePreviewCanvas(
    draft: RiceDraft,
    modifier: Modifier = Modifier
) {
    val draftColors = draft.themeConfig.resolvedColors
    val layout = draft.customizationConfig.layout
    val grid = draft.customizationConfig.grid
    val dock = draft.customizationConfig.dock
    val icons = draft.customizationConfig.icons
    val typography = draft.customizationConfig.typography

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (layout.wallpaperAmoledMode) Color.Black else draftColors.background,
        border = BorderStroke(2.dp, draftColors.accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Miniature Waybar Status Bar
            if (layout.showWaybar) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = draftColors.surface,
                    border = BorderStroke(1.dp, draftColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[1] [2] [3]",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                            color = draftColors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "hyprland ~ 13:37",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                            color = draftColors.textSecondary
                        )
                    }
                }
            }

            // Miniature Clock & Search Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (layout.showClock) {
                    val fontFam = when (typography.fontFamily) {
                        FontFamilyPreference.MONOSPACE -> FontFamily.Monospace
                        FontFamilyPreference.SERIF -> FontFamily.Serif
                        FontFamilyPreference.SANS_SERIF -> FontFamily.SansSerif
                    }
                    Text(
                        text = "13:37",
                        style = TextStyle(
                            fontFamily = fontFam,
                            fontWeight = FontWeight.Bold,
                            fontSize = (22 * typography.fontScale).sp
                        ),
                        color = draftColors.accent
                    )
                    if (layout.showDate) {
                        Text(
                            text = "FRI · 03 OCT",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                            color = draftColors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (layout.showSearchBar) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = draftColors.surfaceElevated,
                        border = BorderStroke(1.dp, draftColors.border),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(
                            text = "> search apps...",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                            color = draftColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Miniature App Grid Preview
                if (layout.showAppGrid) {
                    val clipShape = when (icons.shape) {
                        IconShape.ROUNDED_SQUARE -> RoundedCornerShape(4.dp)
                        IconShape.CIRCLE -> CircleShape
                        IconShape.SQUIRCLE -> RoundedCornerShape(8.dp)
                        IconShape.SYSTEM_DEFAULT -> RoundedCornerShape(4.dp)
                    }
                    val iconColor = when (icons.tint) {
                        IconTint.THEME_ACCENT -> draftColors.accent
                        IconTint.THEME_PRIMARY -> draftColors.textPrimary
                        IconTint.NONE -> draftColors.accentSecondary
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val numCols = grid.columns.coerceIn(2, 6)
                        (1..numCols).forEach { _ ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(clipShape)
                                    .background(iconColor)
                            )
                        }
                    }
                }
            }

            // Miniature Bottom Dock
            if (layout.showDock && dock.enabled) {
                Surface(
                    shape = RoundedCornerShape(dock.cornerRadiusDp.dp / 2),
                    color = draftColors.surfaceElevated.copy(alpha = dock.backgroundAlpha),
                    border = BorderStroke(1.dp, draftColors.border),
                    modifier = Modifier.fillMaxWidth(0.85f).align(Alignment.CenterHorizontally)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        (1..4).forEach { _ ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(draftColors.accent)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioColorsTab(
    themeConfig: ThemeConfig,
    onUpdate: (ThemeConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Preset Color Palettes",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
        items(ThemePresetId.entries.toTypedArray()) { preset ->
            val isSelected = themeConfig.presetId == preset
            val presetColors = ThemePresets.forPreset(preset)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) HyprTheme.colors.surfaceElevated else HyprTheme.colors.surface,
                border = BorderStroke(
                    HyprTheme.shapes.borderWidth,
                    if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdate(themeConfig.copy(presetId = preset)) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.displayName,
                        style = HyprTheme.typography.monospaceSmall,
                        color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ColorDot(presetColors.accent)
                        ColorDot(presetColors.accentSecondary)
                        ColorDot(presetColors.background)
                        ColorDot(presetColors.surface)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Surface(
        modifier = Modifier.size(14.dp),
        shape = CircleShape,
        color = color,
        border = BorderStroke(0.5.dp, Color.Gray)
    ) {}
}

@Composable
private fun StudioLayoutTab(
    layoutConfig: LayoutConfig,
    onUpdate: (LayoutConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            StudioToggle(
                title = "Waybar Status Bar",
                checked = layoutConfig.showWaybar,
                onCheckedChange = { onUpdate(layoutConfig.copy(showWaybar = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Clock Widget",
                checked = layoutConfig.showClock,
                onCheckedChange = { onUpdate(layoutConfig.copy(showClock = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Date Display",
                checked = layoutConfig.showDate,
                onCheckedChange = { onUpdate(layoutConfig.copy(showDate = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Search Bar",
                checked = layoutConfig.showSearchBar,
                onCheckedChange = { onUpdate(layoutConfig.copy(showSearchBar = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Application Grid",
                checked = layoutConfig.showAppGrid,
                onCheckedChange = { onUpdate(layoutConfig.copy(showAppGrid = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Bottom Dock",
                checked = layoutConfig.showDock,
                onCheckedChange = { onUpdate(layoutConfig.copy(showDock = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Pure Black AMOLED",
                checked = layoutConfig.wallpaperAmoledMode,
                onCheckedChange = { onUpdate(layoutConfig.copy(wallpaperAmoledMode = it)) }
            )
        }
    }
}

@Composable
private fun StudioGridDockTab(
    gridConfig: AppGridConfig,
    dockConfig: DockConfig,
    onUpdateGrid: (AppGridConfig) -> Unit,
    onUpdateDock: (DockConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            StudioChips(
                title = "Grid Columns",
                options = listOf(3 to "3 cols", 4 to "4 cols", 5 to "5 cols", 6 to "6 cols"),
                selected = gridConfig.columns,
                onSelect = { onUpdateGrid(gridConfig.copy(columns = it)) }
            )
        }
        item {
            StudioChips(
                title = "Grid Icon Size",
                options = listOf(32 to "32dp", 36 to "36dp", 42 to "42dp", 48 to "48dp"),
                selected = gridConfig.iconSizeDp,
                onSelect = { onUpdateGrid(gridConfig.copy(iconSizeDp = it)) }
            )
        }
        item {
            StudioChips(
                title = "Dock Icon Size",
                options = listOf(36 to "36dp", 40 to "40dp", 48 to "48dp", 56 to "56dp"),
                selected = dockConfig.iconSizeDp,
                onSelect = { onUpdateDock(dockConfig.copy(iconSizeDp = it)) }
            )
        }
        item {
            StudioChips(
                title = "Dock Corner Radius",
                options = listOf(8 to "8dp", 16 to "16dp", 24 to "24dp", 32 to "32dp"),
                selected = dockConfig.cornerRadiusDp,
                onSelect = { onUpdateDock(dockConfig.copy(cornerRadiusDp = it)) }
            )
        }
        item {
            StudioChips(
                title = "Dock Opacity",
                options = listOf(0.6f to "60%", 0.8f to "80%", 0.95f to "95%", 1.0f to "100%"),
                selected = dockConfig.backgroundAlpha,
                onSelect = { onUpdateDock(dockConfig.copy(backgroundAlpha = it)) }
            )
        }
    }
}

@Composable
private fun StudioTypographyTab(
    typographyConfig: TypographyConfig,
    onUpdate: (TypographyConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            StudioChips(
                title = "Font Family",
                options = listOf(
                    FontFamilyPreference.SANS_SERIF to "Modern Sans",
                    FontFamilyPreference.MONOSPACE to "Monospace",
                    FontFamilyPreference.SERIF to "Serif"
                ),
                selected = typographyConfig.fontFamily,
                onSelect = { onUpdate(typographyConfig.copy(fontFamily = it)) }
            )
        }
        item {
            StudioChips(
                title = "Font Scaling",
                options = listOf(0.85f to "85%", 1.0f to "100%", 1.15f to "115%", 1.30f to "130%"),
                selected = typographyConfig.fontScale,
                onSelect = { onUpdate(typographyConfig.copy(fontScale = it)) }
            )
        }
        item {
            StudioChips(
                title = "Letter Spacing",
                options = listOf(0.0f to "0sp", 0.5f to "+0.5sp", 1.0f to "+1.0sp"),
                selected = typographyConfig.letterSpacingSp,
                onSelect = { onUpdate(typographyConfig.copy(letterSpacingSp = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Monospace for Status Only",
                checked = typographyConfig.useMonospaceForStatusOnly,
                onCheckedChange = { onUpdate(typographyConfig.copy(useMonospaceForStatusOnly = it)) }
            )
        }
    }
}

@Composable
private fun StudioIconsTab(
    iconConfig: IconConfig,
    onUpdate: (IconConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            StudioChips(
                title = "Icon Shape Mask",
                options = listOf(
                    IconShape.SYSTEM_DEFAULT to "System",
                    IconShape.ROUNDED_SQUARE to "Rounded",
                    IconShape.CIRCLE to "Circle",
                    IconShape.SQUIRCLE to "Squircle"
                ),
                selected = iconConfig.shape,
                onSelect = { onUpdate(iconConfig.copy(shape = it)) }
            )
        }
        item {
            StudioChips(
                title = "Icon Tinting Mode",
                options = listOf(
                    IconTint.NONE to "Original Colors",
                    IconTint.THEME_ACCENT to "Theme Accent",
                    IconTint.THEME_PRIMARY to "High Contrast"
                ),
                selected = iconConfig.tint,
                onSelect = { onUpdate(iconConfig.copy(tint = it)) }
            )
        }
        item {
            StudioChips(
                title = "Icon Scaling",
                options = listOf(0.8f to "80%", 1.0f to "100%", 1.2f to "120%"),
                selected = iconConfig.scale,
                onSelect = { onUpdate(iconConfig.copy(scale = it)) }
            )
        }
        item {
            StudioToggle(
                title = "Show App Labels",
                checked = iconConfig.showAppLabels,
                onCheckedChange = { onUpdate(iconConfig.copy(showAppLabels = it)) }
            )
        }
    }
}

@Composable
private fun StudioToggle(
    title: String,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textPrimary
            )
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
private fun <T> StudioChips(
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
            modifier = Modifier.padding(vertical = 2.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioFooterBar(
    isDirty: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onSaveAsNew: () -> Unit,
    onApply: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDirty) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surface,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.error),
                modifier = Modifier.clickable { onDiscard() }
            ) {
                Text(
                    text = "[discard]",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                modifier = Modifier.clickable { onSaveAsNew() }
            ) {
                Text(
                    text = "[save as]",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary),
                modifier = Modifier.clickable { onSave() }
            ) {
                Text(
                    text = "[save]",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.accentSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = HyprTheme.colors.accent,
                modifier = Modifier.clickable { onApply() }
            ) {
                Text(
                    text = "[apply to launcher]",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.background,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isError) HyprTheme.colors.error.copy(alpha = 0.2f) else HyprTheme.colors.surfaceElevated,
        border = BorderStroke(
            HyprTheme.shapes.borderWidth,
            if (isError) HyprTheme.colors.error else HyprTheme.colors.terminalGreen
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = HyprTheme.typography.monospaceSmall,
                color = if (isError) HyprTheme.colors.error else HyprTheme.colors.terminalGreen
            )
            Text(
                text = "✕",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ProfileListDialog(
    profiles: List<RiceProfile>,
    activeProfileId: String?,
    onSelect: (RiceProfile) -> Unit,
    onActivate: (String) -> Unit,
    onDuplicate: (String, String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.background,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accentSecondary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED RICE PROFILES (${profiles.size})",
                        style = HyprTheme.typography.monospaceMedium,
                        color = HyprTheme.colors.accentSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surface,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "✕",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        val isActive = profile.id == activeProfileId
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HyprTheme.colors.surface,
                            border = BorderStroke(
                                HyprTheme.shapes.borderWidth,
                                if (isActive) HyprTheme.colors.accent else HyprTheme.colors.border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = profile.name,
                                                style = HyprTheme.typography.monospaceSmall,
                                                color = HyprTheme.colors.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "[ACTIVE]",
                                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                                    color = HyprTheme.colors.terminalGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "theme: ${profile.themeConfig.presetId.displayName} | grid: ${profile.customizationConfig.grid.columns}x${profile.customizationConfig.grid.rows}",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                                            color = HyprTheme.colors.textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = HyprTheme.colors.surfaceElevated,
                                        modifier = Modifier.clickable { onSelect(profile) }
                                    ) {
                                        Text(
                                            text = "load in studio",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.accent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (!isActive) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = HyprTheme.colors.surfaceElevated,
                                            modifier = Modifier.clickable { onActivate(profile.id) }
                                        ) {
                                            Text(
                                                text = "activate",
                                                style = HyprTheme.typography.monospaceSmall,
                                                color = HyprTheme.colors.terminalGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = HyprTheme.colors.surfaceElevated,
                                        modifier = Modifier.clickable {
                                            onDuplicate(profile.id, "${profile.name} (Copy)")
                                        }
                                    ) {
                                        Text(
                                            text = "duplicate",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (profiles.size > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = HyprTheme.colors.surfaceElevated,
                                            modifier = Modifier.clickable { onDelete(profile.id) }
                                        ) {
                                            Text(
                                                text = "delete",
                                                style = HyprTheme.typography.monospaceSmall,
                                                color = HyprTheme.colors.error,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    }
}

@Composable
private fun SaveAsDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = HyprTheme.shapes.small,
            color = HyprTheme.colors.background,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SAVE RICE AS NEW PROFILE",
                    style = HyprTheme.typography.monospaceMedium,
                    color = HyprTheme.colors.accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = HyprTheme.colors.textPrimary
                    ),
                    cursorBrush = SolidColor(HyprTheme.colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HyprTheme.colors.surface, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surface,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "cancel",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.accent,
                        modifier = Modifier.clickable { onSave(name) }
                    ) {
                        Text(
                            text = "save",
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
private fun ExportRiceDialog(
    jsonString: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.background,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.terminalGreen)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "EXPORT RICE PROFILE (JSON)",
                    style = HyprTheme.typography.monospaceMedium,
                    color = HyprTheme.colors.terminalGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surface,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        item {
                            Text(
                                text = jsonString,
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = HyprTheme.colors.textPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.terminalGreen,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "[close]",
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
private fun ImportRiceDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.background,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.warning)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "IMPORT RICE PROFILE (JSON)",
                    style = HyprTheme.typography.monospaceMedium,
                    color = HyprTheme.colors.warning,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste rice profile JSON configuration below:",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = HyprTheme.colors.textPrimary
                    ),
                    cursorBrush = SolidColor(HyprTheme.colors.warning),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(HyprTheme.colors.surface, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surface,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "cancel",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.warning,
                        modifier = Modifier.clickable {
                            if (jsonText.isNotBlank()) onImport(jsonText)
                        }
                    ) {
                        Text(
                            text = "import & parse",
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
