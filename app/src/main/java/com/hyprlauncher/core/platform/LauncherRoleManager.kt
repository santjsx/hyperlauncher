package com.hyprlauncher.core.platform

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Android Default Home/Launcher role detection and resolution.
 * Conforms to PRD Section 3.2 (Native Android) and Phase 1 requirements.
 */
interface LauncherRoleManager {
    fun isDefaultLauncher(): Boolean
    fun createRequestDefaultLauncherIntent(): Intent?
}

@Singleton
class DefaultLauncherRoleManager @Inject constructor(
    @ApplicationContext private val context: Context
) : LauncherRoleManager {

    private val packageManager: PackageManager = context.packageManager

    override fun isDefaultLauncher(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            roleManager?.isRoleHeld(RoleManager.ROLE_HOME) ?: isDefaultViaPackageManager()
        } else {
            isDefaultViaPackageManager()
        }
    }

    private fun isDefaultViaPackageManager(): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    override fun createRequestDefaultLauncherIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }

        // Fallback for devices without standard RoleManager implementation
        return Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
