package com.hyprlauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hyprlauncher.data.database.entity.WidgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for widget persistence and workspace restoration (PRD Section 25 & Phase 10).
 */
@Dao
interface WidgetDao {

    @Query("SELECT * FROM widgets ORDER BY createdAt ASC")
    fun getAllWidgets(): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM widgets WHERE workspaceId = :workspaceId ORDER BY createdAt ASC")
    fun getWidgetsForWorkspace(workspaceId: Int): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM widgets WHERE id = :id LIMIT 1")
    suspend fun getWidgetById(id: String): WidgetEntity?

    @Query("SELECT * FROM widgets WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetEntity?

    @Upsert
    suspend fun upsertWidget(widget: WidgetEntity): Long

    @Query("UPDATE widgets SET cellX = :cellX, cellY = :cellY WHERE id = :id")
    suspend fun updateWidgetPosition(id: String, cellX: Int, cellY: Int): Int

    @Query("UPDATE widgets SET spanX = :spanX, spanY = :spanY WHERE id = :id")
    suspend fun updateWidgetSpan(id: String, spanX: Int, spanY: Int): Int

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteWidget(id: String): Int

    @Query("DELETE FROM widgets WHERE appWidgetId = :appWidgetId")
    suspend fun deleteWidgetByAppWidgetId(appWidgetId: Int): Int

    @Query("DELETE FROM widgets WHERE workspaceId = :workspaceId")
    suspend fun deleteWidgetsForWorkspace(workspaceId: Int): Int

    @Query("SELECT COUNT(*) FROM widgets")
    suspend fun getWidgetCount(): Int
}
