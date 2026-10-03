package com.hyprlauncher.feature.drawer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.hyprlauncher.core.icon.DefaultIconCache
import com.hyprlauncher.core.platform.DefaultAppLauncher
import com.hyprlauncher.core.platform.DefaultPackageDiscoveryManager
import com.hyprlauncher.core.search.DefaultAppSearchEngine
import com.hyprlauncher.core.search.MatchType
import com.hyprlauncher.data.database.HyprDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.repository.DefaultAppRepository
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
import com.hyprlauncher.domain.usecase.LaunchAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DrawerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var database: HyprDatabase
    private lateinit var appDao: AppDao
    private lateinit var appRepository: DefaultAppRepository
    private lateinit var viewModel: DrawerViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, HyprDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = database.appDao()

        val iconCache = DefaultIconCache(context, testDispatcher)
        val discoveryManager = DefaultPackageDiscoveryManager(context, testDispatcher)
        val indexUseCase = DiscoverAndIndexAppsUseCase(discoveryManager, appDao, testDispatcher)
        appRepository = DefaultAppRepository(appDao, iconCache, indexUseCase)
        val searchEngine = DefaultAppSearchEngine()
        val appLauncher = DefaultAppLauncher(context, appDao)
        val launchAppUseCase = LaunchAppUseCase(appLauncher)

        // Seed apps
        appDao.upsertApp(AppEntity(packageName = "org.mozilla.firefox", activityName = "MainActivity", label = "Firefox", launchCount = 10, lastUsedTimestamp = 1000L))
        appDao.upsertApp(AppEntity(packageName = "com.google.android.youtube", activityName = "HomeActivity", label = "YouTube", launchCount = 50, lastUsedTimestamp = 2000L))
        appDao.upsertApp(AppEntity(packageName = "com.android.calculator2", activityName = "CalcActivity", label = "Calculator", launchCount = 5))
        appDao.upsertApp(AppEntity(packageName = "com.android.camera", activityName = "CameraActivity", label = "Camera", launchCount = 2, isFavorite = true, lastUsedTimestamp = 3000L))
        appDao.upsertApp(AppEntity(packageName = "com.termux", activityName = "TermuxActivity", label = "Terminal", launchCount = 30))

        viewModel = DrawerViewModel(
            appRepository = appRepository,
            searchEngine = searchEngine,
            launchAppUseCase = launchAppUseCase
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiStateGroupsAlphabeticallyAndExtractsFavoritesAndRecents() = runTest(testDispatcher) {
        val job = launch(testDispatcher) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.first { it.allApps.isNotEmpty() }
        assertEquals(5, state.totalApps)

        // Check favorites
        assertEquals(1, state.favoriteApps.size)
        assertEquals("com.android.camera", state.favoriteApps.first().packageName)

        // Check recents (Camera was 3000, YouTube 2000, Firefox 1000)
        assertEquals(3, state.recentApps.size)
        assertEquals("com.android.camera", state.recentApps[0].packageName)
        assertEquals("com.google.android.youtube", state.recentApps[1].packageName)
        assertEquals("org.mozilla.firefox", state.recentApps[2].packageName)

        // Check alphabetical groups
        assertTrue(state.alphabeticalGroups.containsKey('C'))
        assertTrue(state.alphabeticalGroups.containsKey('F'))
        assertTrue(state.alphabeticalGroups.containsKey('T'))
        assertTrue(state.alphabeticalGroups.containsKey('Y'))

        job.cancel()
    }

    @Test
    fun searchFiltersAndRanksResultsInstantaneously() = runTest(testDispatcher) {
        val job = launch(testDispatcher) { viewModel.uiState.collect {} }

        // Search acronym "yt"
        viewModel.onSearchQueryChanged("yt")
        val searchState = viewModel.uiState.first { it.searchQuery == "yt" && it.searchResults.isNotEmpty() }

        assertEquals(1, searchState.searchResults.size)
        assertEquals("com.google.android.youtube", searchState.searchResults.first().app.packageName)
        assertEquals(MatchType.ACRONYM, searchState.searchResults.first().matchType)

        job.cancel()
    }

    @Test
    fun toggleFavoriteUpdatesAppFavoriteStatus() = runTest(testDispatcher) {
        val job = launch(testDispatcher) { viewModel.uiState.collect {} }

        val initialState = viewModel.uiState.first { it.allApps.isNotEmpty() }
        assertEquals(1, initialState.favoriteApps.size)

        // Toggle Firefox to favorite
        viewModel.toggleFavorite("org.mozilla.firefox", current = false)

        val updatedState = viewModel.uiState.first { it.favoriteApps.size == 2 }
        assertTrue(updatedState.favoriteApps.any { it.packageName == "org.mozilla.firefox" })

        job.cancel()
    }

    @Test
    fun tabSelectionUpdatesSelectedTab() = runTest(testDispatcher) {
        val job = launch(testDispatcher) { viewModel.uiState.collect {} }

        val initial = viewModel.uiState.first { it.selectedTab == DrawerTab.ALL }
        assertEquals(DrawerTab.ALL, initial.selectedTab)

        viewModel.onSelectTab(DrawerTab.FAVORITES)
        val favState = viewModel.uiState.first { it.selectedTab == DrawerTab.FAVORITES }
        assertEquals(DrawerTab.FAVORITES, favState.selectedTab)

        viewModel.onSelectTab(DrawerTab.RECENTS)
        val recState = viewModel.uiState.first { it.selectedTab == DrawerTab.RECENTS }
        assertEquals(DrawerTab.RECENTS, recState.selectedTab)

        job.cancel()
    }
}
