package com.hyprlauncher.feature.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.core.platform.LaunchResult
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import com.hyprlauncher.data.datastore.LauncherPreferences
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import com.hyprlauncher.domain.usecase.DiscoverAndIndexAppsUseCase
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
    private val appDao: AppDao,
    private val discoverAndIndexAppsUseCase: DiscoverAndIndexAppsUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val getLauncherRoleStatusUseCase: GetLauncherRoleStatusUseCase
) : ViewModel() {

    private val roleStatusFlow = MutableStateFlow(getLauncherRoleStatusUseCase())
    private val lastLaunchResult = MutableStateFlow<LaunchResult?>(null)

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

        // Perform initial application discovery & indexing
        viewModelScope.launch {
            discoverAndIndexAppsUseCase()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        workspaceDao.getAllWorkspaces(),
        appDao.getAllVisibleApps(),
        roleStatusFlow,
        lastLaunchResult
    ) { prefs, workspaces, apps, roleStatus, launchResult ->
        HomeUiState(
            preferences = prefs,
            workspaces = workspaces,
            apps = apps,
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

    fun launchApp(packageName: String, activityName: String? = null) {
        val result = launchAppUseCase(packageName, activityName)
        lastLaunchResult.value = result
    }

    fun refreshRoleStatus() {
        roleStatusFlow.value = getLauncherRoleStatusUseCase()
    }
}
