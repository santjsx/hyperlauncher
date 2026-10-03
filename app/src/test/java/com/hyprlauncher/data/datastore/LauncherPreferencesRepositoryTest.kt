package com.hyprlauncher.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.domain.model.PerformanceMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
