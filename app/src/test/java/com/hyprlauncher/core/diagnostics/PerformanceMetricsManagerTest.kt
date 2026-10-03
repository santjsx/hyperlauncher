package com.hyprlauncher.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PerformanceMetricsManagerTest {

    private lateinit var metricsManager: PerformanceMetricsManager

    @Before
    fun setup() {
        metricsManager = DefaultPerformanceMetricsManager()
    }

    @Test
    fun initialFrameMetricsDefaultToNominalValues() {
        val metrics = metricsManager.getFrameMetrics()
        assertEquals(60.0, metrics.fps, 0.1)
        assertEquals(16.6, metrics.averageFrameTimeMs, 0.1)
        assertEquals(0, metrics.jankFrames)
        assertEquals(0.0, metrics.jankPercentage, 0.1)
        assertEquals(0, metrics.totalRecordedFrames)
    }

    @Test
    fun recordingSmoothFramesCalculatesAccurateFpsAndZeroJank() {
        // Record 60 frames of 16.67ms (16,666,666 nanos)
        repeat(60) {
            metricsManager.recordFrame(16_666_666L)
        }

        val metrics = metricsManager.getFrameMetrics()
        assertEquals(60.0, metrics.fps, 1.0)
        assertEquals(16.67, metrics.averageFrameTimeMs, 0.5)
        assertEquals(0, metrics.jankFrames)
        assertEquals(0.0, metrics.jankPercentage, 0.1)
        assertEquals(60, metrics.totalRecordedFrames)
    }

    @Test
    fun recordingJankFramesAccuratelyComputesJankCountAndPercentage() {
        // Record 10 smooth frames and 10 slow frames (33.3ms)
        repeat(10) {
            metricsManager.recordFrame(16_000_000L)
        }
        repeat(10) {
            metricsManager.recordFrame(33_333_333L) // Jank > 16.67ms
        }

        val metrics = metricsManager.getFrameMetrics()
        assertEquals(20, metrics.totalRecordedFrames)
        assertEquals(10, metrics.jankFrames)
        assertEquals(50.0, metrics.jankPercentage, 0.1)
        assertTrue(metrics.fps < 60.0)
    }

    @Test
    fun searchLatencyRecordingMaintainsLastAndAverage() {
        assertEquals(0L, metricsManager.getLastSearchLatencyMs())
        assertEquals(0.0, metricsManager.getAverageSearchLatencyMs(), 0.01)

        metricsManager.recordSearchLatency(10L)
        metricsManager.recordSearchLatency(20L)
        metricsManager.recordSearchLatency(30L)

        assertEquals(30L, metricsManager.getLastSearchLatencyMs())
        assertEquals(20.0, metricsManager.getAverageSearchLatencyMs(), 0.1)
    }

    @Test
    fun memoryMetricsReportsNonNegativeMemoryValues() {
        val memory = metricsManager.getMemoryMetrics()
        assertTrue(memory.heapAllocatedMb >= 0)
        assertTrue(memory.heapFreeMb >= 0)
        assertTrue(memory.heapMaxMb > 0)
        assertTrue(memory.nativeHeapAllocatedMb >= 0)
    }

    @Test
    fun resetMetricsClearsRecordedFramesAndLatency() {
        metricsManager.recordFrame(20_000_000L)
        metricsManager.recordSearchLatency(45L)

        metricsManager.resetMetrics()

        val frameMetrics = metricsManager.getFrameMetrics()
        assertEquals(0, frameMetrics.totalRecordedFrames)
        assertEquals(0L, metricsManager.getLastSearchLatencyMs())
        assertEquals(0.0, metricsManager.getAverageSearchLatencyMs(), 0.01)
    }
}
