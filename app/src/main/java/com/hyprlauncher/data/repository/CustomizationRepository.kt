package com.hyprlauncher.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.SearchConfig
import com.hyprlauncher.domain.model.TypographyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface CustomizationRepository {
    val customizationConfig: Flow<CustomizationConfig>

    suspend fun updateLayoutConfig(config: LayoutConfig)
    suspend fun updateDockConfig(config: DockConfig)
    suspend fun updateAppGridConfig(config: AppGridConfig)
    suspend fun updateSearchConfig(config: SearchConfig)
    suspend fun updateTypographyConfig(config: TypographyConfig)
    suspend fun updateIconConfig(config: IconConfig)

    suspend fun pinAppToDock(packageName: String)
    suspend fun unpinAppFromDock(packageName: String)
    suspend fun resetToDefaults()
}

@Singleton
class DefaultCustomizationRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : CustomizationRepository {

    companion object {
        val KEY_CUSTOMIZATION_CONFIG = stringPreferencesKey("customization_config_json")
    }

    override val customizationConfig: Flow<CustomizationConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val json = prefs[KEY_CUSTOMIZATION_CONFIG]
            CustomizationConfig.fromJson(json)
        }

    override suspend fun updateLayoutConfig(config: LayoutConfig) {
        val current = customizationConfig.first()
        val updated = current.copy(layout = config)
        saveConfig(updated)
    }

    override suspend fun updateDockConfig(config: DockConfig) {
        val current = customizationConfig.first()
        val updated = current.copy(dock = config)
        saveConfig(updated)
    }

    override suspend fun updateAppGridConfig(config: AppGridConfig) {
        val current = customizationConfig.first()
        val updated = current.copy(grid = config)
        saveConfig(updated)
    }

    override suspend fun updateSearchConfig(config: SearchConfig) {
        val current = customizationConfig.first()
        val updated = current.copy(search = config)
        saveConfig(updated)
    }

    override suspend fun updateTypographyConfig(config: TypographyConfig) {
        val current = customizationConfig.first()
        val updated = current.copy(typography = config)
        saveConfig(updated)
    }

    override suspend fun updateIconConfig(config: IconConfig) {
        val current = customizationConfig.first()
        val updated = current.copy(icons = config)
        saveConfig(updated)
    }

    override suspend fun pinAppToDock(packageName: String) {
        if (packageName.isBlank()) return
        val current = customizationConfig.first()
        val dock = current.dock
        if (!dock.pinnedPackages.contains(packageName) && dock.pinnedPackages.size < dock.maxItems) {
            val updatedDock = dock.copy(pinnedPackages = dock.pinnedPackages + packageName)
            saveConfig(current.copy(dock = updatedDock))
        }
    }

    override suspend fun unpinAppFromDock(packageName: String) {
        val current = customizationConfig.first()
        val dock = current.dock
        val updatedDock = dock.copy(pinnedPackages = dock.pinnedPackages.filter { it != packageName })
        saveConfig(current.copy(dock = updatedDock))
    }

    override suspend fun resetToDefaults() {
        saveConfig(CustomizationConfig())
    }

    private suspend fun saveConfig(config: CustomizationConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOMIZATION_CONFIG] = config.toJson()
        }
    }
}
