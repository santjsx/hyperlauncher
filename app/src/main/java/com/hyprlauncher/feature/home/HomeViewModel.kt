package com.hyprlauncher.feature.home

import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.core.platform.LaunchResult
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import com.hyprlauncher.data.datastore.LauncherPreferences
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import com.hyprlauncher.data.repository.AppRepository
import com.hyprlauncher.domain.usecase.GetLauncherRoleStatusUseCase
import com.hyprlauncher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val apps: List<AppEntity> = emptyList(),
    val dockApps: List<AppEntity> = emptyList(),
    val filteredApps: List<AppEntity> = emptyList(),
    val searchQuery: String = "",
    val totalAppsIndexed: Int = 0,
    val isDefaultLauncher: Boolean = true,
    val requestDefaultIntent: Intent? = null,
    val isLoading: Boolean = false,
    val lastLaunchResult: LaunchResult? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: LauncherPreferencesRepository,
    private val workspaceDao: WorkspaceDao,
    private val appRepository: AppRepository,
    private val launchAppUseCase: LaunchAppUseCase,
    private val getLauncherRoleStatusUseCase: GetLauncherRoleStatusUseCase
) : ViewModel() {

    private data class CoreData(
        val preferences: LauncherPreferences,
        val workspaces: List<WorkspaceEntity>,
        val apps: List<AppEntity>
    )

    private val roleStatusFlow = MutableStateFlow(getLauncherRoleStatusUseCase())
    private val lastLaunchResult = MutableStateFlow<LaunchResult?>(null)
    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Seed default workspaces if none exist (PRD Section 17)
        viewModelScope.launch {
            if (workspaceDao.getWorkspaceCount() == 0) {
                val defaultWorkspaces = listOf(
                    WorkspaceEntity(id = 1, name = "Main", orderIndex = 1, iconName = "terminal"),
                    WorkspaceEntity(id = 2, name = "Work", orderIndex = 2, iconName = "briefcase"),
                    WorkspaceEntity(id = 3, name = "Dev", orderIndex = 3, iconName = "code"),
                    WorkspaceEntity(id = 4, name = "Media", orderIndex = 4, iconName = "play"),
                    WorkspaceEntity(id = 5, name = "Games", orderIndex = 5, iconName = "gamepad")
                )
                workspaceDao.upsertWorkspaces(defaultWorkspaces)
            }
        }

        // Perform initial application discovery & indexing via AppRepository
        viewModelScope.launch {
            appRepository.syncAllApps()
        }
    }

    private val coreDataFlow = combine(
        preferencesRepository.preferences,
        workspaceDao.getAllWorkspaces(),
        appRepository.allApps
    ) { prefs, workspaces, apps ->
        CoreData(prefs, workspaces, apps)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreDataFlow,
        searchQueryFlow,
        roleStatusFlow,
        lastLaunchResult
    ) { coreData, query, roleStatus, launchResult ->
        val apps = coreData.apps
        val prefs = coreData.preferences

        val filtered = if (query.isNotBlank()) {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        } else {
            apps
        }

        val dockApps = if (prefs.dockPackageNames.isNotEmpty()) {
            val appMap = apps.associateBy { it.packageName }
            prefs.dockPackageNames.mapNotNull { appMap[it] }
        } else {
            // Default dock takes up to 5 initial apps
            apps.take(5)
        }

        HomeUiState(
            preferences = prefs,
            workspaces = coreData.workspaces,
            apps = apps,
            dockApps = dockApps,
            filteredApps = filtered,
            searchQuery = query,
            totalAppsIndexed = apps.size,
            isDefaultLauncher = roleStatus.isDefault,
            requestDefaultIntent = roleStatus.requestIntent,
            isLoading = false,
            lastLaunchResult = launchResult
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onWorkspaceSelected(workspaceId: Int) {
        viewModelScope.launch {
            preferencesRepository.updateActiveWorkspace(workspaceId)
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun launchApp(packageName: String, activityName: String? = null) {
        val result = launchAppUseCase(packageName, activityName)
        lastLaunchResult.value = result
    }

    suspend fun getAppIcon(packageName: String): Bitmap? {
        return appRepository.getAppIcon(packageName)
    }

    fun refreshRoleStatus() {
        roleStatusFlow.value = getLauncherRoleStatusUseCase()
    }

    fun updateGridLayout(columns: Int, rows: Int) {
        viewModelScope.launch {
            preferencesRepository.updateGridLayout(columns, rows)
        }
    }

    fun updateClockFormat(is24Hour: Boolean, showSeconds: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateClockFormat(is24Hour, showSeconds)
        }
    }

    fun updateDockVisibility(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDockVisibility(show)
        }
    }

    fun updateAppLabelVisibility(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAppLabelVisibility(show)
        }
    }

    fun updateWallpaperSettings(dim: Float, amoledMode: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateWallpaperSettings(dim, amoledMode)
        }
    }
}
