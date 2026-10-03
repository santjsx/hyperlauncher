package com.hyprlauncher.feature.drawer

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.core.platform.LaunchResult
import com.hyprlauncher.core.search.AppSearchEngine
import com.hyprlauncher.core.search.SearchMatchResult
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.repository.AppRepository
import com.hyprlauncher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DrawerTab {
    ALL,
    FAVORITES,
    RECENTS
}

data class DrawerUiState(
    val searchQuery: String = "",
    val selectedTab: DrawerTab = DrawerTab.ALL,
    val allApps: List<AppEntity> = emptyList(),
    val searchResults: List<SearchMatchResult> = emptyList(),
    val favoriteApps: List<AppEntity> = emptyList(),
    val recentApps: List<AppEntity> = emptyList(),
    val alphabeticalGroups: Map<Char, List<AppEntity>> = emptyMap(),
    val totalApps: Int = 0,
    val isLoading: Boolean = false,
    val lastLaunchResult: LaunchResult? = null
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val searchEngine: AppSearchEngine,
    private val launchAppUseCase: LaunchAppUseCase
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedTabFlow = MutableStateFlow(DrawerTab.ALL)
    private val lastLaunchResult = MutableStateFlow<LaunchResult?>(null)

    val uiState: StateFlow<DrawerUiState> = combine(
        appRepository.allApps,
        searchQueryFlow,
        selectedTabFlow,
        lastLaunchResult
    ) { apps, query, tab, launchResult ->
        val results = if (query.isNotBlank()) {
            searchEngine.rankApps(query, apps)
        } else {
            emptyList()
        }

        val favorites = apps.filter { it.isFavorite }
        val recents = apps.filter { it.lastUsedTimestamp > 0 }.sortedByDescending { it.lastUsedTimestamp }

        val alphabetical = apps.sortedBy { it.label.lowercase() }
            .groupBy { it.label.firstOrNull()?.uppercaseChar() ?: '#' }

        DrawerUiState(
            searchQuery = query,
            selectedTab = tab,
            allApps = apps,
            searchResults = results,
            favoriteApps = favorites,
            recentApps = recents,
            alphabeticalGroups = alphabetical,
            totalApps = apps.size,
            isLoading = false,
            lastLaunchResult = launchResult
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DrawerUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun onSelectTab(tab: DrawerTab) {
        selectedTabFlow.value = tab
    }

    fun launchApp(packageName: String, activityName: String? = null) {
        val result = launchAppUseCase(packageName, activityName)
        lastLaunchResult.value = result
    }

    fun toggleFavorite(packageName: String, current: Boolean) {
        viewModelScope.launch {
            appRepository.setFavorite(packageName, !current)
        }
    }

    suspend fun getAppIcon(packageName: String): Bitmap? {
        return appRepository.getAppIcon(packageName)
    }
}
