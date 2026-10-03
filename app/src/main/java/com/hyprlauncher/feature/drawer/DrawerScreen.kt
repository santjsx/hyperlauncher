package com.hyprlauncher.feature.drawer

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.core.search.MatchType
import com.hyprlauncher.core.search.SearchMatchResult
import com.hyprlauncher.data.database.entity.AppEntity

/**
 * Rofi-inspired App Drawer conforming to PRD Section 12, 14, 15 and 20.
 */
@Composable
fun DrawerScreen(
    uiState: DrawerUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (DrawerTab) -> Unit,
    onAppClick: (String, String?) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    loadIcon: suspend (String) -> Bitmap? = { null },
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(HyprTheme.colors.background)
            .safeDrawingPadding(),
        color = HyprTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Rofi Terminal Window Titlebar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "rofi -dmenu",
                        style = HyprTheme.typography.monospaceMedium,
                        color = HyprTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "apps: ${uiState.totalApps}",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                    modifier = Modifier.clickable { onDismiss() }
                ) {
                    Text(
                        text = "[close / esc]",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.accentSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Command / Search input field
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = HyprTheme.shapes.small,
                color = HyprTheme.colors.surfaceElevated,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">",
                        style = HyprTheme.typography.monospaceLarge,
                        color = HyprTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                text = "Type to search or launch...",
                                style = HyprTheme.typography.bodyMedium,
                                color = HyprTheme.colors.textSecondary
                            )
                        }

                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = HyprTheme.colors.textPrimary
                            ),
                            cursorBrush = SolidColor(HyprTheme.colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    val topResult = uiState.searchResults.firstOrNull()?.app
                                        ?: uiState.allApps.firstOrNull()
                                    if (topResult != null) {
                                        onAppClick(topResult.packageName, topResult.activityName)
                                    }
                                }
                            )
                        )
                    }

                    if (uiState.searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✕",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary,
                            modifier = Modifier
                                .clickable { onSearchQueryChange("") }
                                .padding(4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation tabs when search query is empty
            if (uiState.searchQuery.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DrawerTabPill(
                        label = "all",
                        count = uiState.totalApps,
                        isSelected = uiState.selectedTab == DrawerTab.ALL,
                        onClick = { onSelectTab(DrawerTab.ALL) }
                    )
                    DrawerTabPill(
                        label = "favorites",
                        count = uiState.favoriteApps.size,
                        isSelected = uiState.selectedTab == DrawerTab.FAVORITES,
                        onClick = { onSelectTab(DrawerTab.FAVORITES) }
                    )
                    DrawerTabPill(
                        label = "recent",
                        count = uiState.recentApps.size,
                        isSelected = uiState.selectedTab == DrawerTab.RECENTS,
                        onClick = { onSelectTab(DrawerTab.RECENTS) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Results List Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.searchQuery.isNotEmpty()) {
                    if (uiState.searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No applications matching '${uiState.searchQuery}'",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.textSecondary
                                )
                            }
                        }
                    } else {
                        items(uiState.searchResults, key = { it.app.packageName }) { match ->
                            SearchResultRow(
                                match = match,
                                loadIcon = loadIcon,
                                onAppClick = { onAppClick(match.app.packageName, match.app.activityName) },
                                onToggleFavorite = { onToggleFavorite(match.app.packageName, match.app.isFavorite) }
                            )
                        }
                    }
                } else {
                    when (uiState.selectedTab) {
                        DrawerTab.ALL -> {
                            uiState.alphabeticalGroups.forEach { (char, apps) ->
                                item(key = "header_$char") {
                                    Text(
                                        text = "[$char]",
                                        style = HyprTheme.typography.monospaceMedium,
                                        color = HyprTheme.colors.accent,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(apps, key = { it.packageName }) { app ->
                                    AppDrawerRow(
                                        app = app,
                                        loadIcon = loadIcon,
                                        onAppClick = { onAppClick(app.packageName, app.activityName) },
                                        onToggleFavorite = { onToggleFavorite(app.packageName, app.isFavorite) }
                                    )
                                }
                            }
                        }
                        DrawerTab.FAVORITES -> {
                            if (uiState.favoriteApps.isEmpty()) {
                                item {
                                    EmptyTabNotice("No favorites pinned yet. Tap ★ on any app.")
                                }
                            } else {
                                items(uiState.favoriteApps, key = { it.packageName }) { app ->
                                    AppDrawerRow(
                                        app = app,
                                        loadIcon = loadIcon,
                                        onAppClick = { onAppClick(app.packageName, app.activityName) },
                                        onToggleFavorite = { onToggleFavorite(app.packageName, app.isFavorite) }
                                    )
                                }
                            }
                        }
                        DrawerTab.RECENTS -> {
                            if (uiState.recentApps.isEmpty()) {
                                item {
                                    EmptyTabNotice("No recent applications recorded.")
                                }
                            } else {
                                items(uiState.recentApps, key = { it.packageName }) { app ->
                                    AppDrawerRow(
                                        app = app,
                                        loadIcon = loadIcon,
                                        onAppClick = { onAppClick(app.packageName, app.activityName) },
                                        onToggleFavorite = { onToggleFavorite(app.packageName, app.isFavorite) }
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
private fun SearchResultRow(
    match: SearchMatchResult,
    loadIcon: suspend (String) -> Bitmap?,
    onAppClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AppDrawerRow(
        app = match.app,
        loadIcon = loadIcon,
        onAppClick = onAppClick,
        onToggleFavorite = onToggleFavorite,
        badgeText = when (match.matchType) {
            MatchType.EXACT -> "exact"
            MatchType.PREFIX -> "prefix"
            MatchType.WORD_PREFIX -> "word"
            MatchType.ACRONYM -> "acronym"
            MatchType.SUBSEQUENCE -> "subseq"
            MatchType.FUZZY -> "fuzzy"
            MatchType.PACKAGE_NAME -> "pkg"
            MatchType.NONE -> null
        }
    )
}

@Composable
private fun AppDrawerRow(
    app: AppEntity,
    loadIcon: suspend (String) -> Bitmap?,
    onAppClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    badgeText: String? = null
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(app.packageName) {
        iconBitmap = loadIcon(app.packageName)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAppClick),
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surface,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bitmap = iconBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = HyprTheme.colors.surfaceElevated,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = app.label.firstOrNull()?.uppercase() ?: "?",
                            style = HyprTheme.typography.statusModule,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = HyprTheme.typography.bodyLarge,
                    color = HyprTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badgeText != null) {
                HyprStatusBadge(
                    text = badgeText,
                    accentColor = HyprTheme.colors.terminalGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Favorite star toggle
            Text(
                text = if (app.isFavorite) "★" else "☆",
                style = TextStyle(fontSize = 18.sp),
                color = if (app.isFavorite) HyprTheme.colors.warning else HyprTheme.colors.textSecondary,
                modifier = Modifier
                    .clickable { onToggleFavorite() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun DrawerTabPill(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = HyprTheme.typography.monospaceSmall,
                color = if (isSelected) HyprTheme.colors.background else HyprTheme.colors.textPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($count)",
                style = HyprTheme.typography.monospaceSmall,
                color = if (isSelected) HyprTheme.colors.background else HyprTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun EmptyTabNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = HyprTheme.typography.monospaceSmall,
            color = HyprTheme.colors.textSecondary
        )
    }
}
