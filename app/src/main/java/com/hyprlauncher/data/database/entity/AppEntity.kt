package com.hyprlauncher.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted application metadata for the local launcher index.
 * Conforms to PRD Section 13 (Application Indexing).
 */
@Entity(
    tableName = "apps",
    indices = [
        Index(value = ["label"]),
        Index(value = ["launchCount"]),
        Index(value = ["lastUsedTimestamp"]),
        Index(value = ["workspaceId"])
    ]
)
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: String = "OTHER",
    val installTime: Long = 0L,
    val updateTime: Long = 0L,
    val launchCount: Int = 0,
    val lastUsedTimestamp: Long = 0L,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val workspaceId: Int = 1
)
