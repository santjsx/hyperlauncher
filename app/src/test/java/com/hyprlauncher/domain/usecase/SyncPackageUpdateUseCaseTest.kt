package com.hyprlauncher.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.icon.CacheStats
import com.hyprlauncher.core.icon.IconCache
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncPackageUpdateUseCaseTest {

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private val testDispatcher = UnconfinedTestDispatcher()

    private val invalidatedPackages = mutableListOf<String>()

    private val fakeIconCache = object : IconCache {
        override suspend fun getIcon(packageName: String, activityName: String?): Bitmap? = null
        override suspend fun invalidate(packageName: String) {
            invalidatedPackages.add(packageName)
        }
        override suspend fun clear() {}
        override fun getStats(): CacheStats = CacheStats(0, 0, 0, 0, 0)
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()
        invalidatedPackages.clear()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun handlePackageRemovedDeletesFromDbAndInvalidatesIconCache() = runTest {
        val app = AppEntity(
            packageName = "com.sample.uninstall",
            activityName = "com.sample.uninstall.Main",
            label = "Sample"
        )
        appDao.upsertApp(app)
        assertEquals(1, appDao.getAppCount())

        val useCase = SyncPackageUpdateUseCase(
            context = context,
            appDao = appDao,
            iconCache = fakeIconCache,
            ioDispatcher = testDispatcher
        )

        val result = useCase.handlePackageRemoved("com.sample.uninstall")
        assertTrue(result.isSuccess)
        assertEquals(0, appDao.getAppCount())
        assertNull(appDao.getApp("com.sample.uninstall"))
        assertTrue(invalidatedPackages.contains("com.sample.uninstall"))
    }
}
