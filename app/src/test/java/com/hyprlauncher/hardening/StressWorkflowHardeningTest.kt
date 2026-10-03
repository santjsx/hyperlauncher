package com.hyprlauncher.hardening

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.core.gesture.GestureType
import com.hyprlauncher.core.gesture.resolveSwipeGesture
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.datastore.DefaultLauncherPreferencesRepository
import com.hyprlauncher.data.repository.DefaultThemeRepository
import com.hyprlauncher.data.repository.DefaultWorkspaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
 * Phase 12 Hardening: Stress Testing Rapid Workflows (PRD §51, §53, §57).
 * Tests rapid workspace switching, rapid theme toggling, and rapid gesture dispatch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StressWorkflowHardeningTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun rapidWorkspaceSwitchingMaintainsConsistentState() = runTest(testDispatcher) {
        val workspaceRepo = DefaultWorkspaceRepository(database.workspaceDao())
        workspaceRepo.ensureDefaultWorkspaces()

        val prefsFile = tmpFolder.newFile("stress_prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { prefsFile }
        )
        val prefsRepo = DefaultLauncherPreferencesRepository(dataStore)

        // Rapidly switch active workspaces 500 times
        for (i in 1..500) {
            val targetWs = ((i % 5) + 1)
            prefsRepo.updateActiveWorkspace(targetWs)
        }

        val finalState = prefsRepo.preferences.first()
        assertTrue(finalState.activeWorkspaceId in 1..5)
        assertEquals(1, finalState.activeWorkspaceId) // 500 % 5 = 0 -> (0 + 1) = 1
    }

    @Test
    fun rapidThemeSwitchingMaintainsCleanConvergence() = runTest(testDispatcher) {
        val themeFile = tmpFolder.newFile("stress_theme.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { themeFile }
        )
        val themeRepo = DefaultThemeRepository(dataStore)

        val presets = ThemePresetId.entries
        for (i in 0..100) {
            val targetPreset = presets[i % presets.size]
            themeRepo.selectPreset(targetPreset)
            themeRepo.updateCornerRadius((i % 24))
        }

        val finalTheme = themeRepo.themeConfig.first()
        assertNotNull(finalTheme)
        assertTrue(finalTheme.cornerRadiusDp in 0..24)
        assertNotNull(finalTheme.resolvedColors)
    }

    @Test
    fun rapidGestureResolutionPreventsSpuriousActions() {
        val thresholdPx = 100f

        // Rapid micro-movements (jitter below threshold)
        for (i in -50..50) {
            val resolved = resolveSwipeGesture(i.toFloat(), (i / 2).toFloat(), thresholdPx)
            assertNull("Micro movement jitter should not trigger gesture", resolved)
        }

        // Distinct clear swipe up
        val swipeUp = resolveSwipeGesture(10f, -150f, thresholdPx)
        assertEquals(GestureType.SWIPE_UP, swipeUp)

        // Distinct clear swipe down
        val swipeDown = resolveSwipeGesture(-15f, 180f, thresholdPx)
        assertEquals(GestureType.SWIPE_DOWN, swipeDown)

        // Distinct clear swipe left
        val swipeLeft = resolveSwipeGesture(-200f, 20f, thresholdPx)
        assertEquals(GestureType.SWIPE_LEFT, swipeLeft)

        // Distinct clear swipe right
        val swipeRight = resolveSwipeGesture(220f, -10f, thresholdPx)
        assertEquals(GestureType.SWIPE_RIGHT, swipeRight)
    }
}
