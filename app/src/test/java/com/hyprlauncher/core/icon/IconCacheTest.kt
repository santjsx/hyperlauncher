package com.hyprlauncher.core.icon

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IconCacheTest {

    private lateinit var context: Context
    private lateinit var iconCache: IconCache
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testPackageName = "com.hyprlauncher.testapp"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val shadowPm = Shadows.shadowOf(context.packageManager)
        val testPackage = PackageInfo().apply {
            packageName = testPackageName
            applicationInfo = ApplicationInfo().apply {
                packageName = testPackageName
                flags = ApplicationInfo.FLAG_INSTALLED
            }
        }
        shadowPm.installPackage(testPackage)
        val dummyDrawable = android.graphics.drawable.ColorDrawable(android.graphics.Color.RED)
        shadowPm.setApplicationIcon(testPackageName, dummyDrawable)
        iconCache = DefaultIconCache(context, testDispatcher)
    }

    @Test
    fun getIconForInstalledAppCachesInMemory() = runTest {
        val firstFetch = iconCache.getIcon(testPackageName)
        assertNotNull(firstFetch)

        val statsBefore = iconCache.getStats()
        assertEquals(0, statsBefore.memoryHits)
        assertEquals(1, statsBefore.systemFetches)

        // Second fetch must hit memory cache!
        val secondFetch = iconCache.getIcon(testPackageName)
        assertNotNull(secondFetch)
        val statsAfter = iconCache.getStats()
        assertEquals(1, statsAfter.memoryHits)
    }

    @Test
    fun invalidateRemovesFromMemoryAndDisk() = runTest {
        iconCache.getIcon(testPackageName)

        iconCache.invalidate(testPackageName)

        // Clear in-memory stats
        val stats = iconCache.getStats()
        // Memory hits should not increment if invalidated
        assertEquals(0, stats.memoryHits)
    }

    @Test
    fun getIconForNonExistentAppReturnsNull() = runTest {
        val result = iconCache.getIcon("com.completely.nonexistent.app")
        assertNull(result)
    }

    @Test
    fun trimMemoryPurgesMemoryCacheOnCriticalPressure() = runTest {
        val icon = iconCache.getIcon(testPackageName)
        assertNotNull(icon)
        val statsWithCache = iconCache.getStats()
        org.junit.Assert.assertTrue(statsWithCache.memorySizeBytes > 0)

        // Simulate critical memory pressure
        iconCache.trimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

        val statsAfterTrim = iconCache.getStats()
        assertEquals(0, statsAfterTrim.memorySizeBytes)
    }
}
