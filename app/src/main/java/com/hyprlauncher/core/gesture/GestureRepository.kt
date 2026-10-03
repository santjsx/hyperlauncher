package com.hyprlauncher.core.gesture

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface GestureRepository {
    val gestureBindings: Flow<Map<GestureType, LauncherAction>>
    suspend fun setBinding(gesture: GestureType, action: LauncherAction)
    suspend fun resetDefaults()
}

@Singleton
class DefaultGestureRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : GestureRepository {

    companion object {
        val defaultBindings: Map<GestureType, LauncherAction> = mapOf(
            GestureType.SWIPE_UP to LauncherAction.OpenAppDrawer,
            GestureType.SWIPE_DOWN to LauncherAction.OpenNotificationShade,
            GestureType.SWIPE_LEFT to LauncherAction.NextWorkspace,
            GestureType.SWIPE_RIGHT to LauncherAction.PreviousWorkspace,
            GestureType.DOUBLE_TAP to LauncherAction.OpenCommandPalette,
            GestureType.LONG_PRESS to LauncherAction.OpenSettings
        )
    }

    private fun preferenceKeyFor(gesture: GestureType) =
        stringPreferencesKey("gesture_${gesture.name.lowercase()}")

    override val gestureBindings: Flow<Map<GestureType, LauncherAction>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val result = mutableMapOf<GestureType, LauncherAction>()
            for (gesture in GestureType.entries) {
                val stored = prefs[preferenceKeyFor(gesture)]
                val action = if (stored != null) {
                    LauncherAction.deserialize(stored)
                } else {
                    defaultBindings[gesture] ?: LauncherAction.None
                }
                result[gesture] = action
            }
            result
        }

    override suspend fun setBinding(gesture: GestureType, action: LauncherAction) {
        dataStore.edit { prefs ->
            prefs[preferenceKeyFor(gesture)] = action.serialize()
        }
    }

    override suspend fun resetDefaults() {
        dataStore.edit { prefs ->
            for (gesture in GestureType.entries) {
                prefs.remove(preferenceKeyFor(gesture))
            }
        }
    }
}
