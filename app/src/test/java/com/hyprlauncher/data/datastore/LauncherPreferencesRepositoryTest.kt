package com.hyprlauncher.data.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hyprlauncher.domain.model.PerformanceMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LauncherPreferencesRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: LauncherPreferencesRepository

    @Before
    fun setup() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        repository = DefaultLauncherPreferencesRepository(testDataStore)
    }

    @Test
    fun defaultPreferencesAreReturnedInitially() = runTest {
        val prefs = repository.preferences.first()
        assertEquals(1, prefs.activeWorkspaceId)
        assertEquals(PerformanceMode.BALANCED, prefs.performanceMode)
        assertEquals(4, prefs.gridColumns)
        assertEquals(5, prefs.gridRows)
        assertTrue(prefs.showClock)
        assertTrue(prefs.showDate)
        assertTrue(prefs.showSearchBar)
        assertTrue(prefs.showAppGrid)
        assertTrue(prefs.showDock)
        assertTrue(prefs.showWaybar)
        assertTrue(prefs.showAppLabels)
        assertEquals(0.2f, prefs.wallpaperDim, 0.001f)
        assertFalse(prefs.wallpaperAmoledMode)
        assertTrue(prefs.dockPackageNames.isEmpty())
    }

    @Test
    fun updateActiveWorkspaceUpdatesPreferences() = runTest {
        repository.updateActiveWorkspace(3)
        val prefs = repository.preferences.first()
        assertEquals(3, prefs.activeWorkspaceId)
    }

    @Test
    fun updatePerformanceModeMaximumDisablesBlur() = runTest {
        repository.updatePerformanceMode(PerformanceMode.MAXIMUM_PERFORMANCE)
        val prefs = repository.preferences.first()
        assertEquals(PerformanceMode.MAXIMUM_PERFORMANCE, prefs.performanceMode)
        assertFalse(prefs.enableBlur)
    }

    @Test
    fun updateGridLayoutClampsWithinSafeBounds() = runTest {
        repository.updateGridLayout(columns = 20, rows = 50)
        val prefs = repository.preferences.first()
        assertEquals(8, prefs.gridColumns)
        assertEquals(10, prefs.gridRows)
    }

    @Test
    fun updateLayoutVisibilityUpdatesVisibilityFlags() = runTest {
        repository.updateLayoutVisibility(
            showClock = false,
            showDate = false,
            showSearchBar = false,
            showDock = false
        )
        val prefs = repository.preferences.first()
        assertFalse(prefs.showClock)
        assertFalse(prefs.showDate)
        assertFalse(prefs.showSearchBar)
        assertFalse(prefs.showDock)
        assertTrue(prefs.showAppGrid) // Unchanged
        assertTrue(prefs.showWaybar)  // Unchanged
    }

    @Test
    fun updateWallpaperSettingsClampsDimLevel() = runTest {
        repository.updateWallpaperSettings(dim = 1.5f, amoledMode = true)
        val prefs = repository.preferences.first()
        assertEquals(1.0f, prefs.wallpaperDim, 0.001f)
        assertTrue(prefs.wallpaperAmoledMode)
    }

    @Test
    fun updateDockPackagesPersistsAndRetrievesList() = runTest {
        val packages = listOf("org.mozilla.firefox", "com.android.calculator2", "com.android.settings")
        repository.updateDockPackages(packages)
        val prefs = repository.preferences.first()
        assertEquals(packages, prefs.dockPackageNames)
    }
}
