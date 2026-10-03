package com.hyprlauncher.feature.widget

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.core.widget.WidgetHostManager
import com.hyprlauncher.domain.model.LauncherWidget

/**
 * HyprLauncher widget container conforming to PRD Section 25 & Phase 10.
 * Wraps Android's AppWidgetHostView with crash isolation, custom fallbacks,
 * and Hyprland-inspired editing handles for resizing and deletion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetHostContainer(
    widget: LauncherWidget,
    widgetHostManager: WidgetHostManager,
    onResize: (spanX: Int, spanY: Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var renderError by remember { mutableStateOf(false) }

    val appWidgetInfo = remember(widget.appWidgetId) {
        runCatching { widgetHostManager.getAppWidgetInfo(widget.appWidgetId) }.getOrNull()
    }
    val hasError = renderError || (appWidgetInfo == null)
    val heightDp = if (hasError) 54.dp else (widget.spanY * 60 + 16).coerceIn(72, 320).dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .combinedClickable(
                onClick = {},
                onLongClick = { if (!hasError) isEditMode = !isEditMode }
            ),
        shape = HyprTheme.shapes.small,
        color = if (hasError) HyprTheme.colors.surfaceElevated else HyprTheme.colors.surface,
        border = BorderStroke(
            HyprTheme.shapes.borderWidth,
            if (isEditMode) HyprTheme.colors.accent else if (hasError) HyprTheme.colors.error.copy(alpha = 0.5f) else HyprTheme.colors.border
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasError) {
                // PRD Section 25 Fallback: Sleek compact strip for uninstalled or unbindable widgets
                WidgetErrorPlaceholder(
                    widget = widget,
                    onRemove = onRemove
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        try {
                            widgetHostManager.createView(ctx, widget.appWidgetId, appWidgetInfo)
                                ?: createFallbackErrorView(ctx).also { renderError = true }
                        } catch (t: Throwable) {
                            renderError = true
                            createFallbackErrorView(ctx)
                        }
                    },
                    update = { view ->
                        // Optional view updates
                    }
                )
            }

            // Edit Overlay: Resize & Removal controls
            if (isEditMode) {
                WidgetEditOverlay(
                    widget = widget,
                    onResize = onResize,
                    onRemove = onRemove,
                    onDismiss = { isEditMode = false }
                )
            }
        }
    }
}

@Composable
private fun WidgetErrorPlaceholder(
    widget: LauncherWidget,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "[widget unavailable: ${widget.providerPackage.substringAfterLast('.')}]",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.error,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "external provider uninstalled or unbindable",
                style = HyprTheme.typography.monospaceSmall.copy(fontSize = 10.sp),
                color = HyprTheme.colors.textSecondary,
                maxLines = 1
            )
        }
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = HyprTheme.colors.error.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, HyprTheme.colors.error),
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable(onClick = onRemove)
        ) {
            Text(
                text = "remove",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.error,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun WidgetEditOverlay(
    widget: LauncherWidget,
    onResize: (spanX: Int, spanY: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(4.dp),
        color = HyprTheme.colors.background.copy(alpha = 0.92f),
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Span Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "cols:",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary
                )
                EditBadge(
                    text = "-",
                    enabled = widget.spanX > 1,
                    onClick = { onResize((widget.spanX - 1).coerceAtLeast(1), widget.spanY) }
                )
                Text(
                    text = "${widget.spanX}",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.accent,
                    fontWeight = FontWeight.Bold
                )
                EditBadge(
                    text = "+",
                    enabled = widget.spanX < 8,
                    onClick = { onResize((widget.spanX + 1).coerceAtMost(8), widget.spanY) }
                )

                Text(
                    text = "rows:",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                EditBadge(
                    text = "-",
                    enabled = widget.spanY > 1,
                    onClick = { onResize(widget.spanX, (widget.spanY - 1).coerceAtLeast(1)) }
                )
                Text(
                    text = "${widget.spanY}",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.accent,
                    fontWeight = FontWeight.Bold
                )
                EditBadge(
                    text = "+",
                    enabled = widget.spanY < 12,
                    onClick = { onResize(widget.spanX, (widget.spanY + 1).coerceAtMost(12)) }
                )
            }

            // Actions: Done & Remove
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.error),
                    modifier = Modifier.clickable { onRemove() }
                ) {
                    Text(
                        text = "del",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated,
                    border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.accent),
                    modifier = Modifier.clickable { onDismiss() }
                ) {
                    Text(
                        text = "ok",
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

@Composable
private fun EditBadge(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (enabled) HyprTheme.colors.surfaceElevated else Color.Transparent,
        border = BorderStroke(
            HyprTheme.shapes.borderWidth,
            if (enabled) HyprTheme.colors.border else HyprTheme.colors.border.copy(alpha = 0.3f)
        ),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = text,
            style = HyprTheme.typography.monospaceSmall,
            color = if (enabled) HyprTheme.colors.accent else HyprTheme.colors.textSecondary.copy(alpha = 0.3f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

private fun createFallbackErrorView(context: Context): android.view.View {
    return android.widget.TextView(context).apply {
        text = "[widget fallback]"
        setTextColor(android.graphics.Color.GRAY)
        textSize = 10f
        gravity = android.view.Gravity.CENTER
    }
}
