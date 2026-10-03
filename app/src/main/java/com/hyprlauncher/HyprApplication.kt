package com.hyprlauncher

import android.app.Application
import com.hyprlauncher.core.diagnostics.StartupTracker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * HyprLauncher Application entry point.
 * Initializes Hilt dependency injection, caching frameworks, startup tracking, and logging.
 */
@HiltAndroidApp
class HyprApplication : Application() {

    @Inject
    lateinit var startupTracker: StartupTracker

    override fun onCreate() {
        super.onCreate()
        startupTracker.onAppInitStart()
        startupTracker.onAppInitEnd()
    }
}
