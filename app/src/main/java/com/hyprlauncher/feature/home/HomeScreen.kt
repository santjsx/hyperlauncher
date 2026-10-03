package com.hyprlauncher.feature.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.component.HyprSurfaceCard
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onWorkspaceSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Waybar-inspired status bar
            WaybarTopBar(
                workspaces = uiState.workspaces,
                activeWorkspaceId = uiState.preferences.activeWorkspaceId,
                onWorkspaceSelected = onWorkspaceSelected,
                performanceModeName = uiState.preferences.performanceMode.name
            )

            // Center: Hyprland Minimal Clock & Search Command Palette trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTimeString.ifEmpty { "00:00" },
                    style = HyprTheme.typography.displayLarge,
                    color = HyprTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentDateString.ifEmpty { "HYPR · LAUNCHER" },
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.accent,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Rofi/Wofi-inspired Search bar prompt
                HyprSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clickable { /* Search activation in Phase 4 */ },
                    backgroundColor = HyprTheme.colors.surfaceElevated
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ">",
                            style = HyprTheme.typography.monospaceLarge,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search or command (rofi)...",
                            style = HyprTheme.typography.bodyMedium,
                            color = HyprTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Bottom: Dock shell & Telemetry summary
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HyprSurfaceCard(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    backgroundColor = HyprTheme.colors.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
            }
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
