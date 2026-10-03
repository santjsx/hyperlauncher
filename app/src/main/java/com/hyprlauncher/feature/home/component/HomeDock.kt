package com.hyprlauncher.feature.home.component

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint

/**
 * Bottom Dock component conforming to PRD Section 11 & Section 24.
 * Hosts pinned or favorite applications in a sleek floating container.
 */
@Composable
fun HomeDock(
    dockApps: List<AppEntity>,
    showLabels: Boolean = false,
    iconSizeDp: Int = 40,
    spacingDp: Int = 10,
    cornerRadiusDp: Int = 16,
    backgroundAlpha: Float = 0.95f,
    iconTint: IconTint = IconTint.NONE,
    iconShape: IconShape = IconShape.SYSTEM_DEFAULT,
    loadIcon: suspend (String) -> Bitmap?,
    onAppClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dockApps.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadiusDp.dp),
        color = HyprTheme.colors.surfaceElevated.copy(alpha = backgroundAlpha.coerceIn(0f, 1f)),
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacingDp.dp, vertical = (spacingDp * 0.75f).toInt().coerceAtLeast(4).dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.take(7).forEach { app ->
                DockItem(
                    app = app,
                    showLabel = showLabels,
                    iconSizeDp = iconSizeDp,
                    iconTint = iconTint,
                    iconShape = iconShape,
                    loadIcon = loadIcon,
                    onClick = { onAppClick(app.packageName, app.activityName) }
                )
            }
        }
    }
}

@Composable
private fun DockItem(
    app: AppEntity,
    showLabel: Boolean,
    iconSizeDp: Int,
    iconTint: IconTint,
    iconShape: IconShape,
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
        IconShape.SQUIRCLE -> RoundedCornerShape(32)
        IconShape.SYSTEM_DEFAULT -> RoundedCornerShape(10.dp)
    }

    val colorFilter = when (iconTint) {
        IconTint.THEME_ACCENT -> ColorFilter.tint(HyprTheme.colors.accent)
        IconTint.THEME_PRIMARY -> ColorFilter.tint(HyprTheme.colors.textPrimary)
        IconTint.NONE -> null
    }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
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
                color = HyprTheme.colors.surface,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = app.label,
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
