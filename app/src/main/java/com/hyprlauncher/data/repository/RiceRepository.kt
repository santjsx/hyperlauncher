package com.hyprlauncher.data.repository

import com.hyprlauncher.core.designsystem.theme.ThemePresetId
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.database.entity.RiceEntity
import com.hyprlauncher.domain.model.AppGridConfig
import com.hyprlauncher.domain.model.CustomizationConfig
import com.hyprlauncher.domain.model.DockConfig
import com.hyprlauncher.domain.model.FontFamilyPreference
import com.hyprlauncher.domain.model.IconConfig
import com.hyprlauncher.domain.model.IconShape
import com.hyprlauncher.domain.model.IconTint
import com.hyprlauncher.domain.model.LayoutConfig
import com.hyprlauncher.domain.model.RiceDraft
import com.hyprlauncher.domain.model.RiceProfile
import com.hyprlauncher.domain.model.RiceValidationResult
import com.hyprlauncher.domain.model.ThemeConfig
import com.hyprlauncher.domain.model.TypographyConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface RiceRepository {
    val allRices: Flow<List<RiceProfile>>
    val activeRice: Flow<RiceProfile?>

    suspend fun ensureDefaultRices()
    suspend fun getRiceById(id: String): RiceProfile?
    suspend fun saveRice(profile: RiceProfile): Result<Unit>
    suspend fun duplicateRice(id: String, newName: String): Result<RiceProfile>
    suspend fun renameRice(id: String, newName: String): Result<Unit>
    suspend fun deleteRice(id: String): Result<Unit>
    suspend fun applyRice(id: String): Result<Unit>
    suspend fun applyDraft(draft: RiceDraft): Result<Unit>
    fun exportRice(profile: RiceProfile): String
    suspend fun importRice(jsonString: String): Result<RiceProfile>
}

