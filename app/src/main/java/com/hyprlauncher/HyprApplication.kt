package com.hyprlauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * HyprLauncher Application entry point.
 * Initializes Hilt dependency injection, caching frameworks, and logging.
 */
@HiltAndroidApp
class HyprApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
