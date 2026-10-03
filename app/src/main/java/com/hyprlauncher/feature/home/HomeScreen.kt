package com.hyprlauncher.feature.home

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.component.HyprSurfaceCard
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.data.database.entity.AppEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onWorkspaceSelected: (Int) -> Unit,
    onAppClick: (String, String?) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(uiState.preferences.clock24Hour, uiState.preferences.showClockSeconds) {
        val timePattern = when {
            uiState.preferences.clock24Hour && uiState.preferences.showClockSeconds -> "HH:mm:ss"
            uiState.preferences.clock24Hour -> "HH:mm"
            uiState.preferences.showClockSeconds -> "hh:mm:ss a"
            else -> "hh:mm a"
        }
        val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE · dd MMM", Locale.getDefault())

        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now).uppercase()
            currentDateString = dateFormat.format(now).uppercase()
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HyprTheme.colors.background)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Waybar status bar + optional default launcher banner
            Column {
                WaybarTopBar(
                    workspaces = uiState.workspaces,
                    activeWorkspaceId = uiState.preferences.activeWorkspaceId,
                    onWorkspaceSelected = onWorkspaceSelected,
                    performanceModeName = uiState.preferences.performanceMode.name
                )

                if (!uiState.isDefaultLauncher) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DefaultLauncherBanner(onClick = onSetDefaultLauncher)
                }
            }

            // Center: Hyprland Minimal Clock & Command Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTimeString.ifEmpty { "00:00" },
                    style = HyprTheme.typography.displayLarge,
                    color = HyprTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentDateString.ifEmpty { "HYPR · LAUNCHER" },
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.accent,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Rofi Search prompt
                HyprSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .clickable { /* Search in Phase 4 */ },
                    backgroundColor = HyprTheme.colors.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ">",
                            style = HyprTheme.typography.monospaceLarge,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Search or command (rofi)...",
                            style = HyprTheme.typography.bodyMedium,
                            color = HyprTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Bottom Section: Quick App Launcher Grid + Waybar footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.apps.isNotEmpty()) {
                    // Quick access app grid (shows first 8 launchable apps)
                    val quickApps = uiState.apps.take(8)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickApps, key = { it.packageName }) { app ->
                            AppGridItem(
                                app = app,
                                onClick = { onAppClick(app.packageName, app.activityName) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.isLoading) "Scanning applications..." else "No applications indexed",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Waybar Footer Telemetry
                HyprSurfaceCard(
                    modifier = Modifier.fillMaxWidth(0.96f),
                    backgroundColor = HyprTheme.colors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
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
private fun AppGridItem(
    app: AppEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surface,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // First letter badge with monospace styling
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = app.label,
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
    workspaces: List<com.hyprlauncher.data.database.entity.WorkspaceEntity>,
    activeWorkspaceId: Int,
    onWorkspaceSelected: (Int) -> Unit,
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
                    com.hyprlauncher.data.database.entity.WorkspaceEntity(it, "WS $it", it)
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

        // Status indicator modules
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