@Singleton
class DefaultRiceRepository @Inject constructor(
    private val riceDao: RiceDao,
    private val themeRepository: ThemeRepository,
    private val customizationRepository: CustomizationRepository
) : RiceRepository {

    override val allRices: Flow<List<RiceProfile>> = riceDao.getAllRices().map { entities ->
        entities.mapNotNull { entity ->
            RiceProfile.fromJson(entity.jsonPayload).getOrNull()
        }
    }

    override val activeRice: Flow<RiceProfile?> = riceDao.getActiveRice().map { entity ->
        entity?.let { RiceProfile.fromJson(it.jsonPayload).getOrNull() }
    }

    override suspend fun ensureDefaultRices() {
        if (riceDao.getRiceCount() > 0) return

        val defaultRices = listOf(
            RiceProfile(
                id = "arch_dark_default",
                name = "Arch Dark",
                themeConfig = ThemeConfig(presetId = ThemePresetId.ARCH_DARK),
                customizationConfig = CustomizationConfig(
                    layout = LayoutConfig(showWaybar = true, showClock = true, showDock = true),
                    grid = AppGridConfig(columns = 4, iconSizeDp = 36),
                    dock = DockConfig(iconSizeDp = 40, cornerRadiusDp = 16),
                    typography = TypographyConfig(fontFamily = FontFamilyPreference.SANS_SERIF)
                )
            ),
            RiceProfile(
                id = "hyprland_neon",
                name = "Hyprland Neon",
                themeConfig = ThemeConfig(presetId = ThemePresetId.HYPRLAND, cornerRadiusDp = 14),
                customizationConfig = CustomizationConfig(
                    layout = LayoutConfig(showWaybar = true, showClock = true, showDock = true),
                    grid = AppGridConfig(columns = 5, iconSizeDp = 40),
                    dock = DockConfig(iconSizeDp = 44, cornerRadiusDp = 20, backgroundAlpha = 0.9f),
                    icons = IconConfig(shape = IconShape.SQUIRCLE, tint = IconTint.THEME_ACCENT)
                )
            ),
            RiceProfile(
                id = "tokyo_night_power",
                name = "Tokyo Night",
                themeConfig = ThemeConfig(presetId = ThemePresetId.TOKYO_NIGHT),
                customizationConfig = CustomizationConfig(
                    layout = LayoutConfig(showWaybar = true, showClock = true, showDock = true),
                    grid = AppGridConfig(columns = 4, iconSizeDp = 38),
                    dock = DockConfig(iconSizeDp = 40, cornerRadiusDp = 16, backgroundAlpha = 0.85f),
                    icons = IconConfig(shape = IconShape.ROUNDED_SQUARE)
                )
            ),
            RiceProfile(
                id = "catppuccin_cozy",
                name = "Catppuccin Macchiato",
                themeConfig = ThemeConfig(presetId = ThemePresetId.CATPPUCCIN, cornerRadiusDp = 18),
                customizationConfig = CustomizationConfig(
                    layout = LayoutConfig(showWaybar = true, showClock = true, showDock = true),
                    grid = AppGridConfig(columns = 4, iconSizeDp = 42, spacingHorizontalDp = 12),
                    dock = DockConfig(iconSizeDp = 44, cornerRadiusDp = 22),
                    icons = IconConfig(shape = IconShape.CIRCLE)
                )
            ),
            RiceProfile(
                id = "nord_minimal",
                name = "Nord Minimal",
                themeConfig = ThemeConfig(presetId = ThemePresetId.NORD),
                customizationConfig = CustomizationConfig(
                    layout = LayoutConfig(showWaybar = true, showClock = true, showDock = false),
                    grid = AppGridConfig(columns = 3, iconSizeDp = 48),
                    typography = TypographyConfig(fontFamily = FontFamilyPreference.MONOSPACE, useMonospaceForStatusOnly = false)
                )
            )
        )

        defaultRices.forEachIndexed { index, profile ->
            val entity = RiceEntity(
                id = profile.id,
                name = profile.name,
                version = profile.version,
                isActive = index == 0,
                jsonPayload = profile.toJson(),
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
            riceDao.insertOrUpdate(entity)
        }
    }

    override suspend fun getRiceById(id: String): RiceProfile? {
        val entity = riceDao.getRiceById(id) ?: return null
        return RiceProfile.fromJson(entity.jsonPayload).getOrNull()
    }

    override suspend fun saveRice(profile: RiceProfile): Result<Unit> {
        when (val validation = profile.validate()) {
            is RiceValidationResult.Invalid -> {
                return Result.failure(IllegalArgumentException(validation.errorMessage))
            }
            RiceValidationResult.Valid -> {}
        }

        val existing = riceDao.getRiceById(profile.id)
        val entity = RiceEntity(
            id = profile.id,
            name = profile.name,
            version = profile.version,
            isActive = existing?.isActive ?: false,
            jsonPayload = profile.toJson(),
            createdAt = existing?.createdAt ?: profile.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        riceDao.insertOrUpdate(entity)
        return Result.success(Unit)
    }

    override suspend fun duplicateRice(id: String, newName: String): Result<RiceProfile> {
        val existing = getRiceById(id)
            ?: return Result.failure(IllegalArgumentException("Rice profile with id $id not found"))

        val duplicated = existing.copy(
            id = UUID.randomUUID().toString(),
            name = newName.ifBlank { "${existing.name} (Copy)" },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return saveRice(duplicated).map { duplicated }
    }

    override suspend fun renameRice(id: String, newName: String): Result<Unit> {
        if (newName.isBlank()) {
            return Result.failure(IllegalArgumentException("Profile name cannot be blank"))
        }

        val existing = getRiceById(id)
            ?: return Result.failure(IllegalArgumentException("Rice profile with id $id not found"))

        val renamed = existing.copy(name = newName, updatedAt = System.currentTimeMillis())
        return saveRice(renamed)
    }

    override suspend fun deleteRice(id: String): Result<Unit> {
        val count = riceDao.getRiceCount()
        if (count <= 1) {
            return Result.failure(IllegalStateException("Cannot delete the only remaining rice profile"))
        }

        val existing = riceDao.getRiceById(id)
            ?: return Result.failure(IllegalArgumentException("Rice profile with id $id not found"))

        if (existing.isActive) {
            // Find another rice to set active before deleting
            val fallback = riceDao.getRiceById("arch_dark_default") ?: return Result.failure(
                IllegalStateException("Cannot delete active profile without a fallback")
            )
            riceDao.setActiveRice(fallback.id, System.currentTimeMillis())
        }

        riceDao.deleteRice(id)
        return Result.success(Unit)
    }

    override suspend fun applyRice(id: String): Result<Unit> {
        val profile = getRiceById(id)
            ?: return Result.failure(IllegalArgumentException("Rice profile with id $id not found"))

        // Apply Theme properties
        val theme = profile.themeConfig
        themeRepository.selectPreset(theme.presetId)
        themeRepository.updateAnimationScale(theme.animationScale)
        themeRepository.updateCornerRadius(theme.cornerRadiusDp)
        themeRepository.updateBorderWidth(theme.borderWidthDp)
        themeRepository.updateFontScale(theme.fontScale)
        themeRepository.updateUseMonospaceAll(theme.useMonospaceAll)

        // Apply Customization properties
        val custom = profile.customizationConfig
        customizationRepository.updateLayoutConfig(custom.layout)
        customizationRepository.updateDockConfig(custom.dock)
        customizationRepository.updateAppGridConfig(custom.grid)
        customizationRepository.updateSearchConfig(custom.search)
        customizationRepository.updateTypographyConfig(custom.typography)
        customizationRepository.updateIconConfig(custom.icons)

        // Update active flag in Room
        riceDao.clearActiveStatus()
        riceDao.setActiveRice(profile.id, System.currentTimeMillis())

        return Result.success(Unit)
    }

    override suspend fun applyDraft(draft: RiceDraft): Result<Unit> {
        // Apply Draft directly to active launcher state (PRD Section 23 Live Apply)
        val theme = draft.themeConfig
        themeRepository.selectPreset(theme.presetId)
        themeRepository.updateAnimationScale(theme.animationScale)
        themeRepository.updateCornerRadius(theme.cornerRadiusDp)
        themeRepository.updateBorderWidth(theme.borderWidthDp)
        themeRepository.updateFontScale(theme.fontScale)
        themeRepository.updateUseMonospaceAll(theme.useMonospaceAll)

        val custom = draft.customizationConfig
        customizationRepository.updateLayoutConfig(custom.layout)
        customizationRepository.updateDockConfig(custom.dock)
        customizationRepository.updateAppGridConfig(custom.grid)
        customizationRepository.updateSearchConfig(custom.search)
        customizationRepository.updateTypographyConfig(custom.typography)
        customizationRepository.updateIconConfig(custom.icons)

        return Result.success(Unit)
    }

    override fun exportRice(profile: RiceProfile): String {
        return profile.toJson()
    }

    override suspend fun importRice(jsonString: String): Result<RiceProfile> {
        val parsedResult = RiceProfile.fromJson(jsonString)
        if (parsedResult.isFailure) {
            return parsedResult
        }

        val originalProfile = parsedResult.getOrThrow()
        val importedProfile = originalProfile.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val saveResult = saveRice(importedProfile)
        if (saveResult.isFailure) {
            return Result.failure(saveResult.exceptionOrNull() ?: Exception("Failed to persist imported rice"))
        }

        return Result.success(importedProfile)
    }
}
