package com.hyprlauncher.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.core.designsystem.theme.ThemePresets
import com.hyprlauncher.domain.model.AnimationScale
import com.hyprlauncher.domain.model.ThemeConfig

/**
 * Hyprland-inspired Theme Picker Dialog conforming to PRD Section 21.
 * Allows instant live preview and selection across all 9 built-in themes.
 */
@Composable
fun ThemePickerDialog(
    themeConfig: ThemeConfig,
    onSelectPreset: (ThemePresetId) -> Unit,
    onSelectAnimationScale: (AnimationScale) -> Unit,
    onSelectCornerRadius: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.surface,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "hyprctl: themes",
                        style = HyprTheme.typography.monospaceMedium,
                        color = HyprTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                    HyprStatusBadge(
                        text = themeConfig.presetId.displayName.lowercase(),
                        accentColor = HyprTheme.colors.accentSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Presets List
                Text(
                    text = "palettes (prd §21):",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ThemePresetId.entries.filter { it != ThemePresetId.CUSTOM }) { preset ->
                        val isSelected = themeConfig.presetId == preset
                        val presetColors = ThemePresets.forPreset(preset)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPreset(preset) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) HyprTheme.colors.surfaceElevated else HyprTheme.colors.background,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 3 Swatches: background, surface, accent
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(presetColors.background)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(presetColors.surfaceElevated)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(presetColors.accent)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = preset.displayName,
                                        style = HyprTheme.typography.monospaceSmall,
                                        color = if (isSelected) HyprTheme.colors.accent else HyprTheme.colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                if (isSelected) {
                                    Text(
                                        text = "[active]",
                                        style = HyprTheme.typography.monospaceSmall,
                                        color = HyprTheme.colors.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Corner Radius Selector
                Text(
                    text = "geometry (corner radius):",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val radiusOptions = listOf(2 to "sharp", 6 to "normal", 12 to "round", 20 to "soft")
                    radiusOptions.forEach { (radius, label) ->
                        val isCur = themeConfig.cornerRadiusDp == radius
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectCornerRadius(radius) },
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCur) HyprTheme.colors.accent else HyprTheme.colors.background,
                            border = BorderStroke(1.dp, if (isCur) HyprTheme.colors.accent else HyprTheme.colors.border)
                        ) {
                            Text(
                                text = label,
                                style = HyprTheme.typography.monospaceSmall,
                                color = if (isCur) HyprTheme.colors.background else HyprTheme.colors.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Animation Scale Selector (PRD §29)
                Text(
                    text = "animations (prd §29):",
                    style = HyprTheme.typography.monospaceSmall,
                    color = HyprTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimationScale.entries.forEach { scale ->
                        val isCur = themeConfig.animationScale == scale
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectAnimationScale(scale) },
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCur) HyprTheme.colors.accentSecondary else HyprTheme.colors.background,
                            border = BorderStroke(1.dp, if (isCur) HyprTheme.colors.accentSecondary else HyprTheme.colors.border)
                        ) {
                            Text(
                                text = scale.displayName.lowercase(),
                                style = HyprTheme.typography.monospaceSmall,
                                color = if (isCur) Color.White else HyprTheme.colors.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dismiss Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated
                ) {
                    Text(
                        text = "apply & close",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.accent,
                        modifier = Modifier.padding(vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
