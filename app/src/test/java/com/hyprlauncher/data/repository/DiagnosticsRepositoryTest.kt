package com.hyprlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.diagnostics.DefaultPerformanceMetricsManager
import com.hyprlauncher.core.diagnostics.PerformanceMetricsManager
import com.hyprlauncher.core.diagnostics.StartupTracker
import com.hyprlauncher.core.icon.DefaultIconCache
import com.hyprlauncher.core.icon.IconCache
import com.hyprlauncher.core.platform.DefaultPackageDiscoveryManager
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.database.dao.WidgetDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.datastore.DefaultLauncherPreferencesRepository
import com.hyprlauncher.domain.model.LauncherWidget
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
import com.hyprlauncher.feature.home.HomeViewModelTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DiagnosticsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private lateinit var widgetDao: WidgetDao
    private lateinit var riceDao: RiceDao
    private lateinit var startupTracker: StartupTracker
    private lateinit var metricsManager: PerformanceMetricsManager
    private lateinit var iconCache: IconCache
    private lateinit var appRepository: AppRepository
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var widgetRepository: WidgetRepository
    private lateinit var riceRepository: RiceRepository
    private lateinit var preferencesRepository: DefaultLauncherPreferencesRepository
    private lateinit var diagnosticsRepository: DiagnosticsRepository

    @Before
    fun setup() = runBlocking(testDispatcher) {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()
        widgetDao = database.widgetDao()
        riceDao = database.riceDao()

        startupTracker = StartupTracker()
        metricsManager = DefaultPerformanceMetricsManager()
        iconCache = DefaultIconCache(context, testDispatcher)

        val discoveryManager = DefaultPackageDiscoveryManager(context, testDispatcher)
        val indexUseCase = DiscoverAndIndexAppsUseCase(discoveryManager, appDao, testDispatcher)
        appRepository = DefaultAppRepository(appDao, iconCache, indexUseCase)
        workspaceRepository = DefaultWorkspaceRepository(database.workspaceDao())
        widgetRepository = DefaultWidgetRepository(widgetDao, HomeViewModelTest.FakeWidgetHostManager())

        val themeDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("diag_theme.preferences_pb") }
        )
        val themeRepo = DefaultThemeRepository(themeDataStore)
        val customDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("diag_custom.preferences_pb") }
        )
        val customRepo = DefaultCustomizationRepository(customDataStore)
        riceRepository = DefaultRiceRepository(riceDao, themeRepo, customRepo)

        val prefsDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("diag_prefs.preferences_pb") }
        )
        preferencesRepository = DefaultLauncherPreferencesRepository(prefsDataStore)

        // Seed some test data
        appDao.upsertApp(AppEntity("pkg.terminal", "MainActivity", "Terminal"))
        appDao.upsertApp(AppEntity("pkg.browser", "MainActivity", "Browser"))
        workspaceRepository.ensureDefaultWorkspaces()
        riceRepository.ensureDefaultRices()
        widgetRepository.placeWidget(1, 101, "com.test.widget", "Provider", 0, 0)

        diagnosticsRepository = DefaultDiagnosticsRepository(
            context = context,
            startupTracker = startupTracker,
            metricsManager = metricsManager,
            iconCache = iconCache,
            appRepository = appRepository,
            workspaceRepository = workspaceRepository,
            widgetRepository = widgetRepository,
            riceRepository = riceRepository,
            preferencesRepository = preferencesRepository,
            appDao = appDao,
            widgetDao = widgetDao,
            riceDao = riceDao,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            runCatching { database.close() }
        }
    }

    @Test
    fun getLiveReportPopulatesSystemMetricsAndInventory() = runTest(testDispatcher) {
        metricsManager.recordSearchLatency(14L)
        val report = diagnosticsRepository.getLiveReport()

        assertNotNull(report)
        assertEquals(2, report.appCount)
        assertEquals(5, report.workspaceCount)
        assertEquals(1, report.widgetCount)
        assertEquals(5, report.riceProfileCount)
        assertEquals(14L, report.searchLatencyMs)
        assertTrue(report.heapMaxMb > 0)
        assertTrue(report.deviceModel.isNotBlank())
        assertTrue(report.androidVersion.contains("Android"))
    }

    @Test
    fun exportDiagnosticsJsonProducesValidSanitizedJson() = runTest(testDispatcher) {
        val result = diagnosticsRepository.exportDiagnostics(asJson = true)
        assertTrue(result.isSuccess)
        val jsonStr = result.getOrThrow()

        val json = JSONObject(jsonStr)
        assertTrue(json.has("timestamp"))
        assertTrue(json.has("device"))
        assertTrue(json.has("startup"))
        assertTrue(json.has("frames"))
        assertTrue(json.has("memory"))
        assertTrue(json.has("database"))

        val dbJson = json.getJSONObject("database")
        assertEquals(2, dbJson.getInt("appsIndexed"))
        assertEquals(1, dbJson.getInt("widgetsPlaced"))
    }

    @Test
    fun exportDiagnosticsAsciiContainsExpectedSections() = runTest(testDispatcher) {
        val result = diagnosticsRepository.exportDiagnostics(asJson = false)
        assertTrue(result.isSuccess)
        val ascii = result.getOrThrow()

        assertTrue(ascii.contains("hyprctl @ diagnostics"))
        assertTrue(ascii.contains("[PERFORMANCE METRICS]"))
        assertTrue(ascii.contains("[MEMORY & STORAGE]"))
        assertTrue(ascii.contains("[LAUNCHER INVENTORY]"))
        assertTrue(ascii.contains("Indexed Apps:   2"))
        assertTrue(ascii.contains("Active Widgets: 1"))
    }

    @Test
    fun maintenanceActionsSucceedWithoutErrors() = runTest(testDispatcher) {
        val clearCacheRes = diagnosticsRepository.clearCache()
        assertTrue(clearCacheRes.isSuccess)

        val reindexRes = diagnosticsRepository.rebuildAppIndex()
        assertTrue(reindexRes.isSuccess)

        val resetUiRes = diagnosticsRepository.resetUiState()
        assertTrue(resetUiRes.isSuccess)
    }
}
