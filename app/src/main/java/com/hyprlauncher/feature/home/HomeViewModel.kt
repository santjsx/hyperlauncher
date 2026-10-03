package com.hyprlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import com.hyprlauncher.data.datastore.LauncherPreferences
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val totalAppsIndexed: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: LauncherPreferencesRepository,
    private val workspaceDao: WorkspaceDao,
    private val appDao: AppDao
) : ViewModel() {

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
    }

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        workspaceDao.getAllWorkspaces(),
        appDao.getAllVisibleApps()
    ) { prefs, workspaces, apps ->
        HomeUiState(
            preferences = prefs,
            workspaces = workspaces,
            totalAppsIndexed = apps.size,
            isLoading = false
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
}
