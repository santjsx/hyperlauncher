package com.hyprlauncher.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hyprlauncher.core.designsystem.theme.HyprColors
import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.core.designsystem.theme.ThemePresets
import com.hyprlauncher.domain.model.AnimationScale
import com.hyprlauncher.domain.model.ThemeConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ThemeRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: ThemeRepository

    @Before
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("theme_test.preferences_pb") }
        )
        repository = DefaultThemeRepository(dataStore)
    }

    @Test
    fun defaultThemeConfigIsArchDark() = runTest(testDispatcher) {
        val config = repository.themeConfig.first()
        assertEquals(ThemePresetId.ARCH_DARK, config.presetId)
        assertEquals(8, config.cornerRadiusDp)
        assertEquals(1.0f, config.borderWidthDp)
        assertEquals(1.0f, config.fontScale)
        assertEquals(false, config.useMonospaceAll)
        assertEquals(AnimationScale.FULL, config.animationScale)
    }

    @Test
    fun selectPresetUpdatesConfigAndEmitsMatchingColors() = runTest(testDispatcher) {
        // Select Tokyo Night
        repository.selectPreset(ThemePresetId.TOKYO_NIGHT)
        val config = repository.themeConfig.first()
        assertEquals(ThemePresetId.TOKYO_NIGHT, config.presetId)

        val colors = repository.currentColors.first()
        assertEquals(ThemePresets.TokyoNight.accent, colors.accent)
        assertEquals(ThemePresets.TokyoNight.background, colors.background)

        // Select AMOLED
        repository.selectPreset(ThemePresetId.AMOLED)
        val amoledColors = repository.currentColors.first()
        assertEquals(Color(0xFF000000), amoledColors.background)

        // Select Catppuccin
        repository.selectPreset(ThemePresetId.CATPPUCCIN)
        val catppuccinColors = repository.currentColors.first()
        assertEquals(ThemePresets.Catppuccin.accent, catppuccinColors.accent)
    }

    @Test
    fun allPresetPalettesAreDefinedAndDistinct() {
        val presets = ThemePresetId.entries.filter { it != ThemePresetId.CUSTOM }
        assertEquals(9, presets.size)

        for (preset in presets) {
            val colors = ThemePresets.forPreset(preset)
            assertNotNull(colors)
            assertNotNull(colors.accent)
            assertNotNull(colors.background)
        }
    }

    @Test
    fun updateCornerRadiusUpdatesShapes() = runTest(testDispatcher) {
        repository.updateCornerRadius(16)
        val config = repository.themeConfig.first()
        assertEquals(16, config.cornerRadiusDp)

        val shapes = repository.currentShapes.first()
        assertNotNull(shapes)
    }

    @Test
    fun updateAnimationScalePersists() = runTest(testDispatcher) {
        repository.updateAnimationScale(AnimationScale.REDUCED)
        val config = repository.themeConfig.first()
        assertEquals(AnimationScale.REDUCED, config.animationScale)
        assertEquals(0.5f, config.animationScale.multiplier)

        repository.updateAnimationScale(AnimationScale.DISABLED)
        val disabledConfig = repository.themeConfig.first()
        assertEquals(AnimationScale.DISABLED, disabledConfig.animationScale)
        assertEquals(0.0f, disabledConfig.animationScale.multiplier)
    }

    @Test
    fun updateFontScaleAndMonospaceUpdatesTypography() = runTest(testDispatcher) {
        repository.updateFontScale(1.2f)
        repository.updateUseMonospaceAll(true)

        val config = repository.themeConfig.first()
        assertEquals(1.2f, config.fontScale)
        assertEquals(true, config.useMonospaceAll)

        val typography = repository.currentTypography.first()
        assertEquals(FontFamily.Monospace, typography.headline.fontFamily)
        assertEquals(FontFamily.Monospace, typography.bodyLarge.fontFamily)
    }

    @Test
    fun themeConfigJsonSerializationRoundtrip() {
        val config = ThemeConfig(
            presetId = ThemePresetId.DRACULA,
            customColors = null,
            cornerRadiusDp = 12,
            borderWidthDp = 1.5f,
            fontScale = 1.1f,
            useMonospaceAll = true,
            animationScale = AnimationScale.MINIMAL
        )

        val json = config.toJson()
        val deserialized = ThemeConfig.fromJson(json)

        assertEquals(config.presetId, deserialized.presetId)
        assertEquals(config.cornerRadiusDp, deserialized.cornerRadiusDp)
        assertEquals(config.borderWidthDp, deserialized.borderWidthDp)
        assertEquals(config.fontScale, deserialized.fontScale)
        assertEquals(config.useMonospaceAll, deserialized.useMonospaceAll)
        assertEquals(config.animationScale, deserialized.animationScale)
    }
}
