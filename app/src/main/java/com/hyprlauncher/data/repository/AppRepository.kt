package com.hyprlauncher.data.repository

import android.graphics.Bitmap
import com.hyprlauncher.core.icon.IconCache
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface AppRepository {
    val allApps: Flow<List<AppEntity>>
    val favoriteApps: Flow<List<AppEntity>>
    fun getFrequentApps(limit: Int = 8): Flow<List<AppEntity>>
    fun getAppsForWorkspace(workspaceId: Int): Flow<List<AppEntity>>
    fun searchApps(query: String): Flow<List<AppEntity>>
    suspend fun getAppIcon(packageName: String): Bitmap?
    suspend fun syncAllApps(): Result<Int>
    suspend fun setFavorite(packageName: String, isFavorite: Boolean)
    suspend fun setWorkspace(packageName: String, workspaceId: Int)
    suspend fun setHidden(packageName: String, isHidden: Boolean)
}

@Singleton
class DefaultAppRepository @Inject constructor(
    private val appDao: AppDao,
    private val iconCache: IconCache,
    private val discoverAndIndexAppsUseCase: DiscoverAndIndexAppsUseCase
) : AppRepository {

    override val allApps: Flow<List<AppEntity>> = appDao.getAllVisibleApps()

    override val favoriteApps: Flow<List<AppEntity>> = appDao.getFavoriteApps()

    override fun getFrequentApps(limit: Int): Flow<List<AppEntity>> = appDao.getFrequentApps(limit)

    override fun getAppsForWorkspace(workspaceId: Int): Flow<List<AppEntity>> = appDao.getAppsForWorkspace(workspaceId)

    override fun searchApps(query: String): Flow<List<AppEntity>> = appDao.searchApps(query)

    override suspend fun getAppIcon(packageName: String): Bitmap? {
        return iconCache.getIcon(packageName)
    }

    override suspend fun syncAllApps(): Result<Int> {
        val result = discoverAndIndexAppsUseCase()
        result.onSuccess {
            val packageNames = appDao.getAllVisiblePackageNames()
            iconCache.preloadIcons(packageNames)
        }
        return result
    }

    override suspend fun setFavorite(packageName: String, isFavorite: Boolean) {
        val existing = appDao.getApp(packageName) ?: return
        appDao.upsertApp(existing.copy(isFavorite = isFavorite))
    }

    override suspend fun setWorkspace(packageName: String, workspaceId: Int) {
        val existing = appDao.getApp(packageName) ?: return
        appDao.upsertApp(existing.copy(workspaceId = workspaceId))
    }

    override suspend fun setHidden(packageName: String, isHidden: Boolean) {
        val existing = appDao.getApp(packageName) ?: return
        appDao.upsertApp(existing.copy(isHidden = isHidden))
    }
}
