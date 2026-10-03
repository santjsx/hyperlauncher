package com.hyprlauncher.core.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Calculates gesture direction from total X and Y offsets with a given threshold.
 */
fun resolveSwipeGesture(
    totalX: Float,
    totalY: Float,
    thresholdPx: Float
): GestureType? {
    val absX = abs(totalX)
    val absY = abs(totalY)

    if (absX < thresholdPx && absY < thresholdPx) {
        return null
    }

    return if (absY > absX) {
        if (totalY < -thresholdPx) GestureType.SWIPE_UP else GestureType.SWIPE_DOWN
    } else {
        if (totalX < -thresholdPx) GestureType.SWIPE_LEFT else GestureType.SWIPE_RIGHT
    }
}

/**
 * Modifier detecting swipe gestures, double-taps, and long-presses conforming to PRD Section 18 & 19.
 */
@Composable
fun Modifier.hyprGestureHandler(
    onGesture: (GestureType) -> Unit,
    swipeThreshold: Dp = 48.dp
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = with(density) { swipeThreshold.toPx() }

    var accumulatedX by remember { mutableFloatStateOf(0f) }
    var accumulatedY by remember { mutableFloatStateOf(0f) }

    return this
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { onGesture(GestureType.DOUBLE_TAP) },
                onLongPress = { onGesture(GestureType.LONG_PRESS) }
            )
        }
        .pointerInput(thresholdPx) {
            detectDragGestures(
                onDragStart = {
                    accumulatedX = 0f
                    accumulatedY = 0f
                },
                onDragEnd = {
                    val detected = resolveSwipeGesture(accumulatedX, accumulatedY, thresholdPx)
                    if (detected != null) {
                        onGesture(detected)
                    }
                    accumulatedX = 0f
                    accumulatedY = 0f
                },
                onDragCancel = {
                    accumulatedX = 0f
                    accumulatedY = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    accumulatedX += dragAmount.x
                    accumulatedY += dragAmount.y
                }
            )
        }
}
