package com.hyprlauncher.domain.usecase

import com.hyprlauncher.core.di.IoDispatcher
import com.hyprlauncher.core.platform.PackageDiscoveryManager
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DiscoverAndIndexAppsUseCase @Inject constructor(
    private val discoveryManager: PackageDiscoveryManager,
    private val appDao: AppDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            val discoveredApps = discoveryManager.discoverLaunchableApps()

            val entities = discoveredApps.map { app ->
                val existing = appDao.getApp(app.packageName)
                AppEntity(
                    packageName = app.packageName,
                    activityName = app.activityName,
                    label = app.label,
                    category = app.category,
                    installTime = app.installTime,
                    updateTime = app.updateTime,
                    launchCount = existing?.launchCount ?: 0,
                    lastUsedTimestamp = existing?.lastUsedTimestamp ?: 0L,
                    isFavorite = existing?.isFavorite ?: false,
                    isHidden = existing?.isHidden ?: false,
                    workspaceId = existing?.workspaceId ?: 1
                )
            }

            appDao.upsertApps(entities)
            entities.size
        }
    }
}
