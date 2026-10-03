package com.hyprlauncher.feature.home

import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hyprlauncher.core.gesture.GestureActionExecutor
import com.hyprlauncher.core.gesture.GestureRepository
import com.hyprlauncher.core.gesture.GestureType
import com.hyprlauncher.core.gesture.LauncherAction
import com.hyprlauncher.core.platform.LaunchResult
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.datastore.LauncherPreferences
import com.hyprlauncher.data.datastore.LauncherPreferencesRepository
import com.hyprlauncher.data.repository.AppRepository
import com.hyprlauncher.data.repository.WorkspaceRepository
import com.hyprlauncher.domain.model.Workspace
import com.hyprlauncher.domain.model.WorkspaceLayoutConfig
import com.hyprlauncher.domain.usecase.GetLauncherRoleStatusUseCase
import com.hyprlauncher.domain.usecase.LaunchAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val preferences: LauncherPreferences = LauncherPreferences(),
    val workspaces: List<Workspace> = emptyList(),
    val activeWorkspace: Workspace? = null,
    val apps: List<AppEntity> = emptyList(),
    val dockApps: List<AppEntity> = emptyList(),
    val filteredApps: List<AppEntity> = emptyList(),
    val gestureBindings: Map<GestureType, LauncherAction> = emptyMap(),
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
    private val workspaceRepository: WorkspaceRepository,
    private val appRepository: AppRepository,
    private val launchAppUseCase: LaunchAppUseCase,
    private val getLauncherRoleStatusUseCase: GetLauncherRoleStatusUseCase,
    private val gestureRepository: GestureRepository,
    private val gestureActionExecutor: GestureActionExecutor
) : ViewModel() {

    private data class CoreData(
        val preferences: LauncherPreferences,
        val workspaces: List<Workspace>,
        val apps: List<AppEntity>,
        val gestureBindings: Map<GestureType, LauncherAction>
    )

    private val roleStatusFlow = MutableStateFlow(getLauncherRoleStatusUseCase())
    private val lastLaunchResult = MutableStateFlow<LaunchResult?>(null)
    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Seed default workspaces if none exist (PRD Section 17)
        viewModelScope.launch {
            workspaceRepository.ensureDefaultWorkspaces()
        }

        // Perform initial application discovery & indexing via AppRepository
        viewModelScope.launch {
            appRepository.syncAllApps()
        }
    }

    private val coreDataFlow = combine(
        preferencesRepository.preferences,
        workspaceRepository.allWorkspaces,
        appRepository.allApps,
        gestureRepository.gestureBindings
    ) { prefs, workspaces, apps, gestures ->
        CoreData(prefs, workspaces, apps, gestures)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreDataFlow,
        searchQueryFlow,
        roleStatusFlow,
        lastLaunchResult
    ) { coreData, query, roleStatus, launchResult ->
        val apps = coreData.apps
        val prefs = coreData.preferences
        val activeWs = coreData.workspaces.firstOrNull { it.id == prefs.activeWorkspaceId }
            ?: coreData.workspaces.firstOrNull()

        // Per-workspace assigned apps filter (PRD Section 17)
        val workspaceApps = if (activeWs != null && activeWs.layoutConfig.assignedPackageNames.isNotEmpty()) {
            val assignedSet = activeWs.layoutConfig.assignedPackageNames.toSet()
            apps.filter { it.packageName in assignedSet }
        } else {
            apps
        }

        val filtered = if (query.isNotBlank()) {
            workspaceApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        } else {
            workspaceApps
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
            activeWorkspace = activeWs,
            apps = apps,
            dockApps = dockApps,
            filteredApps = filtered,
            gestureBindings = coreData.gestureBindings,
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

    fun onGestureTriggered(gesture: GestureType, onOpenDrawer: () -> Unit) {
        viewModelScope.launch {
            val bindings = gestureRepository.gestureBindings.first()
            val action = bindings[gesture] ?: LauncherAction.None
            val currentWs = uiState.value.preferences.activeWorkspaceId
            val allWs = uiState.value.workspaces

            gestureActionExecutor.execute(
                action = action,
                onOpenDrawer = onOpenDrawer,
                onNextWorkspace = {
                    if (allWs.isNotEmpty()) {
                        val nextWs = allWs.firstOrNull { it.id > currentWs }?.id ?: allWs.first().id
                        onWorkspaceSelected(nextWs)
                    }
                },
                onPreviousWorkspace = {
                    if (allWs.isNotEmpty()) {
                        val prevWs = allWs.lastOrNull { it.id < currentWs }?.id ?: allWs.last().id
                        onWorkspaceSelected(prevWs)
                    }
                }
            )
        }
    }

    fun setGestureBinding(gesture: GestureType, action: LauncherAction) {
        viewModelScope.launch {
            gestureRepository.setBinding(gesture, action)
        }
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

    fun createWorkspace(name: String) {
        viewModelScope.launch {
            val created = workspaceRepository.createWorkspace(name)
            preferencesRepository.updateActiveWorkspace(created.id)
        }
    }

    fun renameWorkspace(id: Int, newName: String) {
        viewModelScope.launch {
            val existing = workspaceRepository.getWorkspaceById(id) ?: return@launch
            workspaceRepository.updateWorkspace(existing.copy(name = newName))
        }
    }

    fun deleteWorkspace(id: Int) {
        viewModelScope.launch {
            val all = uiState.value.workspaces
            if (all.size <= 1) return@launch
            val wasDeleted = workspaceRepository.deleteWorkspace(id)
            if (wasDeleted && uiState.value.preferences.activeWorkspaceId == id) {
                val fallback = all.firstOrNull { it.id != id }?.id ?: 1
                preferencesRepository.updateActiveWorkspace(fallback)
            }
        }
    }

    fun reorderWorkspaces(workspaceIds: List<Int>) {
        viewModelScope.launch {
            workspaceRepository.reorderWorkspaces(workspaceIds)
        }
    }

    fun updateWorkspaceLayout(workspaceId: Int, config: WorkspaceLayoutConfig) {
        viewModelScope.launch {
            workspaceRepository.updateWorkspaceLayout(workspaceId, config)
        }
    }

    fun assignAppToWorkspace(workspaceId: Int, packageName: String) {
        viewModelScope.launch {
            val ws = workspaceRepository.getWorkspaceById(workspaceId) ?: return@launch
            val current = ws.layoutConfig.assignedPackageNames
            if (packageName !in current) {
                val updatedConfig = ws.layoutConfig.copy(
                    assignedPackageNames = current + packageName
                )
                workspaceRepository.updateWorkspaceLayout(workspaceId, updatedConfig)
            }
        }
    }

    fun removeAppFromWorkspace(workspaceId: Int, packageName: String) {
        viewModelScope.launch {
            val ws = workspaceRepository.getWorkspaceById(workspaceId) ?: return@launch
            val current = ws.layoutConfig.assignedPackageNames
            if (packageName in current) {
                val updatedConfig = ws.layoutConfig.copy(
                    assignedPackageNames = current - packageName
                )
                workspaceRepository.updateWorkspaceLayout(workspaceId, updatedConfig)
            }
        }
    }
}
