package com.hyprlauncher.core.gesture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.hyprlauncher.core.platform.AppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface GestureActionExecutor {
    suspend fun execute(
        action: LauncherAction,
        onOpenDrawer: () -> Unit = {},
        onNextWorkspace: () -> Unit = {},
        onPreviousWorkspace: () -> Unit = {}
    )
}

@Singleton
class DefaultGestureActionExecutor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appLauncher: AppLauncher
) : GestureActionExecutor {

    override suspend fun execute(
        action: LauncherAction,
        onOpenDrawer: () -> Unit,
        onNextWorkspace: () -> Unit,
        onPreviousWorkspace: () -> Unit
    ) {
        when (action) {
            is LauncherAction.OpenAppDrawer, is LauncherAction.OpenSearch, is LauncherAction.OpenCommandPalette -> {
                onOpenDrawer()
            }
            is LauncherAction.NextWorkspace -> {
                onNextWorkspace()
            }
            is LauncherAction.PreviousWorkspace -> {
                onPreviousWorkspace()
            }
            is LauncherAction.LaunchApplication -> {
                appLauncher.launchApp(action.packageName)
            }
            is LauncherAction.OpenNotificationShade -> {
                expandNotificationShade()
            }
            is LauncherAction.OpenSettings -> {
                openDeviceSettings()
            }
            is LauncherAction.LockDevice -> {
                // Platform restriction: Locking device requires DevicePolicyManager / Accessibility.
                // Fallback gracefully without crash (PRD Section 3.4).
            }
            is LauncherAction.None -> {
                // No-op
            }
        }
    }

    @SuppressLint("WrongConstant")
    private fun expandNotificationShade() {
        runCatching {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val method = statusBarManagerClass.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        }
    }

    private fun openDeviceSettings() {
        runCatching {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
