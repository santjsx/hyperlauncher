package com.hyprlauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted workspace configuration inspired by Hyprland (PRD Section 17).
 */
@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val orderIndex: Int,
    val iconName: String = "terminal",
    val wallpaperUri: String? = null,
    val layoutConfigJson: String? = null
)
