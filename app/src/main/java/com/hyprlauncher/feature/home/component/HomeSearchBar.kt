package com.hyprlauncher.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyprlauncher.core.designsystem.theme.HyprTheme

/**
 * Terminal-centric Rofi/Wofi search input bar conforming to PRD Section 14, 20 & 24.
 */
@Composable
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String = "Search apps or run command (rofi)...",
    onSearchSubmit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = HyprTheme.shapes.small,
        color = HyprTheme.colors.surfaceElevated,
        border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
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

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = HyprTheme.typography.bodyMedium,
                        color = HyprTheme.colors.textSecondary
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = HyprTheme.colors.textPrimary
                    ),
                    cursorBrush = SolidColor(HyprTheme.colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() })
                )
            }

            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = HyprTheme.shapes.small,
                    color = HyprTheme.colors.surface,
                    modifier = Modifier.clickable { onQueryChange("") }
                ) {
                    Text(
                        text = "✕",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
