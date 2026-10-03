package com.hyprlauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspaces ORDER BY orderIndex ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceById(id: Int): WorkspaceEntity?

    @Upsert
    suspend fun upsertWorkspaces(workspaces: List<WorkspaceEntity>): List<Long>

    @Upsert
    suspend fun upsertWorkspace(workspace: WorkspaceEntity): Long

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteWorkspace(id: Int): Int

    @Query("SELECT COUNT(*) FROM workspaces")
    suspend fun getWorkspaceCount(): Int
}
