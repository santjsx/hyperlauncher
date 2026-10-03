package com.hyprlauncher.data.repository

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hyprlauncher.core.designsystem.theme.DefaultHyprTypography
import com.hyprlauncher.core.designsystem.theme.HyprColors
import com.hyprlauncher.core.designsystem.theme.HyprShapes
import com.hyprlauncher.core.designsystem.theme.HyprTypography
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.domain.model.AnimationScale
import com.hyprlauncher.domain.model.ThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeRepository {
    val themeConfig: Flow<ThemeConfig>
    val currentColors: Flow<HyprColors>
    val currentShapes: Flow<HyprShapes>
    val currentTypography: Flow<HyprTypography>

    suspend fun selectPreset(presetId: ThemePresetId)
    suspend fun updateCustomColors(colors: HyprColors)
    suspend fun updateCornerRadius(radiusDp: Int)
    suspend fun updateBorderWidth(widthDp: Float)
    suspend fun updateFontScale(scale: Float)
    suspend fun updateUseMonospaceAll(enable: Boolean)
    suspend fun updateAnimationScale(scale: AnimationScale)
    suspend fun resetDefaults()
}

@Singleton
class DefaultThemeRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {

    companion object {
        val KEY_THEME_CONFIG = stringPreferencesKey("theme_config_json")
    }

    override val themeConfig: Flow<ThemeConfig> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val json = prefs[KEY_THEME_CONFIG]
            ThemeConfig.fromJson(json)
        }

    override val currentColors: Flow<HyprColors> = themeConfig.map { it.resolvedColors }

    override val currentShapes: Flow<HyprShapes> = themeConfig.map { config ->
        val radius = config.cornerRadiusDp.coerceIn(0, 32)
        val border = config.borderWidthDp.coerceIn(0.5f, 4f)
        HyprShapes(
            small = RoundedCornerShape((radius / 2).coerceAtLeast(2).dp),
            medium = RoundedCornerShape(radius.dp),
            large = RoundedCornerShape((radius * 1.5f).toInt().dp),
            pill = RoundedCornerShape(50),
            borderWidth = border.dp
        )
    }

    override val currentTypography: Flow<HyprTypography> = themeConfig.map { config ->
        val scale = config.fontScale.coerceIn(0.75f, 1.35f)
        val base = DefaultHyprTypography
        val sansFamily = if (config.useMonospaceAll) FontFamily.Monospace else FontFamily.SansSerif

        HyprTypography(
            displayLarge = base.displayLarge.copy(
                fontSize = (56 * scale).sp
            ),
            displayMedium = base.displayMedium.copy(
                fontSize = (36 * scale).sp
            ),
            headline = base.headline.copy(
                fontFamily = sansFamily,
                fontSize = (20 * scale).sp
            ),
            bodyLarge = base.bodyLarge.copy(
                fontFamily = sansFamily,
                fontSize = (15 * scale).sp
            ),
            bodyMedium = base.bodyMedium.copy(
                fontFamily = sansFamily,
                fontSize = (13 * scale).sp
            ),
            monospaceLarge = base.monospaceLarge.copy(
                fontSize = (15 * scale).sp
            ),
            monospaceMedium = base.monospaceMedium.copy(
                fontSize = (13 * scale).sp
            ),
            monospaceSmall = base.monospaceSmall.copy(
                fontSize = (11 * scale).sp
            ),
            statusModule = base.statusModule.copy(
                fontSize = (12 * scale).sp
            )
        )
    }

    override suspend fun selectPreset(presetId: ThemePresetId) {
        updateConfig { it.copy(presetId = presetId) }
    }

    override suspend fun updateCustomColors(colors: HyprColors) {
        updateConfig {
            it.copy(
                presetId = ThemePresetId.CUSTOM,
                customColors = colors
            )
        }
    }

    override suspend fun updateCornerRadius(radiusDp: Int) {
        updateConfig { it.copy(cornerRadiusDp = radiusDp.coerceIn(0, 32)) }
    }

    override suspend fun updateBorderWidth(widthDp: Float) {
        updateConfig { it.copy(borderWidthDp = widthDp.coerceIn(0.5f, 4f)) }
    }

    override suspend fun updateFontScale(scale: Float) {
        updateConfig { it.copy(fontScale = scale.coerceIn(0.75f, 1.35f)) }
    }

    override suspend fun updateUseMonospaceAll(enable: Boolean) {
        updateConfig { it.copy(useMonospaceAll = enable) }
    }

    override suspend fun updateAnimationScale(scale: AnimationScale) {
        updateConfig { it.copy(animationScale = scale) }
    }

    override suspend fun resetDefaults() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_THEME_CONFIG)
        }
    }

    private suspend fun updateConfig(transform: (ThemeConfig) -> ThemeConfig) {
        dataStore.edit { prefs ->
            val current = ThemeConfig.fromJson(prefs[KEY_THEME_CONFIG])
            val updated = transform(current)
            prefs[KEY_THEME_CONFIG] = updated.toJson()
        }
    }
}
