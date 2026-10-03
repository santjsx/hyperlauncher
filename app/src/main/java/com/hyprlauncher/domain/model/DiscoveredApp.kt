package com.hyprlauncher.domain.model

import android.graphics.drawable.Drawable

/**
 * Normalized model for an application discovered via Android PackageManager.
 */
data class DiscoveredApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: String = "OTHER",
    val installTime: Long = 0L,
    val updateTime: Long = 0L,
    val iconDrawable: Drawable? = null
)
