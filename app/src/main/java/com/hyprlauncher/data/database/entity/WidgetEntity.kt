package com.hyprlauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing a persisted widget on a workspace (PRD Section 25 & Phase 10).
 */
@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val appWidgetId: Int,
    val workspaceId: Int,
    val providerPackage: String,
    val providerClass: String,
    val cellX: Int,
    val cellY: Int,
    val spanX: Int = 2,
    val spanY: Int = 2,
    val label: String = "Widget",
    val createdAt: Long = System.currentTimeMillis()
)
