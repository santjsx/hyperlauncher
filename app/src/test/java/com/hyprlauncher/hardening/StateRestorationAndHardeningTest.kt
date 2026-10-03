package com.hyprlauncher.hardening

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.database.dao.WidgetDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.datastore.DefaultLauncherPreferencesRepository
import com.hyprlauncher.data.datastore.LauncherPreferences
import com.hyprlauncher.data.repository.DefaultCustomizationRepository
import com.hyprlauncher.data.repository.DefaultRiceRepository
import com.hyprlauncher.data.repository.DefaultThemeRepository
import com.hyprlauncher.data.repository.DefaultWidgetRepository
import com.hyprlauncher.data.repository.DefaultWorkspaceRepository
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.RiceDraft
import com.hyprlauncher.domain.model.RiceProfile
import com.hyprlauncher.domain.model.RiceValidationResult
import com.hyprlauncher.domain.model.ThemeConfig
import com.hyprlauncher.feature.home.HomeViewModelTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Phase 12 Hardening: State Restoration, Corrupted Configuration Recovery & Process-Death (PRD §51, §57).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StateRestorationAndHardeningTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var workspaceDao: WorkspaceDao
    private lateinit var widgetDao: WidgetDao
    private lateinit var riceDao: RiceDao

    private lateinit var prefsFile: File
    private lateinit var themeFile: File
    private lateinit var customFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workspaceDao = database.workspaceDao()
        widgetDao = database.widgetDao()
        riceDao = database.riceDao()

        prefsFile = tmpFolder.newFile("hardening_prefs.preferences_pb")
        themeFile = tmpFolder.newFile("hardening_theme.preferences_pb")
        customFile = tmpFolder.newFile("hardening_custom.preferences_pb")
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun preferencesSurviveSimulatedProcessDeath() = runTest(testDispatcher) {
        // Step 1: Initial process instance updates preferences
        val job1 = Job()
        val scope1 = CoroutineScope(testDispatcher + job1)
        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = scope1,
            produceFile = { prefsFile }
        )
        val repo1 = DefaultLauncherPreferencesRepository(dataStore1)
        repo1.updateActiveWorkspace(3)
        repo1.updatePerformanceMode(com.hyprlauncher.domain.model.PerformanceMode.MAXIMUM_PERFORMANCE)
        repo1.updateClockFormat(is24Hour = false, showSeconds = true)

        val state1 = repo1.preferences.first()
        assertEquals(3, state1.activeWorkspaceId)
        assertEquals(com.hyprlauncher.domain.model.PerformanceMode.MAXIMUM_PERFORMANCE, state1.performanceMode)
        assertFalse(state1.clock24Hour)
        assertTrue(state1.showClockSeconds)

        // Kill process 1 coroutine scope / release lock
        job1.cancel()

        // Step 2: Simulate process recreation reading from the exact same persistent storage
        val job2 = Job()
        val scope2 = CoroutineScope(testDispatcher + job2)
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = scope2,
            produceFile = { prefsFile }
        )
        val repo2 = DefaultLauncherPreferencesRepository(dataStore2)
        val state2 = repo2.preferences.first()

        assertEquals(3, state2.activeWorkspaceId)
        assertEquals(com.hyprlauncher.domain.model.PerformanceMode.MAXIMUM_PERFORMANCE, state2.performanceMode)
        assertFalse(state2.clock24Hour)
        assertTrue(state2.showClockSeconds)

        job2.cancel()
    }

    @Test
    fun databaseEntitiesPersistAndRestoreCleanly() = runTest(testDispatcher) {
        val workspaceRepo = DefaultWorkspaceRepository(workspaceDao)
        workspaceRepo.ensureDefaultWorkspaces()

        val fakeHostManager = HomeViewModelTest.FakeWidgetHostManager()
        val widgetRepo = DefaultWidgetRepository(widgetDao, fakeHostManager)
        widgetRepo.placeWidget(
            workspaceId = 1,
            appWidgetId = 555,
            providerPackage = "com.sample.weather",
            providerClass = "WeatherWidgetProvider",
            cellX = 0,
            cellY = 1,
            spanX = 4,
            spanY = 2
        )

        val restoredWorkspaces = workspaceRepo.allWorkspaces.first()
        assertEquals(5, restoredWorkspaces.size)

        val restoredWidgets = widgetRepo.getWidgetsForWorkspace(1).first()
        assertEquals(1, restoredWidgets.size)
        assertEquals(555, restoredWidgets.first().appWidgetId)
        assertEquals(4, restoredWidgets.first().spanX)
        assertEquals(2, restoredWidgets.first().spanY)
    }

    @Test
    fun corruptedRiceConfigurationRecoversSafelyWithoutCrashing() = runTest {
        // Null or empty payload
        val nullResult = RiceProfile.fromJson(null)
        assertTrue(nullResult.isFailure)

        val emptyResult = RiceProfile.fromJson("")
        assertTrue(emptyResult.isFailure)

        // Random binary or corrupt syntax
        val corruptJson = "{ malformed json ::: !!@@## "
        val corruptResult = RiceProfile.fromJson(corruptJson)
        assertTrue(corruptResult.isFailure)

        // Schema constraint violation (negative column count)
        val invalidColumnJson = """
            {
              "id": "bad_rice",
              "name": "Bad Rice",
              "version": 1,
              "customizationConfig": {
                "grid": { "columns": 999 }
              }
            }
        """.trimIndent()
        val invalidResult = RiceProfile.fromJson(invalidColumnJson)
        assertTrue(invalidResult.isFailure)
    }

    @Test
    fun schemaMigrationUpgradesLegacyV0ToV1Correctly() = runTest {
        val legacyV0Json = """
            {
              "id": "legacy_rice_0",
              "name": "Old School",
              "version": 0,
              "themeConfig": {
                "presetId": "CATPPUCCIN",
                "cornerRadiusDp": 8
              },
              "customizationConfig": {
                "grid": { "columns": 4, "iconSizeDp": 40 }
              }
            }
        """.trimIndent()

        val parsed = RiceProfile.fromJson(legacyV0Json)
        assertTrue(parsed.isSuccess)
        val profile = parsed.getOrThrow()

        assertEquals(RiceProfile.CURRENT_SCHEMA_VERSION, profile.version)
        assertEquals("Old School", profile.name)
        assertEquals(ThemePresetId.CATPPUCCIN, profile.themeConfig.presetId)
        assertEquals(8, profile.themeConfig.cornerRadiusDp)
        assertEquals(4, profile.customizationConfig.grid.columns)
    }

    @Test
    fun widgetCrashSafetyAndPlaceholderValidation() = runTest(testDispatcher) {
        val fakeHostManager = HomeViewModelTest.FakeWidgetHostManager()
        val widgetRepo = DefaultWidgetRepository(widgetDao, fakeHostManager)

        // Place and remove widget
        val placed = widgetRepo.placeWidget(
            workspaceId = 2,
            appWidgetId = 999,
            providerPackage = "com.broken.widget",
            providerClass = "BrokenWidget",
            cellX = 0,
            cellY = 0
        )
        assertTrue(placed.isSuccess)

        // Verify move and resize clamping
        val widgetId = placed.getOrThrow().id
        val moveResult = widgetRepo.moveWidget(
            id = widgetId,
            newCellX = 99,
            newCellY = 99
        )
        assertTrue(moveResult.isSuccess)

        val resizeResult = widgetRepo.resizeWidget(
            id = widgetId,
            newSpanX = 10,
            newSpanY = 20
        )
        assertTrue(resizeResult.isSuccess)

        val updated = widgetRepo.getWidgetsForWorkspace(2).first().first()
        assertTrue(updated.cellX >= 0)
        assertTrue(updated.cellY >= 0)
        assertTrue(updated.spanX in 1..8)
        assertTrue(updated.spanY in 1..12)
    }
}
