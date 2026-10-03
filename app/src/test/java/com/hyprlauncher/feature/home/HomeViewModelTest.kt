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
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import com.hyprlauncher.core.widget.WidgetHostManager
import com.hyprlauncher.data.repository.DefaultWidgetRepository
import com.hyprlauncher.data.repository.WidgetRepository
import com.hyprlauncher.domain.model.WidgetProviderItem
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
import com.hyprlauncher.domain.usecase.GetLauncherRoleStatusUseCase
import com.hyprlauncher.domain.usecase.LaunchAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private lateinit var widgetRepository: WidgetRepository
    private lateinit var fakeWidgetHostManager: FakeWidgetHostManager
    private lateinit var viewModel: HomeViewModel

    class FakeWidgetHostManager : WidgetHostManager {
        var isListening = false
        private var nextId = 100
        val deletedIds = mutableListOf<Int>()
        val boundComponents = mutableMapOf<Int, ComponentName>()
        var providers = listOf(
            WidgetProviderItem(
                providerPackage = "com.hyprlauncher.weather",
                providerClass = "com.hyprlauncher.weather.WeatherWidget",
                appLabel = "Weather",
                widgetLabel = "Live Weather",
                minWidthDp = 140,
                minHeightDp = 70,
                minSpanX = 2,
                minSpanY = 1
            ),
            WidgetProviderItem(
                providerPackage = "com.hyprlauncher.clock",
                providerClass = "com.hyprlauncher.clock.AnalogClockWidget",
                appLabel = "Clock",
                widgetLabel = "Analog Clock",
                minWidthDp = 140,
                minHeightDp = 140,
                minSpanX = 2,
                minSpanY = 2
            )
        )

        override fun startListening() { isListening = true }
        override fun stopListening() { isListening = false }
        override fun allocateAppWidgetId(): Int = nextId++
        override fun deleteAppWidgetId(appWidgetId: Int) { deletedIds.add(appWidgetId) }
        override fun getAvailableProviders(): List<WidgetProviderItem> = providers
        override fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? = null
        override fun createView(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView? = null
        override fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean {
            boundComponents[appWidgetId] = provider
            return true
        }
    }

    @Before
    fun setup() = runBlocking(testDispatcher) {
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

        fakeWidgetHostManager = FakeWidgetHostManager()
        widgetRepository = DefaultWidgetRepository(database.widgetDao(), fakeWidgetHostManager)

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
            widgetRepository = widgetRepository,
            widgetHostManager = fakeWidgetHostManager,
            launchAppUseCase = launchAppUseCase,
            getLauncherRoleStatusUseCase = getRoleStatusUseCase
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            runCatching { database.close() }
        }
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

    @Test
    fun placeWidgetAllocatesAndPersistsWidgetInActiveWorkspace() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        val provider = fakeWidgetHostManager.providers.first()
        viewModel.placeWidget(provider = provider, cellX = 0, cellY = 0)

        val state = viewModel.uiState.first { it.widgets.isNotEmpty() }
        assertEquals(1, state.widgets.size)
        val placed = state.widgets.first()
        assertEquals(1, placed.workspaceId)
        assertEquals("com.hyprlauncher.weather", placed.providerPackage)
        assertEquals(2, placed.spanX)
        assertEquals(1, placed.spanY)
        assertEquals(100, placed.appWidgetId)
        assertTrue(fakeWidgetHostManager.boundComponents.containsKey(100))
    }

    @Test
    fun resizeWidgetUpdatesDimensionsInUiState() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        val provider = fakeWidgetHostManager.providers.first()
        viewModel.placeWidget(provider = provider, cellX = 0, cellY = 0)
        val initial = viewModel.uiState.first { it.widgets.isNotEmpty() }
        val widgetId = initial.widgets.first().id

        viewModel.resizeWidget(widgetId, spanX = 4, spanY = 3)
        val updated = viewModel.uiState.first { it.widgets.any { w -> w.spanX == 4 && w.spanY == 3 } }
        val resized = updated.widgets.first { it.id == widgetId }
        assertEquals(4, resized.spanX)
        assertEquals(3, resized.spanY)
    }

    @Test
    fun removeWidgetDeletesWidgetAndCleansUpHostId() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        val provider = fakeWidgetHostManager.providers.first()
        viewModel.placeWidget(provider = provider, cellX = 0, cellY = 0)
        val initial = viewModel.uiState.first { it.widgets.isNotEmpty() }
        val widget = initial.widgets.first()

        viewModel.removeWidget(widget.id)
        val updated = viewModel.uiState.first { it.widgets.isEmpty() }
        assertEquals(0, updated.widgets.size)
        assertTrue(fakeWidgetHostManager.deletedIds.contains(widget.appWidgetId))
    }

    @Test
    fun switchingWorkspacesFiltersWidgetsToActiveWorkspaceOnly() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect {} }
        viewModel.uiState.first { it.workspaces.isNotEmpty() }

        // Place widget in workspace 1
        val provider1 = fakeWidgetHostManager.providers[0]
        viewModel.placeWidget(provider = provider1, cellX = 0, cellY = 0)
        val ws1State = viewModel.uiState.first { it.widgets.size == 1 }
        assertEquals(1, ws1State.widgets.first().workspaceId)

        // Switch to workspace 2
        viewModel.onWorkspaceSelected(2)
        val ws2EmptyState = viewModel.uiState.first { it.preferences.activeWorkspaceId == 2 && it.widgets.isEmpty() }
        assertEquals(0, ws2EmptyState.widgets.size)

        // Place widget in workspace 2
        val provider2 = fakeWidgetHostManager.providers[1]
        viewModel.placeWidget(provider = provider2, cellX = 1, cellY = 1)
        val ws2State = viewModel.uiState.first { it.preferences.activeWorkspaceId == 2 && it.widgets.size == 1 }
        assertEquals(2, ws2State.widgets.first().workspaceId)
        assertEquals("com.hyprlauncher.clock", ws2State.widgets.first().providerPackage)

        // Switch back to workspace 1
        viewModel.onWorkspaceSelected(1)
        val ws1ReturnState = viewModel.uiState.first { it.preferences.activeWorkspaceId == 1 && it.widgets.size == 1 }
        assertEquals(1, ws1ReturnState.widgets.first().workspaceId)
        assertEquals("com.hyprlauncher.weather", ws1ReturnState.widgets.first().providerPackage)
    }
}
