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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.sp
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint

/**
 * Responsive Application Grid conforming to PRD Section 10 & Section 24.
 */
@Composable
fun HomeAppGrid(
    apps: List<AppEntity>,
    columns: Int,
    showLabels: Boolean,
    iconSizeDp: Int = 36,
    horizontalSpacingDp: Int = 8,
    verticalSpacingDp: Int = 8,
    iconShape: IconShape = IconShape.SYSTEM_DEFAULT,
    iconTint: IconTint = IconTint.NONE,
    labelFontSizeSp: Int = 11,
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
        verticalArrangement = Arrangement.spacedBy(verticalSpacingDp.coerceIn(2, 24).dp),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacingDp.coerceIn(2, 24).dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppGridTile(
                app = app,
                showLabel = showLabels,
                iconSizeDp = iconSizeDp,
                iconShape = iconShape,
                iconTint = iconTint,
                labelFontSizeSp = labelFontSizeSp,
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
    iconSizeDp: Int,
    iconShape: IconShape,
    iconTint: IconTint,
    labelFontSizeSp: Int,
    loadIcon: suspend (String) -> Bitmap?,
    onClick: () -> Unit
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(app.packageName) {
        iconBitmap = loadIcon(app.packageName)
    }

    val shapeModifier = when (iconShape) {
        IconShape.ROUNDED_SQUARE -> RoundedCornerShape(8.dp)
        IconShape.CIRCLE -> CircleShape
        IconShape.SQUIRCLE -> RoundedCornerShape(30)
        IconShape.SYSTEM_DEFAULT -> RoundedCornerShape(8.dp)
    }

    val colorFilter = when (iconTint) {
        IconTint.THEME_ACCENT -> ColorFilter.tint(HyprTheme.colors.accent)
        IconTint.THEME_PRIMARY -> ColorFilter.tint(HyprTheme.colors.textPrimary)
        IconTint.NONE -> null
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
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val bitmap = iconBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .size(iconSizeDp.dp)
                        .clip(shapeModifier)
                )
            } else {
                Surface(
                    modifier = Modifier.size(iconSizeDp.dp),
                    shape = shapeModifier,
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
                    style = HyprTheme.typography.monospaceSmall.copy(fontSize = labelFontSizeSp.sp),
                    color = HyprTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
