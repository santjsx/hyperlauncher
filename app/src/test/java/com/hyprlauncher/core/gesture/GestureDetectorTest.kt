package com.hyprlauncher.core.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureDetectorTest {

    private val threshold = 48f

    @Test
    fun resolveSwipeGestureReturnsNullWhenBelowThreshold() {
        assertNull(resolveSwipeGesture(totalX = 20f, totalY = 30f, thresholdPx = threshold))
        assertNull(resolveSwipeGesture(totalX = -40f, totalY = -10f, thresholdPx = threshold))
        assertNull(resolveSwipeGesture(totalX = 0f, totalY = 0f, thresholdPx = threshold))
    }

    @Test
    fun resolveSwipeGestureDetectsSwipeUp() {
        val result = resolveSwipeGesture(totalX = 10f, totalY = -120f, thresholdPx = threshold)
        assertEquals(GestureType.SWIPE_UP, result)
    }

    @Test
    fun resolveSwipeGestureDetectsSwipeDown() {
        val result = resolveSwipeGesture(totalX = -15f, totalY = 90f, thresholdPx = threshold)
        assertEquals(GestureType.SWIPE_DOWN, result)
    }

    @Test
    fun resolveSwipeGestureDetectsSwipeLeft() {
        val result = resolveSwipeGesture(totalX = -100f, totalY = 20f, thresholdPx = threshold)
        assertEquals(GestureType.SWIPE_LEFT, result)
    }

    @Test
    fun resolveSwipeGestureDetectsSwipeRight() {
        val result = resolveSwipeGesture(totalX = 150f, totalY = -30f, thresholdPx = threshold)
        assertEquals(GestureType.SWIPE_RIGHT, result)
    }

    @Test
    fun resolveSwipeGestureDominantAxisWinsWhenBothAboveThreshold() {
        // Y dominant
        assertEquals(
            GestureType.SWIPE_UP,
            resolveSwipeGesture(totalX = 60f, totalY = -120f, thresholdPx = threshold)
        )
        assertEquals(
            GestureType.SWIPE_DOWN,
            resolveSwipeGesture(totalX = -60f, totalY = 120f, thresholdPx = threshold)
        )

        // X dominant
        assertEquals(
            GestureType.SWIPE_LEFT,
            resolveSwipeGesture(totalX = -120f, totalY = 60f, thresholdPx = threshold)
        )
        assertEquals(
            GestureType.SWIPE_RIGHT,
            resolveSwipeGesture(totalX = 120f, totalY = -60f, thresholdPx = threshold)
        )
    }
}
