package com.hyprlauncher.core.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.hyprlauncher.core.di.IoDispatcher
import com.hyprlauncher.domain.model.DiscoveredApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Discovers launchable applications installed on the Android device.
 * Conforms to PRD Section 13 (Application Indexing) and Section 3.1 (Performance First).
 */
interface PackageDiscoveryManager {
    suspend fun discoverLaunchableApps(): List<DiscoveredApp>
    fun getAppIcon(packageName: String): android.graphics.drawable.Drawable?
}

@Singleton
class DefaultPackageDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PackageDiscoveryManager {

    private val packageManager: PackageManager = context.packageManager

    override suspend fun discoverLaunchableApps(): List<DiscoveredApp> = withContext(ioDispatcher) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val myPackageName = context.packageName

        resolveInfoList
            .asSequence()
            .filter { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName
                pkg != null && pkg != myPackageName
            }
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val pkgName = activityInfo.packageName
                val actName = activityInfo.name
                val appLabel = runCatching {
                    resolveInfo.loadLabel(packageManager).toString()
                }.getOrDefault(pkgName)

                var installTime = 0L
                var updateTime = 0L
                var category = "OTHER"

                runCatching {
                    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(
                            pkgName,
                            PackageManager.PackageInfoFlags.of(0L)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(pkgName, 0)
                    }
                    installTime = packageInfo.firstInstallTime
                    updateTime = packageInfo.lastUpdateTime

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        category = when (packageInfo.applicationInfo?.category) {
                            ApplicationInfo.CATEGORY_GAME -> "GAMES"
                            ApplicationInfo.CATEGORY_AUDIO -> "AUDIO"
                            ApplicationInfo.CATEGORY_VIDEO -> "VIDEO"
                            ApplicationInfo.CATEGORY_IMAGE -> "IMAGE"
                            ApplicationInfo.CATEGORY_SOCIAL -> "SOCIAL"
                            ApplicationInfo.CATEGORY_NEWS -> "NEWS"
                            ApplicationInfo.CATEGORY_MAPS -> "MAPS"
                            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "PRODUCTIVITY"
                            else -> "OTHER"
                        }
                    }
                }

                DiscoveredApp(
                    packageName = pkgName,
                    activityName = actName,
                    label = appLabel,
                    category = category,
                    installTime = installTime,
                    updateTime = updateTime
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    override fun getAppIcon(packageName: String): android.graphics.drawable.Drawable? {
        return runCatching {
            packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }
}
