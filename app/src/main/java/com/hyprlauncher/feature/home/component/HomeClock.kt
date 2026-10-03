package com.hyprlauncher.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Isolated Hyprland Clock & Date component conforming to PRD Section 9 & Section 24.
 * Ticking state is scoped entirely within this composable to avoid unnecessary recomposition
 * of the parent screen or surrounding grid/dock layouts.
 */
@Composable
fun HomeClock(
    clock24Hour: Boolean,
    showSeconds: Boolean,
    showDate: Boolean,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }

    LaunchedEffect(clock24Hour, showSeconds) {
        val timePattern = when {
            clock24Hour && showSeconds -> "HH:mm:ss"
            clock24Hour -> "HH:mm"
            showSeconds -> "hh:mm:ss a"
            else -> "hh:mm a"
        }
        val timeFormat = SimpleDateFormat(timePattern, Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE · dd MMM", Locale.getDefault())

        while (true) {
            val now = Date()
            val formattedTime = timeFormat.format(now).uppercase()
            val formattedDate = dateFormat.format(now).uppercase()
            if (currentTimeString != formattedTime) {
                currentTimeString = formattedTime
            }
            if (currentDateString != formattedDate) {
                currentDateString = formattedDate
            }
            val nowMs = System.currentTimeMillis()
            val delayMs = if (showSeconds) {
                1000L - (nowMs % 1000L)
            } else {
                60000L - (nowMs % 60000L)
            }
            delay(delayMs.coerceAtLeast(100L))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = currentTimeString.ifEmpty { if (clock24Hour) "00:00" else "12:00 AM" },
            style = HyprTheme.typography.displayLarge,
            color = HyprTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        if (showDate) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentDateString.ifEmpty { "HYPR · LAUNCHER" },
                style = HyprTheme.typography.monospaceSmall,
                color = HyprTheme.colors.accent,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
