package com.hyprlauncher

import android.app.Application
import android.content.ComponentCallbacks2
import com.hyprlauncher.core.diagnostics.StartupTracker
import com.hyprlauncher.core.icon.IconCache
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * HyprLauncher Application entry point.
 * Initializes Hilt dependency injection, caching frameworks, startup tracking, and logging.
 * Integrates low-memory callbacks (PRD §51) to trim in-memory icon caches during system pressure.
 */
@HiltAndroidApp
class HyprApplication : Application() {

    @Inject
    lateinit var startupTracker: StartupTracker

    @Inject
    lateinit var iconCache: IconCache

    override fun onCreate() {
        super.onCreate()
        startupTracker.onAppInitStart()
        startupTracker.onAppInitEnd()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (::iconCache.isInitialized) {
            iconCache.trimMemory(level)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::iconCache.isInitialized) {
            iconCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        }
    }
}
