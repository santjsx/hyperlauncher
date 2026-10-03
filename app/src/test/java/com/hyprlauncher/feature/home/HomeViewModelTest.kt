package com.hyprlauncher.feature.home

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
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
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.datastore.DefaultLauncherPreferencesRepository
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import com.hyprlauncher.data.repository.AppRepository
import com.hyprlauncher.data.repository.DefaultAppRepository
import com.hyprlauncher.data.repository.DefaultThemeRepository
import com.hyprlauncher.data.repository.DefaultWorkspaceRepository
import com.hyprlauncher.data.repository.ThemeRepository
import com.hyprlauncher.data.repository.WorkspaceRepository
import com.hyprlauncher.core.search.DefaultAppSearchEngine
import com.hyprlauncher.data.repository.CustomizationRepository
import com.hyprlauncher.data.repository.DefaultCustomizationRepository
import com.hyprlauncher.domain.model.AnimationScale
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.FontFamilyPreference
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.SearchConfig
import com.hyprlauncher.domain.model.SearchRankingMode
import com.hyprlauncher.domain.model.TypographyConfig
import com.hyprlauncher.domain.model.WorkspaceLayoutConfig
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var themeRepository: ThemeRepository
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
        workspaceRepository = DefaultWorkspaceRepository(database.workspaceDao())

        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("vm_test_prefs.preferences_pb") }
        )
        preferencesRepository = DefaultLauncherPreferencesRepository(dataStore)

        val themeDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("vm_test_theme.preferences_pb") }
        )
        themeRepository = DefaultThemeRepository(themeDataStore)

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

        val customizationDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("vm_test_customization.preferences_pb") }
        )
        val customizationRepository = DefaultCustomizationRepository(customizationDataStore)
        val searchEngine = DefaultAppSearchEngine()

        viewModel = HomeViewModel(
            preferencesRepository = preferencesRepository,
            workspaceRepository = workspaceRepository,
            appRepository = appRepository,
            themeRepository = themeRepository,
            customizationRepository = customizationRepository,
            appSearchEngine = searchEngine,
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

    @Test
    fun workspaceSelectionUpdatesActiveWorkspaceInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.workspaces.isNotEmpty() }
        assertEquals(1, initial.preferences.activeWorkspaceId)
        assertEquals("Main", initial.activeWorkspace?.name)

        viewModel.onWorkspaceSelected(3)
        val updated = viewModel.uiState.first { it.preferences.activeWorkspaceId == 3 }
        assertEquals(3, updated.preferences.activeWorkspaceId)
        assertEquals("Dev", updated.activeWorkspace?.name)
    }

    @Test
    fun createWorkspaceAddsWorkspaceAndSwitchesToIt() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        viewModel.createWorkspace("Hacking")
        val updated = viewModel.uiState.first {
            it.workspaces.size == 6 && it.preferences.activeWorkspaceId == 6
        }
        assertEquals(6, updated.preferences.activeWorkspaceId)
        assertEquals("Hacking", updated.activeWorkspace?.name)
    }

    @Test
    fun renameWorkspaceUpdatesNameInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        viewModel.renameWorkspace(2, "Office")
        val updated = viewModel.uiState.first {
            it.workspaces.any { ws -> ws.id == 2 && ws.name == "Office" }
        }
        val officeWs = updated.workspaces.first { it.id == 2 }
        assertEquals("Office", officeWs.name)
    }

    @Test
    fun deleteWorkspaceRemovesAndFallsBackIfActive() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        viewModel.onWorkspaceSelected(5)
        viewModel.uiState.first { it.preferences.activeWorkspaceId == 5 }

        viewModel.deleteWorkspace(5)
        val updated = viewModel.uiState.first {
            it.workspaces.size == 4 && it.preferences.activeWorkspaceId != 5
        }
        assertEquals(false, updated.workspaces.any { it.id == 5 })
        assertTrue(updated.preferences.activeWorkspaceId != 5)
    }

    @Test
    fun assignedWorkspaceAppsFilterDisplaysOnlyAssignedApps() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() && it.apps.isNotEmpty() }

        // Configure workspace 3 ("Dev") with assigned apps: Termux and Firefox
        val devConfig = WorkspaceLayoutConfig(
            assignedPackageNames = listOf("com.termux", "org.mozilla.firefox")
        )
        viewModel.updateWorkspaceLayout(3, devConfig)

        // Switch to workspace 3
        viewModel.onWorkspaceSelected(3)
        val state = viewModel.uiState.first {
            it.preferences.activeWorkspaceId == 3 && it.activeWorkspace?.layoutConfig?.assignedPackageNames?.isNotEmpty() == true
        }

        // Only the 2 assigned apps should be present in filteredApps
        assertEquals(2, state.filteredApps.size)
        val packages = state.filteredApps.map { it.packageName }.toSet()
        assertTrue(packages.contains("com.termux"))
        assertTrue(packages.contains("org.mozilla.firefox"))
    }

    @Test
    fun selectThemeUpdatesThemeConfigInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        val initial = viewModel.uiState.first { it.themeConfig.presetId == ThemePresetId.ARCH_DARK }
        assertEquals(ThemePresetId.ARCH_DARK, initial.themeConfig.presetId)

        viewModel.selectTheme(ThemePresetId.TOKYO_NIGHT)
        val updated = viewModel.uiState.first { it.themeConfig.presetId == ThemePresetId.TOKYO_NIGHT }
        assertEquals(ThemePresetId.TOKYO_NIGHT, updated.themeConfig.presetId)
    }

    @Test
    fun setAnimationScaleAndCornerRadiusUpdateUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.setAnimationScale(AnimationScale.REDUCED)
        viewModel.setCornerRadius(16)

        val updated = viewModel.uiState.first {
            it.themeConfig.animationScale == AnimationScale.REDUCED && it.themeConfig.cornerRadiusDp == 16
        }
        assertEquals(AnimationScale.REDUCED, updated.themeConfig.animationScale)
        assertEquals(16, updated.themeConfig.cornerRadiusDp)
    }

    @Test
    fun updateCustomizationConfigsReflectedInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        // Test Layout config
        viewModel.updateLayoutConfig(LayoutConfig(showWaybar = false, wallpaperDim = 0.5f))
        val layoutState = viewModel.uiState.first { !it.customizationConfig.layout.showWaybar }
        assertEquals(0.5f, layoutState.customizationConfig.layout.wallpaperDim, 0.01f)

        // Test Dock config
        viewModel.updateDockConfig(DockConfig(iconSizeDp = 48, showLabels = true))
        val dockState = viewModel.uiState.first { it.customizationConfig.dock.iconSizeDp == 48 }
        assertTrue(dockState.customizationConfig.dock.showLabels)

        // Test AppGrid config
        viewModel.updateAppGridConfig(AppGridConfig(columns = 5, rows = 6, iconSizeDp = 42))
        val gridState = viewModel.uiState.first { it.customizationConfig.grid.columns == 5 }
        assertEquals(6, gridState.customizationConfig.grid.rows)
        assertEquals(42, gridState.customizationConfig.grid.iconSizeDp)

        // Test Search config
        viewModel.updateSearchConfig(SearchConfig(rankingMode = SearchRankingMode.FREQUENCY_FIRST))
        val searchState = viewModel.uiState.first { it.customizationConfig.search.rankingMode == SearchRankingMode.FREQUENCY_FIRST }
        assertEquals(SearchRankingMode.FREQUENCY_FIRST, searchState.customizationConfig.search.rankingMode)

        // Test Typography config
        viewModel.updateTypographyConfig(TypographyConfig(fontFamily = FontFamilyPreference.MONOSPACE, fontScale = 1.15f))
        val typeState = viewModel.uiState.first { it.customizationConfig.typography.fontFamily == FontFamilyPreference.MONOSPACE }
        assertEquals(1.15f, typeState.customizationConfig.typography.fontScale, 0.01f)

        // Test Icon config
        viewModel.updateIconConfig(IconConfig(shape = IconShape.SQUIRCLE, tint = IconTint.THEME_ACCENT))
        val iconState = viewModel.uiState.first { it.customizationConfig.icons.shape == IconShape.SQUIRCLE }
        assertEquals(IconTint.THEME_ACCENT, iconState.customizationConfig.icons.tint)
    }

    @Test
    fun pinAndUnpinAppUpdatesDockAppsInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.pinAppToDock("com.termux")
        val pinnedState = viewModel.uiState.first {
            it.customizationConfig.dock.pinnedPackages.contains("com.termux")
        }
        assertTrue(pinnedState.dockApps.any { it.packageName == "com.termux" })

        viewModel.unpinAppFromDock("com.termux")
        val unpinnedState = viewModel.uiState.first {
            !it.customizationConfig.dock.pinnedPackages.contains("com.termux")
        }
        assertFalse(unpinnedState.customizationConfig.dock.pinnedPackages.contains("com.termux"))
    }
}
