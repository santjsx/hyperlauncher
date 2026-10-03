package com.hyprlauncher.core.diagnostics

import android.os.Debug
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class FrameMetricsData(
    val fps: Double,
    val averageFrameTimeMs: Double,
    val jankFrames: Int,
    val jankPercentage: Double,
    val totalRecordedFrames: Int
)

data class MemoryMetricsData(
    val heapAllocatedMb: Long,
    val heapFreeMb: Long,
    val heapMaxMb: Long,
    val nativeHeapAllocatedMb: Long
)

/**
 * Manages runtime performance telemetry including frame metrics, memory tracking,
 * and search latency conforming to PRD Section 40.
 */
interface PerformanceMetricsManager {
    fun recordFrame(frameDurationNanos: Long)
    fun recordSearchLatency(durationMs: Long)
    fun getFrameMetrics(): FrameMetricsData
    fun getMemoryMetrics(): MemoryMetricsData
    fun getLastSearchLatencyMs(): Long
    fun getAverageSearchLatencyMs(): Double
    fun resetMetrics()
}

@Singleton
class DefaultPerformanceMetricsManager @Inject constructor() : PerformanceMetricsManager {

    companion object {
        private const val FRAME_WINDOW_SIZE = 120
        private const val JANK_THRESHOLD_NANOS = 16_666_667L // 16.67ms threshold (~60fps)
        private const val SEARCH_WINDOW_SIZE = 20
    }

    private val frameDurations = ArrayDeque<Long>(FRAME_WINDOW_SIZE)
    private val searchDurations = ArrayDeque<Long>(SEARCH_WINDOW_SIZE)
    private val lastSearchLatency = AtomicLong(0L)

    @Synchronized
    override fun recordFrame(frameDurationNanos: Long) {
        if (frameDurations.size >= FRAME_WINDOW_SIZE) {
            frameDurations.pollFirst()
        }
        frameDurations.addLast(frameDurationNanos)
    }

    @Synchronized
    override fun recordSearchLatency(durationMs: Long) {
        lastSearchLatency.set(durationMs)
        if (searchDurations.size >= SEARCH_WINDOW_SIZE) {
            searchDurations.pollFirst()
        }
        searchDurations.addLast(durationMs)
    }

    @Synchronized
    override fun getFrameMetrics(): FrameMetricsData {
        if (frameDurations.isEmpty()) {
            return FrameMetricsData(
                fps = 60.0,
                averageFrameTimeMs = 16.6,
                jankFrames = 0,
                jankPercentage = 0.0,
                totalRecordedFrames = 0
            )
        }

        val totalNanos = frameDurations.sum()
        val count = frameDurations.size
        val avgNanos = totalNanos.toDouble() / count
        val avgMs = avgNanos / 1_000_000.0

        val fps = if (avgNanos > 0) {
            (1_000_000_000.0 / avgNanos).coerceIn(0.0, 144.0)
        } else {
            60.0
        }

        val jankCount = frameDurations.count { it > JANK_THRESHOLD_NANOS }
        val jankPercent = (jankCount.toDouble() / count) * 100.0

        return FrameMetricsData(
            fps = (fps * 10.0).roundToInt() / 10.0,
            averageFrameTimeMs = (avgMs * 100.0).roundToInt() / 100.0,
            jankFrames = jankCount,
            jankPercentage = (jankPercent * 10.0).roundToInt() / 10.0,
            totalRecordedFrames = count
        )
    }

    override fun getMemoryMetrics(): MemoryMetricsData {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val allocatedMemory = totalMemory - freeMemory

        val mb = 1024 * 1024
        val nativeAllocated = runCatching { Debug.getNativeHeapAllocatedSize() }.getOrDefault(0L)

        return MemoryMetricsData(
            heapAllocatedMb = (allocatedMemory / mb).coerceAtLeast(0L),
            heapFreeMb = (freeMemory / mb).coerceAtLeast(0L),
            heapMaxMb = (maxMemory / mb).coerceAtLeast(0L),
            nativeHeapAllocatedMb = (nativeAllocated / mb).coerceAtLeast(0L)
        )
    }

    override fun getLastSearchLatencyMs(): Long = lastSearchLatency.get()

    @Synchronized
    override fun getAverageSearchLatencyMs(): Double {
        if (searchDurations.isEmpty()) return 0.0
        val avg = searchDurations.average()
        return (avg * 10.0).roundToInt() / 10.0
    }

    @Synchronized
    override fun resetMetrics() {
        frameDurations.clear()
        searchDurations.clear()
        lastSearchLatency.set(0L)
    }
}
