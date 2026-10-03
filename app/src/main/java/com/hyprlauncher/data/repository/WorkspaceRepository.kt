package com.hyprlauncher.data.repository

import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.WorkspaceEntity
import com.hyprlauncher.domain.model.Workspace
import com.hyprlauncher.domain.model.WorkspaceLayoutConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface WorkspaceRepository {
    val allWorkspaces: Flow<List<Workspace>>
    suspend fun getWorkspaceById(id: Int): Workspace?
    suspend fun createWorkspace(name: String, iconName: String = "terminal"): Workspace
    suspend fun updateWorkspace(workspace: Workspace)
    suspend fun deleteWorkspace(id: Int): Boolean
    suspend fun reorderWorkspaces(workspaceIds: List<Int>)
    suspend fun updateWorkspaceLayout(id: Int, config: WorkspaceLayoutConfig)
    suspend fun ensureDefaultWorkspaces()
}

@Singleton
class DefaultWorkspaceRepository @Inject constructor(
    private val workspaceDao: WorkspaceDao
) : WorkspaceRepository {

    override val allWorkspaces: Flow<List<Workspace>> =
        workspaceDao.getAllWorkspaces().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getWorkspaceById(id: Int): Workspace? {
        return workspaceDao.getWorkspaceById(id)?.toDomain()
    }

    override suspend fun createWorkspace(name: String, iconName: String): Workspace {
        val currentEntities = workspaceDao.getAllWorkspaces().first()
        val nextId = (currentEntities.maxOfOrNull { it.id } ?: 0) + 1
        val nextOrderIndex = (currentEntities.maxOfOrNull { it.orderIndex } ?: -1) + 1

        val entity = WorkspaceEntity(
            id = nextId,
            name = name.ifBlank { "WS $nextId" },
            orderIndex = nextOrderIndex,
            iconName = iconName,
            wallpaperUri = null,
            layoutConfigJson = null
        )
        workspaceDao.upsertWorkspace(entity)
        return entity.toDomain()
    }

    override suspend fun updateWorkspace(workspace: Workspace) {
        workspaceDao.upsertWorkspace(workspace.toEntity())
    }

    override suspend fun deleteWorkspace(id: Int): Boolean {
        val count = workspaceDao.getWorkspaceCount()
        if (count <= 1) {
            // Cannot delete the only remaining workspace
            return false
        }
        val rowsDeleted = workspaceDao.deleteWorkspace(id)
        return rowsDeleted > 0
    }

    override suspend fun reorderWorkspaces(workspaceIds: List<Int>) {
        val current = workspaceDao.getAllWorkspaces().first().associateBy { it.id }
        val updated = workspaceIds.mapIndexedNotNull { index, id ->
            val entity = current[id] ?: return@mapIndexedNotNull null
            entity.copy(orderIndex = index)
        }
        if (updated.isNotEmpty()) {
            workspaceDao.upsertWorkspaces(updated)
        }
    }

    override suspend fun updateWorkspaceLayout(id: Int, config: WorkspaceLayoutConfig) {
        val existing = workspaceDao.getWorkspaceById(id) ?: return
        val updated = existing.copy(
            layoutConfigJson = config.toJson(),
            wallpaperUri = config.wallpaperUri ?: existing.wallpaperUri
        )
        workspaceDao.upsertWorkspace(updated)
    }

    override suspend fun ensureDefaultWorkspaces() {
        if (workspaceDao.getWorkspaceCount() == 0) {
            val defaults = listOf(
                WorkspaceEntity(id = 1, name = "Main", orderIndex = 0),
                WorkspaceEntity(id = 2, name = "Work", orderIndex = 1),
                WorkspaceEntity(id = 3, name = "Dev", orderIndex = 2),
                WorkspaceEntity(id = 4, name = "Media", orderIndex = 3),
                WorkspaceEntity(id = 5, name = "Games", orderIndex = 4)
            )
            workspaceDao.upsertWorkspaces(defaults)
        }
    }

    private fun WorkspaceEntity.toDomain(): Workspace {
        return Workspace(
            id = id,
            name = name,
            orderIndex = orderIndex,
            iconName = iconName,
            wallpaperUri = wallpaperUri,
            layoutConfig = WorkspaceLayoutConfig.fromJson(layoutConfigJson)
        )
    }

    private fun Workspace.toEntity(): WorkspaceEntity {
        return WorkspaceEntity(
            id = id,
            name = name,
            orderIndex = orderIndex,
            iconName = iconName,
            wallpaperUri = wallpaperUri ?: layoutConfig.wallpaperUri,
            layoutConfigJson = layoutConfig.toJson()
        )
    }
}
