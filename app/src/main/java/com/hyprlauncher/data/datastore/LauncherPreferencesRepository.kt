package com.hyprlauncher.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hyprlauncher.domain.model.PerformanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface LauncherPreferencesRepository {
    val preferences: Flow<LauncherPreferences>
    suspend fun updateActiveWorkspace(workspaceId: Int)
    suspend fun updatePerformanceMode(mode: PerformanceMode)
    suspend fun updateThemeId(themeId: String)
    suspend fun updateGridLayout(columns: Int, rows: Int)
    suspend fun updateDockVisibility(show: Boolean)
    suspend fun updateClockFormat(is24Hour: Boolean, showSeconds: Boolean)
}

@Singleton
class DefaultLauncherPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : LauncherPreferencesRepository {

    private object PreferencesKeys {
        val ACTIVE_WORKSPACE_ID = intPreferencesKey("active_workspace_id")
        val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")
        val THEME_ID = stringPreferencesKey("theme_id")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val SHOW_DOCK = booleanPreferencesKey("show_dock")
        val CLOCK_24_HOUR = booleanPreferencesKey("clock_24_hour")
        val SHOW_CLOCK_SECONDS = booleanPreferencesKey("show_clock_seconds")
        val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
    }

    override val preferences: Flow<LauncherPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val workspaceId = prefs[PreferencesKeys.ACTIVE_WORKSPACE_ID] ?: 1
            val modeString = prefs[PreferencesKeys.PERFORMANCE_MODE] ?: PerformanceMode.BALANCED.name
            val mode = runCatching { PerformanceMode.valueOf(modeString) }.getOrDefault(PerformanceMode.BALANCED)
            val theme = prefs[PreferencesKeys.THEME_ID] ?: "arch_dark"
            val cols = prefs[PreferencesKeys.GRID_COLUMNS] ?: 4
            val rows = prefs[PreferencesKeys.GRID_ROWS] ?: 5
            val dock = prefs[PreferencesKeys.SHOW_DOCK] ?: true
            val is24H = prefs[PreferencesKeys.CLOCK_24_HOUR] ?: true
            val seconds = prefs[PreferencesKeys.SHOW_CLOCK_SECONDS] ?: false
            val blur = prefs[PreferencesKeys.ENABLE_BLUR] ?: true

            LauncherPreferences(
                activeWorkspaceId = workspaceId,
                performanceMode = mode,
                themeId = theme,
                gridColumns = cols,
                gridRows = rows,
                showDock = dock,
                clock24Hour = is24H,
                showClockSeconds = seconds,
                enableBlur = blur
            )
        }

    override suspend fun updateActiveWorkspace(workspaceId: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.ACTIVE_WORKSPACE_ID] = workspaceId
        }
    }

    override suspend fun updatePerformanceMode(mode: PerformanceMode) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.PERFORMANCE_MODE] = mode.name
            // Maximum performance turns off blur
            if (mode == PerformanceMode.MAXIMUM_PERFORMANCE) {
                prefs[PreferencesKeys.ENABLE_BLUR] = false
            }
        }
    }

    override suspend fun updateThemeId(themeId: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_ID] = themeId
        }
    }

    override suspend fun updateGridLayout(columns: Int, rows: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.GRID_COLUMNS] = columns.coerceIn(2, 8)
            prefs[PreferencesKeys.GRID_ROWS] = rows.coerceIn(2, 10)
        }
    }

    override suspend fun updateDockVisibility(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_DOCK] = show
        }
    }

    override suspend fun updateClockFormat(is24Hour: Boolean, showSeconds: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.CLOCK_24_HOUR] = is24Hour
            prefs[PreferencesKeys.SHOW_CLOCK_SECONDS] = showSeconds
        }
    }
}
