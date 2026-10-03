package com.hyprlauncher.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.hyprlauncher.core.di.IoDispatcher
import com.hyprlauncher.core.icon.IconCache
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncPackageUpdateUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val iconCache: IconCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun handlePackageAddedOrUpdated(packageName: String): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            // Invalidate cached icon so updated icons are loaded fresh
            iconCache.invalidate(packageName)

            if (packageName == context.packageName) {
                return@runCatching false
            }

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent == null) {
                // Not launchable from launcher drawer, remove if it previously existed
                appDao.deleteApp(packageName)
                return@runCatching false
            }

            val activityComponent = launchIntent.component
            val activityName = activityComponent?.className ?: ""

            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }

            val label = runCatching {
                appInfo.loadLabel(packageManager).toString()
            }.getOrDefault(packageName)

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            var category = "OTHER"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                category = when (appInfo.category) {
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

            val existing = appDao.getApp(packageName)
            val entity = AppEntity(
                packageName = packageName,
                activityName = activityName,
                label = label,
                category = category,
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                launchCount = existing?.launchCount ?: 0,
                lastUsedTimestamp = existing?.lastUsedTimestamp ?: 0L,
                isFavorite = existing?.isFavorite ?: false,
                isHidden = existing?.isHidden ?: false,
                workspaceId = existing?.workspaceId ?: 1
            )

            appDao.upsertApp(entity)
            true
        }
    }

    suspend fun handlePackageRemoved(packageName: String): Result<Boolean> = withContext(ioDispatcher) {
        runCatching {
            iconCache.invalidate(packageName)
            appDao.deleteApp(packageName)
            true
        }
    }
}
