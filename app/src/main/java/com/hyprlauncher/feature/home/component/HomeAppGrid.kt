package com.hyprlauncher.feature.home.component

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.data.database.entity.AppEntity

/**
 * Responsive Application Grid conforming to PRD Section 10 & Section 24.
 */
@Composable
fun HomeAppGrid(
    apps: List<AppEntity>,
    columns: Int,
    showLabels: Boolean,
    loadIcon: suspend (String) -> Bitmap?,
    onAppClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No applications available in this view",
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textSecondary
            )
        }
        return
    }

    val safeColumns = columns.coerceIn(2, 6)

    LazyVerticalGrid(
        columns = GridCells.Fixed(safeColumns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppGridTile(
                app = app,
                showLabel = showLabels,
                loadIcon = loadIcon,
                onClick = { onAppClick(app.packageName, app.activityName) }
            )
        }
    }
}

@Composable
private fun AppGridTile(
    app: AppEntity,
    showLabel: Boolean,
    loadIcon: suspend (String) -> Bitmap?,
    onClick: () -> Unit
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(app.packageName) {
        iconBitmap = loadIcon(app.packageName)
    }

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

            if (showLabel) {
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
}
