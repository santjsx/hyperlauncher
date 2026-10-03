package com.hyprlauncher.core.diagnostics

import android.os.Build
import android.os.Process
import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks cold-start, warmup, and UI-ready lifecycle metrics conforming to PRD Section 40 & Phase 11.
 */
@Singleton
class StartupTracker @Inject constructor() {

    private val processStartTimeMs: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Process.getStartElapsedRealtime()
    } else {
        SystemClock.elapsedRealtime()
    }

    private var appInitStartMs: Long = SystemClock.elapsedRealtime()
    private var appInitDurationMs: Long = 0L
    private var launcherReadyTimestampMs: Long = 0L

    fun onAppInitStart() {
        appInitStartMs = SystemClock.elapsedRealtime()
    }

    fun onAppInitEnd() {
        appInitDurationMs = SystemClock.elapsedRealtime() - appInitStartMs
    }

    fun onLauncherReady() {
        if (launcherReadyTimestampMs == 0L) {
            launcherReadyTimestampMs = SystemClock.elapsedRealtime()
        }
    }

    /**
     * Total cold-start time from process creation to first launcher UI frame ready.
     */
    fun getColdStartTimeMs(): Long {
        return if (launcherReadyTimestampMs > 0 && processStartTimeMs > 0) {
            (launcherReadyTimestampMs - processStartTimeMs).coerceAtLeast(0L)
        } else {
            (SystemClock.elapsedRealtime() - processStartTimeMs).coerceAtLeast(0L)
        }
    }

    fun getAppInitDurationMs(): Long = appInitDurationMs
}
