package com.hyprlauncher.domain.model

import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RiceProfileTest {

    @Test
    fun validProfilePassesValidation() {
        val profile = RiceProfile(
            name = "Test Rice",
            version = 1
        )
        val result = profile.validate()
        assertTrue(result is RiceValidationResult.Valid)
    }

    @Test
    fun blankNameFailsValidation() {
        val profile = RiceProfile(
            name = "   ",
            version = 1
        )
        val result = profile.validate()
        assertTrue(result is RiceValidationResult.Invalid)
        val invalid = result as RiceValidationResult.Invalid
        assertTrue(invalid.errorMessage.contains("name cannot be blank"))
    }

    @Test
    fun invalidVersionFailsValidation() {
        val profile = RiceProfile(
            name = "Invalid Version",
            version = 0
        )
        val result = profile.validate()
        assertTrue(result is RiceValidationResult.Invalid)
        val invalid = result as RiceValidationResult.Invalid
        assertTrue(invalid.errorMessage.contains("Invalid schema version"))
    }

    @Test
    fun invalidGridColumnsAndRowsFailValidation() {
        val invalidGrid = AppGridConfig(columns = 1, rows = 15)
        val profile = RiceProfile(
            name = "Bad Grid",
            customizationConfig = CustomizationConfig(grid = invalidGrid)
        )
        val result = profile.validate()
        assertTrue(result is RiceValidationResult.Invalid)
        val invalid = result as RiceValidationResult.Invalid
        assertTrue(invalid.errorMessage.contains("Grid columns must be between 2 and 8"))
        assertTrue(invalid.errorMessage.contains("Grid rows must be between 2 and 12"))
    }

    @Test
    fun invalidDockIconSizeFailsValidation() {
        val invalidDock = DockConfig(iconSizeDp = 120)
        val profile = RiceProfile(
            name = "Bad Dock",
            customizationConfig = CustomizationConfig(dock = invalidDock)
        )
        val result = profile.validate()
        assertTrue(result is RiceValidationResult.Invalid)
        val invalid = result as RiceValidationResult.Invalid
        assertTrue(invalid.errorMessage.contains("Dock icon size must be between 24dp and 96dp"))
    }

    @Test
    fun invalidFontScaleFailsValidation() {
        val invalidType = TypographyConfig(fontScale = 3.5f)
        val profile = RiceProfile(
            name = "Bad Typography",
            customizationConfig = CustomizationConfig(typography = invalidType)
        )
        val result = profile.validate()
        assertTrue(result is RiceValidationResult.Invalid)
        val invalid = result as RiceValidationResult.Invalid
        assertTrue(invalid.errorMessage.contains("Font scale must be between 0.5x and 2.5x"))
    }

    @Test
    fun jsonSerializationRoundtrip() {
        val original = RiceProfile(
            id = "rice-tokyo-test",
            name = "Tokyo Ricing",
            version = 1,
            themeConfig = ThemeConfig(presetId = ThemePresetId.TOKYO_NIGHT, cornerRadiusDp = 12),
            customizationConfig = CustomizationConfig(
                grid = AppGridConfig(columns = 5, rows = 6),
                dock = DockConfig(showLabels = false, iconSizeDp = 48)
            ),
            wallpaperUri = "content://wallpaper/test.jpg"
        )

        val json = original.toJson()
        val parsed = RiceProfile.fromJson(json).getOrThrow()

        assertEquals(original.id, parsed.id)
        assertEquals(original.name, parsed.name)
        assertEquals(original.version, parsed.version)
        assertEquals(original.themeConfig.presetId, parsed.themeConfig.presetId)
        assertEquals(original.themeConfig.cornerRadiusDp, parsed.themeConfig.cornerRadiusDp)
        assertEquals(original.customizationConfig.grid.columns, parsed.customizationConfig.grid.columns)
        assertEquals(original.customizationConfig.grid.rows, parsed.customizationConfig.grid.rows)
        assertEquals(original.customizationConfig.dock.showLabels, parsed.customizationConfig.dock.showLabels)
        assertEquals(original.customizationConfig.dock.iconSizeDp, parsed.customizationConfig.dock.iconSizeDp)
        assertEquals(original.wallpaperUri, parsed.wallpaperUri)
    }

    @Test
    fun fromJsonWithNullOrBlankFails() {
        assertTrue(RiceProfile.fromJson(null).isFailure)
        assertTrue(RiceProfile.fromJson("   ").isFailure)
    }

    @Test
    fun fromJsonWithMalformedJsonFails() {
        assertTrue(RiceProfile.fromJson("not-a-json").isFailure)
    }

    @Test
    fun schemaMigrationFromVersion0AddsMissingConfigsAndSetsVersion1() {
        val legacyJson = JSONObject().apply {
            put("id", "legacy-profile-id")
            put("name", "Legacy Rice")
            put("version", 0)
            // themeConfig and customizationConfig are omitted in legacy version 0
        }

        val migrated = RiceProfile.migrate(legacyJson, fromVersion = 0, targetVersion = 1)
        assertEquals(1, migrated.getInt("version"))
        assertTrue(migrated.has("themeConfig"))
        assertTrue(migrated.has("customizationConfig"))

        // When imported through fromJson, migration is executed automatically
        val parsed = RiceProfile.fromJson(legacyJson.toString()).getOrThrow()
        assertEquals(1, parsed.version)
        assertEquals("Legacy Rice", parsed.name)
        assertNotNull(parsed.themeConfig)
        assertNotNull(parsed.customizationConfig)
    }

    @Test
    fun riceDraftConversionAndDirtyFlag() {
        val profile = RiceProfile(
            id = "rice-draft-test",
            name = "Original Name",
            themeConfig = ThemeConfig(presetId = ThemePresetId.CATPPUCCIN)
        )

        val draft = RiceDraft.fromProfile(profile)
        assertEquals(profile.id, draft.profileId)
        assertEquals("Original Name", draft.name)
        assertEquals(ThemePresetId.CATPPUCCIN, draft.themeConfig.presetId)
        assertFalse(draft.isDirty)

        val modifiedDraft = draft.copy(name = "Modified Name", isDirty = true)
        assertTrue(modifiedDraft.isDirty)

        val convertedProfile = modifiedDraft.toProfile()
        assertEquals(profile.id, convertedProfile.id)
        assertEquals("Modified Name", convertedProfile.name)
        assertEquals(ThemePresetId.CATPPUCCIN, convertedProfile.themeConfig.presetId)
    }
}
