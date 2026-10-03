package com.hyprlauncher.feature.diagnostics

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.component.HyprSurfaceCard
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.domain.model.DiagnosticReport

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose {
            viewModel.stopPolling()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = HyprTheme.colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "hyprctl ~ perfmon",
                            style = HyprTheme.typography.monospaceLarge,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(
                                    color = if (uiState.autoRefresh) HyprTheme.colors.terminalGreen else HyprTheme.colors.warning,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    Text(
                        text = "system diagnostics & telemetry dashboard",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surfaceElevated,
                        border = BorderStroke(
                            HyprTheme.shapes.borderWidth,
                            if (uiState.autoRefresh) HyprTheme.colors.terminalGreen else HyprTheme.colors.border
                        ),
                        modifier = Modifier.clickable { viewModel.toggleAutoRefresh() }
                    ) {
                        Text(
                            text = if (uiState.autoRefresh) "live: on" else "live: off",
                            style = HyprTheme.typography.monospaceSmall,
                            color = if (uiState.autoRefresh) HyprTheme.colors.terminalGreen else HyprTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surfaceElevated,
                        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                        modifier = Modifier.clickable { onNavigateBack() }
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

            // Status message toast
            if (uiState.statusMessage != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissStatusMessage() }
                ) {
                    Text(
                        text = "> ${uiState.statusMessage}",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Scrollable Metrics Dashboard
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val report = uiState.report
                if (report != null) {
                    // Section 1: Frame Timing & Rendering
                    TelemetryCard(title = "rendering & frame metrics") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                label = "FPS",
                                value = "${report.fps}",
                                color = if (report.fps >= 55.0) HyprTheme.colors.terminalGreen else if (report.fps >= 40.0) HyprTheme.colors.warning else HyprTheme.colors.error
                            )
                            MetricItem(
                                label = "FRAME TIME",
                                value = "${report.averageFrameTimeMs}ms",
                                color = HyprTheme.colors.textPrimary
                            )
                            MetricItem(
                                label = "JANK",
                                value = "${report.jankFrames} (${report.jankPercentage}%)",
                                color = if (report.jankFrames == 0) HyprTheme.colors.terminalGreen else HyprTheme.colors.warning
                            )
                            MetricItem(
                                label = "COLD START",
                                value = "${report.startupTimeMs}ms",
                                color = HyprTheme.colors.accentSecondary
                            )
                        }
                    }

                    // Section 2: Memory & Garbage Collector
                    TelemetryCard(title = "runtime memory telemetry") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "JVM Heap Allocated:",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.textSecondary
                                )
                                Text(
                                    text = "${report.heapAllocatedMb} MB / ${report.heapMaxMb} MB",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val progress = if (report.heapMaxMb > 0) {
                                (report.heapAllocatedMb.toFloat() / report.heapMaxMb.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = HyprTheme.colors.accent,
                                trackColor = HyprTheme.colors.surfaceElevated
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Native Heap Allocated:",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.textSecondary
                                )
                                Text(
                                    text = "${report.nativeHeapAllocatedMb} MB",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.accentSecondary
                                )
                            }
                        }
                    }

                    // Section 3: Search Latency & Subsystems
                    TelemetryCard(title = "search & subsystems") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                label = "LAST SEARCH",
                                value = "${report.searchLatencyMs}ms",
                                color = HyprTheme.colors.terminalGreen
                            )
                            MetricItem(
                                label = "AVG SEARCH",
                                value = "${report.avgSearchLatencyMs}ms",
                                color = HyprTheme.colors.textPrimary
                            )
                            MetricItem(
                                label = "ICON CACHE",
                                value = "${report.iconCacheSizeBytes / 1024} KB",
                                color = HyprTheme.colors.accentSecondary
                            )
                            MetricItem(
                                label = "DB SIZE",
                                value = "${report.databaseSizeBytes / 1024} KB",
                                color = HyprTheme.colors.accent
                            )
                        }
                    }

                    // Section 4: Launcher Inventory
                    TelemetryCard(title = "launcher state inventory") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                label = "INDEXED APPS",
                                value = "${report.appCount}",
                                color = HyprTheme.colors.textPrimary
                            )
                            MetricItem(
                                label = "WIDGETS",
                                value = "${report.widgetCount}",
                                color = HyprTheme.colors.accent
                            )
                            MetricItem(
                                label = "WORKSPACES",
                                value = "${report.workspaceCount}",
                                color = HyprTheme.colors.accentSecondary
                            )
                            MetricItem(
                                label = "RICE PROFILES",
                                value = "${report.riceProfileCount}",
                                color = HyprTheme.colors.terminalGreen
                            )
                        }
                    }

                    // Section 5: Hardware & Platform
                    TelemetryCard(title = "host environment") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            InfoRow(label = "Host Device", value = report.deviceModel)
                            InfoRow(label = "Platform", value = report.androidVersion)
                            InfoRow(label = "Governor Mode", value = report.performanceMode)
                        }
                    }
                }

                // Section 6: Maintenance Commands
                TelemetryCard(title = "developer maintenance controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "clear cache",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.clearCache() }
                            )
                            ActionButton(
                                text = "rebuild index",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.rebuildAppIndex() }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionButton(
                                text = "reset stats",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.resetUiState() }
                            )
                            ActionButton(
                                text = "export ascii",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.exportDiagnostics(asJson = false) }
                            )
                            ActionButton(
                                text = "export json",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.exportDiagnostics(asJson = true) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Export Modal Dialog
    if (uiState.exportedContent != null) {
        Dialog(onDismissRequest = { viewModel.dismissExport() }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.80f),
                shape = HyprTheme.shapes.medium,
                color = HyprTheme.colors.background,
                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "hyprctl ~ export",
                            style = HyprTheme.typography.monospaceMedium,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HyprTheme.colors.surfaceElevated,
                                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.terminalGreen),
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(uiState.exportedContent ?: ""))
                                }
                            ) {
                                Text(
                                    text = "copy",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.terminalGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HyprTheme.colors.surfaceElevated,
                                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                                modifier = Modifier.clickable { viewModel.dismissExport() }
                            ) {
                                Text(
                                    text = "✕",
                                    style = HyprTheme.typography.monospaceSmall,
                                    color = HyprTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        color = HyprTheme.colors.surface,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
                    ) {
                        val exportScroll = rememberScrollState()
                        Text(
                            text = uiState.exportedContent ?: "",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.textPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(exportScroll)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surface,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "> $title",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.accentSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = HyprTheme.typography.monospaceSmall,
            color = HyprTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = HyprTheme.typography.monospaceMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = HyprTheme.typography.monospaceSmall,
            color = HyprTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = HyprTheme.typography.monospaceSmall,
            color = HyprTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
