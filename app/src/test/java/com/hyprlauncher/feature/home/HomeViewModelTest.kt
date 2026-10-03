package com.hyprlauncher.feature.home

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.gesture.DefaultGestureActionExecutor
import com.hyprlauncher.core.gesture.DefaultGestureRepository
import com.hyprlauncher.core.gesture.GestureActionExecutor
import com.hyprlauncher.core.gesture.GestureRepository
import com.hyprlauncher.core.gesture.GestureType
import com.hyprlauncher.core.gesture.LauncherAction
import com.hyprlauncher.core.icon.DefaultIconCache
import com.hyprlauncher.core.platform.DefaultAppLauncher
import com.hyprlauncher.core.platform.DefaultLauncherRoleManager
import com.hyprlauncher.core.platform.DefaultPackageDiscoveryManager
import com.hyprlauncher.core.platform.LaunchResult
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.datastore.DefaultLauncherPreferencesRepository
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import com.hyprlauncher.data.repository.DefaultAppRepository
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
import com.hyprlauncher.domain.usecase.GetLauncherRoleStatusUseCase
import com.hyprlauncher.domain.usecase.LaunchAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private lateinit var workspaceDao: WorkspaceDao
    private lateinit var preferencesRepository: LauncherPreferencesRepository
    private lateinit var gestureRepository: GestureRepository
    private lateinit var gestureActionExecutor: GestureActionExecutor
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()
        workspaceDao = database.workspaceDao()

        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("vm_test_prefs.preferences_pb") }
        )
        preferencesRepository = DefaultLauncherPreferencesRepository(dataStore)

        val gestureDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("vm_test_gestures.preferences_pb") }
        )
        gestureRepository = DefaultGestureRepository(gestureDataStore)

        val iconCache = DefaultIconCache(context, testDispatcher)
        val discoveryManager = DefaultPackageDiscoveryManager(context, testDispatcher)
        val indexUseCase = DiscoverAndIndexAppsUseCase(discoveryManager, appDao, testDispatcher)
        val appRepository = DefaultAppRepository(appDao, iconCache, indexUseCase)
        val appLauncher = DefaultAppLauncher(context, appDao)
        val launchAppUseCase = LaunchAppUseCase(appLauncher)
        val roleManager = DefaultLauncherRoleManager(context)
        val getRoleStatusUseCase = GetLauncherRoleStatusUseCase(roleManager)
        gestureActionExecutor = DefaultGestureActionExecutor(context, appLauncher)

        // Seed sample test apps
        appDao.upsertApp(AppEntity(packageName = "org.mozilla.firefox", activityName = "MainActivity", label = "Firefox"))
        appDao.upsertApp(AppEntity(packageName = "com.google.android.youtube", activityName = "HomeActivity", label = "YouTube"))
        appDao.upsertApp(AppEntity(packageName = "com.android.calculator2", activityName = "CalcActivity", label = "Calculator"))
        appDao.upsertApp(AppEntity(packageName = "com.android.camera", activityName = "CameraActivity", label = "Camera"))
        appDao.upsertApp(AppEntity(packageName = "com.android.settings", activityName = "SettingsActivity", label = "Settings"))
        appDao.upsertApp(AppEntity(packageName = "com.termux", activityName = "TermuxActivity", label = "Terminal"))

        viewModel = HomeViewModel(
            preferencesRepository = preferencesRepository,
            workspaceDao = workspaceDao,
            appRepository = appRepository,
            gestureRepository = gestureRepository,
            gestureActionExecutor = gestureActionExecutor,
            launchAppUseCase = launchAppUseCase,
            getLauncherRoleStatusUseCase = getRoleStatusUseCase
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun initialWorkspacesAreSeededIfEmpty() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it.workspaces.isNotEmpty() }
        assertEquals(5, state.workspaces.size)
        assertEquals("Main", state.workspaces[0].name)
        assertEquals("Dev", state.workspaces[2].name)
    }

    @Test
    fun searchQueryFiltersAppsByLabelAndPackage() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.apps.isNotEmpty() }
        assertEquals(6, initial.apps.size)

        // Filter by label
        viewModel.onSearchQueryChanged("calc")
        val searchState = viewModel.uiState.first { it.searchQuery == "calc" }
        assertEquals(1, searchState.filteredApps.size)
        assertEquals("com.android.calculator2", searchState.filteredApps[0].packageName)

        // Clear query returns all apps
        viewModel.onSearchQueryChanged("")
        val clearedState = viewModel.uiState.first { it.searchQuery.isEmpty() }
        assertEquals(6, clearedState.filteredApps.size)
    }

    @Test
    fun dockAppsPopulatesDefaultOrPinnedPackages() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it.dockApps.isNotEmpty() }
        // Default takes first 5
        assertEquals(5, state.dockApps.size)

        // Custom pinned dock packages
        preferencesRepository.updateDockPackages(listOf("com.termux", "org.mozilla.firefox"))
        val updatedState = viewModel.uiState.first { it.preferences.dockPackageNames.isNotEmpty() }
        assertEquals(2, updatedState.dockApps.size)
        assertEquals("com.termux", updatedState.dockApps[0].packageName)
        assertEquals("org.mozilla.firefox", updatedState.dockApps[1].packageName)
    }

    @Test
    fun launchNonExistentAppSetsLaunchResultInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.launchApp("com.nonexistent.app", null)
        val state = viewModel.uiState.first { it.lastLaunchResult != null }
        assertTrue(state.lastLaunchResult is LaunchResult.AppNotFound)
    }

    @Test
    fun defaultGesturesPopulateInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val state = viewModel.uiState.first { it.gestureBindings.isNotEmpty() }
        assertEquals(LauncherAction.OpenAppDrawer, state.gestureBindings[GestureType.SWIPE_UP])
        assertEquals(LauncherAction.OpenNotificationShade, state.gestureBindings[GestureType.SWIPE_DOWN])
        assertEquals(LauncherAction.NextWorkspace, state.gestureBindings[GestureType.SWIPE_LEFT])
        assertEquals(LauncherAction.PreviousWorkspace, state.gestureBindings[GestureType.SWIPE_RIGHT])
    }

    @Test
    fun triggerSwipeUpExecutesOpenDrawerCallback() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        var drawerOpened = false
        viewModel.onGestureTriggered(GestureType.SWIPE_UP) {
            drawerOpened = true
        }
        testScheduler.advanceUntilIdle()
        assertTrue(drawerOpened)
    }

    @Test
    fun updateGestureBindingPersistsAndUpdatesState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.setGestureBinding(GestureType.DOUBLE_TAP, LauncherAction.OpenSettings)
        val state = viewModel.uiState.first { it.gestureBindings[GestureType.DOUBLE_TAP] == LauncherAction.OpenSettings }
        assertEquals(LauncherAction.OpenSettings, state.gestureBindings[GestureType.DOUBLE_TAP])
    }
}
