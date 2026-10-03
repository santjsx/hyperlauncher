package com.hyprlauncher.domain.model

import org.json.JSONObject
import java.util.UUID

sealed interface RiceValidationResult {
    data object Valid : RiceValidationResult
    data class Invalid(val errors: List<String>) : RiceValidationResult {
        val errorMessage: String get() = errors.joinToString("; ")
    }
}

/**
 * Complete Rice Profile conforming to PRD Section 22 & Phase 9.
 * Represents an entire desktop/mobile ricing snapshot:
 * - Theme & Color Palettes
 * - Declarative Layout
 * - Application Grid & Dock
 * - Typography System
 * - Icon Shape & Tint
 * - Search Behaviors & Animations
 */
data class RiceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val version: Int = CURRENT_SCHEMA_VERSION,
    val themeConfig: ThemeConfig = ThemeConfig(),
    val customizationConfig: CustomizationConfig = CustomizationConfig(),
    val wallpaperUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {

    fun validate(): RiceValidationResult {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors.add("Rice profile name cannot be blank")
        }
        if (version < 1) {
            errors.add("Invalid schema version: $version")
        }

        // Validate App Grid ranges
        val grid = customizationConfig.grid
        if (grid.columns !in 2..8) {
            errors.add("Grid columns must be between 2 and 8 (was ${grid.columns})")
        }
        if (grid.rows !in 2..12) {
            errors.add("Grid rows must be between 2 and 12 (was ${grid.rows})")
        }
        if (grid.iconSizeDp !in 24..96) {
            errors.add("Grid icon size must be between 24dp and 96dp (was ${grid.iconSizeDp})")
        }

        // Validate Dock ranges
        val dock = customizationConfig.dock
        if (dock.iconSizeDp !in 24..96) {
            errors.add("Dock icon size must be between 24dp and 96dp (was ${dock.iconSizeDp})")
        }
        if (dock.cornerRadiusDp !in 0..64) {
            errors.add("Dock corner radius must be between 0dp and 64dp (was ${dock.cornerRadiusDp})")
        }

        // Validate Typography
        val type = customizationConfig.typography
        if (type.fontScale !in 0.5f..2.5f) {
            errors.add("Font scale must be between 0.5x and 2.5x (was ${type.fontScale})")
        }

        // Validate Theme
        if (themeConfig.cornerRadiusDp !in 0..64) {
            errors.add("Theme corner radius must be between 0dp and 64dp (was ${themeConfig.cornerRadiusDp})")
        }

        return if (errors.isEmpty()) RiceValidationResult.Valid else RiceValidationResult.Invalid(errors)
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)
        root.put("version", version)
        root.put("themeConfig", JSONObject(themeConfig.toJson()))
        root.put("customizationConfig", JSONObject(customizationConfig.toJson()))
        if (wallpaperUri != null) {
            root.put("wallpaperUri", wallpaperUri)
        }
        root.put("createdAt", createdAt)
        root.put("updatedAt", updatedAt)
        return root.toString(2)
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromJson(jsonString: String?): Result<RiceProfile> {
            if (jsonString.isNullOrBlank()) {
                return Result.failure(IllegalArgumentException("Empty or null JSON payload"))
            }

            return runCatching {
                val rawJson = JSONObject(jsonString)
                val rawVersion = rawJson.optInt("version", 0)

                // Perform schema migration if needed (PRD Section 22 & Phase 9 Migration)
                val migratedJson = if (rawVersion < CURRENT_SCHEMA_VERSION) {
                    migrate(rawJson, rawVersion, CURRENT_SCHEMA_VERSION)
                } else {
                    rawJson
                }

                val id = migratedJson.optString("id", UUID.randomUUID().toString())
                val name = migratedJson.optString("name", "Imported Rice")
                val version = migratedJson.optInt("version", CURRENT_SCHEMA_VERSION)

                val themeConfigJson = migratedJson.optJSONObject("themeConfig")?.toString()
                val themeConfig = ThemeConfig.fromJson(themeConfigJson)

                val customizationConfigJson = migratedJson.optJSONObject("customizationConfig")?.toString()
                val customizationConfig = CustomizationConfig.fromJson(customizationConfigJson)

                val wallpaperUri = if (migratedJson.has("wallpaperUri")) migratedJson.optString("wallpaperUri") else null
                val createdAt = migratedJson.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = migratedJson.optLong("updatedAt", System.currentTimeMillis())

                val profile = RiceProfile(
                    id = id,
                    name = name,
                    version = version,
                    themeConfig = themeConfig,
                    customizationConfig = customizationConfig,
                    wallpaperUri = wallpaperUri,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )

                // Validate schema constraints (PRD Section 22 & Phase 9 Validation)
                when (val validation = profile.validate()) {
                    is RiceValidationResult.Valid -> profile
                    is RiceValidationResult.Invalid -> throw IllegalArgumentException("Rice schema validation failed: ${validation.errorMessage}")
                }
            }
        }

        /**
         * Migrates rice configuration schemas across versions (PRD Phase 9 Migration).
         */
        fun migrate(json: JSONObject, fromVersion: Int, targetVersion: Int): JSONObject {
            val migrated = JSONObject(json.toString())

            if (fromVersion < 1) {
                // Version 0 -> Version 1: Ensure themeConfig and customizationConfig objects exist
                if (!migrated.has("themeConfig")) {
                    migrated.put("themeConfig", JSONObject(ThemeConfig().toJson()))
                }
                if (!migrated.has("customizationConfig")) {
                    migrated.put("customizationConfig", JSONObject(CustomizationConfig().toJson()))
                }
                migrated.put("version", 1)
            }

            return migrated
        }
    }
}

/**
 * In-memory working draft state for live preview in Rice Studio (PRD Section 23).
 * Changes in draft do not write directly to permanent storage until applied or saved.
 */
data class RiceDraft(
    val profileId: String? = null,
    val name: String = "Custom Rice",
    val themeConfig: ThemeConfig = ThemeConfig(),
    val customizationConfig: CustomizationConfig = CustomizationConfig(),
    val wallpaperUri: String? = null,
    val isDirty: Boolean = false
) {
    fun toProfile(): RiceProfile {
        return RiceProfile(
            id = profileId ?: UUID.randomUUID().toString(),
            name = name,
            version = RiceProfile.CURRENT_SCHEMA_VERSION,
            themeConfig = themeConfig,
            customizationConfig = customizationConfig,
            wallpaperUri = wallpaperUri,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        fun fromProfile(profile: RiceProfile): RiceDraft {
            return RiceDraft(
                profileId = profile.id,
                name = profile.name,
                themeConfig = profile.themeConfig,
                customizationConfig = profile.customizationConfig,
                wallpaperUri = profile.wallpaperUri,
                isDirty = false
            )
        }
    }
}
