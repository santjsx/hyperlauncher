package com.hyprlauncher.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.FontFamilyPreference
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.SearchConfig
import com.hyprlauncher.domain.model.SearchRankingMode
import com.hyprlauncher.domain.model.TypographyConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CustomizationRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: CustomizationRepository

    @Before
    fun setup() {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("customization_test.preferences_pb") }
        )
        repository = DefaultCustomizationRepository(dataStore)
    }

    @Test
    fun defaultCustomizationConfigHasExpectedDefaults() = runTest(testDispatcher) {
        val config = repository.customizationConfig.first()
        assertTrue(config.layout.showWaybar)
        assertTrue(config.layout.showClock)
        assertTrue(config.layout.showDate)
        assertTrue(config.layout.showSearchBar)
        assertTrue(config.layout.showAppGrid)
        assertTrue(config.layout.showDock)

        assertTrue(config.dock.enabled)
        assertEquals(40, config.dock.iconSizeDp)
        assertFalse(config.dock.showLabels)

        assertEquals(4, config.grid.columns)
        assertEquals(5, config.grid.rows)
        assertEquals(36, config.grid.iconSizeDp)
        assertTrue(config.grid.showLabels)

        assertTrue(config.search.fuzzyMatchingEnabled)
        assertEquals(SearchRankingMode.DETERMINISTIC_HYBRID, config.search.rankingMode)

        assertEquals(FontFamilyPreference.SANS_SERIF, config.typography.fontFamily)
        assertEquals(1.0f, config.typography.fontScale, 0.01f)

        assertEquals(IconShape.SYSTEM_DEFAULT, config.icons.shape)
        assertEquals(IconTint.NONE, config.icons.tint)
    }

    @Test
    fun updateLayoutConfigPersistsChanges() = runTest(testDispatcher) {
        val updatedLayout = LayoutConfig(
            showWaybar = false,
            showClock = false,
            showDate = false,
            showSearchBar = false,
            showAppGrid = true,
            showDock = false,
            clockAtTop = false,
            wallpaperDim = 0.6f,
            wallpaperAmoledMode = true
        )
        repository.updateLayoutConfig(updatedLayout)

        val config = repository.customizationConfig.first()
        assertFalse(config.layout.showWaybar)
        assertFalse(config.layout.showClock)
        assertFalse(config.layout.showDock)
        assertTrue(config.layout.wallpaperAmoledMode)
        assertEquals(0.6f, config.layout.wallpaperDim, 0.01f)
    }

    @Test
    fun updateDockConfigPersistsChanges() = runTest(testDispatcher) {
        val updatedDock = DockConfig(
            enabled = false,
            iconSizeDp = 48,
            showLabels = true,
            spacingDp = 16,
            cornerRadiusDp = 24,
            backgroundAlpha = 0.5f,
            pinnedPackages = listOf("org.mozilla.firefox", "com.termux")
        )
        repository.updateDockConfig(updatedDock)

        val config = repository.customizationConfig.first()
        assertFalse(config.dock.enabled)
        assertEquals(48, config.dock.iconSizeDp)
        assertTrue(config.dock.showLabels)
        assertEquals(16, config.dock.spacingDp)
        assertEquals(2, config.dock.pinnedPackages.size)
        assertTrue(config.dock.pinnedPackages.contains("org.mozilla.firefox"))
    }

    @Test
    fun updateAppGridConfigPersistsChanges() = runTest(testDispatcher) {
        val updatedGrid = AppGridConfig(
            columns = 5,
            rows = 6,
            iconSizeDp = 42,
            spacingHorizontalDp = 12,
            spacingVerticalDp = 12,
            showLabels = false,
            labelFontSizeSp = 9
        )
        repository.updateAppGridConfig(updatedGrid)

        val config = repository.customizationConfig.first()
        assertEquals(5, config.grid.columns)
        assertEquals(6, config.grid.rows)
        assertEquals(42, config.grid.iconSizeDp)
        assertFalse(config.grid.showLabels)
        assertEquals(9, config.grid.labelFontSizeSp)
    }

    @Test
    fun updateSearchConfigPersistsChanges() = runTest(testDispatcher) {
        val updatedSearch = SearchConfig(
            fuzzyMatchingEnabled = false,
            prefixMatchingEnabled = true,
            searchHistoryEnabled = false,
            maxHistoryItems = 10,
            placeholderText = "> run",
            showPackageNames = true,
            rankingMode = SearchRankingMode.FREQUENCY_FIRST
        )
        repository.updateSearchConfig(updatedSearch)

        val config = repository.customizationConfig.first()
        assertFalse(config.search.fuzzyMatchingEnabled)
        assertTrue(config.search.showPackageNames)
        assertEquals(SearchRankingMode.FREQUENCY_FIRST, config.search.rankingMode)
        assertEquals("> run", config.search.placeholderText)
    }

    @Test
    fun updateTypographyConfigPersistsChanges() = runTest(testDispatcher) {
        val updatedTypography = TypographyConfig(
            fontFamily = FontFamilyPreference.MONOSPACE,
            fontScale = 1.15f,
            letterSpacingSp = 0.5f,
            useMonospaceForStatusOnly = false
        )
        repository.updateTypographyConfig(updatedTypography)

        val config = repository.customizationConfig.first()
        assertEquals(FontFamilyPreference.MONOSPACE, config.typography.fontFamily)
        assertEquals(1.15f, config.typography.fontScale, 0.01f)
        assertEquals(0.5f, config.typography.letterSpacingSp, 0.01f)
        assertFalse(config.typography.useMonospaceForStatusOnly)
    }

    @Test
    fun updateIconConfigPersistsChanges() = runTest(testDispatcher) {
        val updatedIcons = IconConfig(
            shape = IconShape.SQUIRCLE,
            scale = 1.2f,
            tint = IconTint.THEME_ACCENT,
            showAppLabels = false
        )
        repository.updateIconConfig(updatedIcons)

        val config = repository.customizationConfig.first()
        assertEquals(IconShape.SQUIRCLE, config.icons.shape)
        assertEquals(1.2f, config.icons.scale, 0.01f)
        assertEquals(IconTint.THEME_ACCENT, config.icons.tint)
        assertFalse(config.icons.showAppLabels)
    }

    @Test
    fun pinAndUnpinAppFromDock() = runTest(testDispatcher) {
        repository.pinAppToDock("com.termux")
        repository.pinAppToDock("org.mozilla.firefox")

        var config = repository.customizationConfig.first()
        assertEquals(2, config.dock.pinnedPackages.size)
        assertTrue(config.dock.pinnedPackages.contains("com.termux"))
        assertTrue(config.dock.pinnedPackages.contains("org.mozilla.firefox"))

        // Duplicate pin should not add twice
        repository.pinAppToDock("com.termux")
        config = repository.customizationConfig.first()
        assertEquals(2, config.dock.pinnedPackages.size)

        // Unpin
        repository.unpinAppFromDock("com.termux")
        config = repository.customizationConfig.first()
        assertEquals(1, config.dock.pinnedPackages.size)
        assertFalse(config.dock.pinnedPackages.contains("com.termux"))
        assertTrue(config.dock.pinnedPackages.contains("org.mozilla.firefox"))
    }

    @Test
    fun resetToDefaultsRestoresAllConfigs() = runTest(testDispatcher) {
        repository.updateLayoutConfig(LayoutConfig(showWaybar = false))
        repository.updateDockConfig(DockConfig(iconSizeDp = 56))
        repository.updateIconConfig(IconConfig(shape = IconShape.CIRCLE))

        repository.resetToDefaults()

        val config = repository.customizationConfig.first()
        assertTrue(config.layout.showWaybar)
        assertEquals(40, config.dock.iconSizeDp)
        assertEquals(IconShape.SYSTEM_DEFAULT, config.icons.shape)
    }

    @Test
    fun serializationRoundTripPreservesAllFields() {
        val original = CustomizationConfig(
            layout = LayoutConfig(showClock = false, wallpaperDim = 0.5f),
            dock = DockConfig(iconSizeDp = 48, showLabels = true, pinnedPackages = listOf("a", "b")),
            grid = AppGridConfig(columns = 6, iconSizeDp = 42),
            search = SearchConfig(rankingMode = SearchRankingMode.RECENCY_FIRST),
            typography = TypographyConfig(fontFamily = FontFamilyPreference.SERIF, fontScale = 1.3f),
            icons = IconConfig(shape = IconShape.ROUNDED_SQUARE, tint = IconTint.THEME_PRIMARY)
        )

        val json = original.toJson()
        val deserialized = CustomizationConfig.fromJson(json)

        assertEquals(original.layout.showClock, deserialized.layout.showClock)
        assertEquals(original.layout.wallpaperDim, deserialized.layout.wallpaperDim, 0.01f)
        assertEquals(original.dock.iconSizeDp, deserialized.dock.iconSizeDp)
        assertEquals(original.dock.showLabels, deserialized.dock.showLabels)
        assertEquals(original.dock.pinnedPackages, deserialized.dock.pinnedPackages)
        assertEquals(original.grid.columns, deserialized.grid.columns)
        assertEquals(original.search.rankingMode, deserialized.search.rankingMode)
        assertEquals(original.typography.fontFamily, deserialized.typography.fontFamily)
        assertEquals(original.typography.fontScale, deserialized.typography.fontScale, 0.01f)
        assertEquals(original.icons.shape, deserialized.icons.shape)
        assertEquals(original.icons.tint, deserialized.icons.tint)
    }
}
