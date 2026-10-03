package com.hyprlauncher.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.icon.CacheStats
import com.hyprlauncher.core.icon.IconCache
import com.hyprlauncher.core.platform.PackageDiscoveryManager
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.domain.model.DiscoveredApp
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppRepositoryTest {

    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private lateinit var repository: AppRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeIconCache = object : IconCache {
        override suspend fun getIcon(packageName: String, activityName: String?): Bitmap? = null
        override suspend fun invalidate(packageName: String) {}
        override suspend fun clear() {}
        override fun getStats(): CacheStats = CacheStats(0, 0, 0, 0, 0)
    }

    private val fakeDiscoveryManager = object : PackageDiscoveryManager {
        override suspend fun discoverLaunchableApps(): List<DiscoveredApp> = listOf(
            DiscoveredApp("com.app.one", "com.app.one.Main", "App One"),
            DiscoveredApp("com.app.two", "com.app.two.Main", "App Two")
        )
        override fun getAppIcon(packageName: String) = null
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()

        val discoverUseCase = DiscoverAndIndexAppsUseCase(
            discoveryManager = fakeDiscoveryManager,
            appDao = appDao,
            ioDispatcher = testDispatcher
        )

        repository = DefaultAppRepository(
            appDao = appDao,
            iconCache = fakeIconCache,
            discoverAndIndexAppsUseCase = discoverUseCase
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun syncAllAppsAndVerifyQueries() = runTest {
        val syncResult = repository.syncAllApps()
        assertTrue(syncResult.isSuccess)
        assertEquals(2, syncResult.getOrNull())

        val allApps = repository.allApps.first()
        assertEquals(2, allApps.size)
    }

    @Test
    fun setFavoriteUpdatesAppInDatabase() = runTest {
        val app = AppEntity("com.fav.test", "com.fav.test.Main", "Favorite Test")
        appDao.upsertApp(app)

        repository.setFavorite("com.fav.test", true)

        val favorites = repository.favoriteApps.first()
        assertEquals(1, favorites.size)
        assertEquals("com.fav.test", favorites[0].packageName)
    }

    @Test
    fun setWorkspaceAssignsAppToTargetWorkspace() = runTest {
        val app = AppEntity("com.work.test", "com.work.test.Main", "Work Test", workspaceId = 1)
        appDao.upsertApp(app)

        repository.setWorkspace("com.work.test", 3)

        val workspace3Apps = repository.getAppsForWorkspace(3).first()
        assertEquals(1, workspace3Apps.size)
        assertEquals("com.work.test", workspace3Apps[0].packageName)
    }
}
