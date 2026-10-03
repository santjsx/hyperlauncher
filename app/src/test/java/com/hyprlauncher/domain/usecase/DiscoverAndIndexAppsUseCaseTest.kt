package com.hyprlauncher.domain.usecase

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.platform.PackageDiscoveryManager
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.domain.model.DiscoveredApp
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class DiscoverAndIndexAppsUseCaseTest {

    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun indexingPreservesExistingUserStats() = runTest {
        // Pre-populate with user customization
        val existingApp = AppEntity(
            packageName = "org.example.browser",
            activityName = "org.example.browser.MainActivity",
            label = "Old Label",
            launchCount = 42,
            isFavorite = true,
            workspaceId = 3
        )
        appDao.upsertApp(existingApp)

        // Mock discovery manager returning updated package
        val fakeDiscoveryManager = object : PackageDiscoveryManager {
            override suspend fun discoverLaunchableApps(): List<DiscoveredApp> {
                return listOf(
                    DiscoveredApp(
                        packageName = "org.example.browser",
                        activityName = "org.example.browser.MainActivity",
                        label = "New Browser Label",
                        category = "INTERNET"
                    ),
                    DiscoveredApp(
                        packageName = "org.example.calculator",
                        activityName = "org.example.calculator.CalcActivity",
                        label = "Calculator",
                        category = "PRODUCTIVITY"
                    )
                )
            }

            override fun getAppIcon(packageName: String): Drawable? = null
        }

        val useCase = DiscoverAndIndexAppsUseCase(
            discoveryManager = fakeDiscoveryManager,
            appDao = appDao,
            ioDispatcher = testDispatcher
        )

        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val updatedApp = appDao.getApp("org.example.browser")
        // Label is updated
        assertEquals("New Browser Label", updatedApp?.label)
        // User statistics and preferences are strictly preserved!
        assertEquals(42, updatedApp?.launchCount)
        assertEquals(true, updatedApp?.isFavorite)
        assertEquals(3, updatedApp?.workspaceId)
    }
}
