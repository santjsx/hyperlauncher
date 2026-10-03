package com.hyprlauncher.feature.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.domain.model.LauncherWidget
import com.hyprlauncher.domain.model.WidgetProviderItem

/**
 * Hyprland-themed widget picker dialog for browsing and placing widgets (PRD Section 25 & Phase 10).
 */
@Composable
fun WidgetPickerDialog(
    providers: List<WidgetProviderItem>,
    activeWidgets: List<LauncherWidget> = emptyList(),
    onRemoveWidget: (String) -> Unit = {},
    onSelectProvider: (WidgetProviderItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredProviders = remember(searchQuery, providers) {
        if (searchQuery.isBlank()) {
            providers
        } else {
            val q = searchQuery.trim().lowercase()
            providers.filter {
                it.appLabel.lowercase().contains(q) || it.widgetLabel.lowercase().contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.background,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "hyprctl ~ widgets",
                            style = HyprTheme.typography.monospaceLarge,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "manage active or select widget to place",
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
                            text = "✕",
                            style = HyprTheme.typography.monospaceMedium,
                            color = HyprTheme.colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Widgets Section (Quick Removal & Management)
                if (activeWidgets.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "# active on workspace (${activeWidgets.size})",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeWidgets.forEach { widget ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = HyprTheme.colors.surfaceElevated,
                                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = widget.providerPackage.substringAfterLast('.'),
                                                style = HyprTheme.typography.monospaceMedium,
                                                color = HyprTheme.colors.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "span: ${widget.spanX}x${widget.spanY} · id: ${widget.appWidgetId}",
                                                style = HyprTheme.typography.monospaceSmall,
                                                color = HyprTheme.colors.textSecondary,
                                                maxLines = 1
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = HyprTheme.colors.error.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, HyprTheme.colors.error),
                                            modifier = Modifier.clickable { onRemoveWidget(widget.id) }
                                        ) {
                                            Text(
                                                text = "remove",
                                                style = HyprTheme.typography.monospaceSmall,
                                                color = HyprTheme.colors.error,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "> search:",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.accentSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = HyprTheme.colors.textPrimary,
                                fontFamily = HyprTheme.typography.monospaceMedium.fontFamily,
                                fontSize = HyprTheme.typography.monospaceMedium.fontSize
                            ),
                            cursorBrush = SolidColor(HyprTheme.colors.accent),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Widget List
                if (filteredProviders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (providers.isEmpty()) "No widgets available on system" else "No matching widgets found",
                            style = HyprTheme.typography.monospaceMedium,
                            color = HyprTheme.colors.textSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProviders, key = { "${it.providerPackage}/${it.providerClass}" }) { provider ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HyprTheme.colors.surface,
                                border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectProvider(provider)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = provider.widgetLabel,
                                            style = HyprTheme.typography.bodyLarge,
                                            color = HyprTheme.colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${provider.appLabel} · ${provider.providerPackage}",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.textSecondary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = HyprTheme.colors.surfaceElevated,
                                        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
                                    ) {
                                        Text(
                                            text = "${provider.minSpanX}×${provider.minSpanY}",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.accent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
}
