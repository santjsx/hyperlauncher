package com.hyprlauncher.data.repository

import com.hyprlauncher.core.widget.WidgetHostManager
import com.hyprlauncher.data.database.dao.WidgetDao
import com.hyprlauncher.data.database.entity.WidgetEntity
import com.hyprlauncher.domain.model.LauncherWidget
import com.hyprlauncher.domain.model.WidgetProviderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface WidgetRepository {
    val allWidgets: Flow<List<LauncherWidget>>
    fun getWidgetsForWorkspace(workspaceId: Int): Flow<List<LauncherWidget>>
    suspend fun getWidgetById(id: String): LauncherWidget?
    suspend fun placeWidget(
        workspaceId: Int,
        appWidgetId: Int,
        providerPackage: String,
        providerClass: String,
        cellX: Int,
        cellY: Int,
        spanX: Int = 2,
        spanY: Int = 2,
        label: String = "Widget"
    ): Result<LauncherWidget>
    suspend fun moveWidget(id: String, newCellX: Int, newCellY: Int): Result<Unit>
    suspend fun resizeWidget(id: String, newSpanX: Int, newSpanY: Int): Result<Unit>
    suspend fun removeWidget(id: String): Result<Unit>
    suspend fun removeWidgetByAppWidgetId(appWidgetId: Int): Result<Unit>
    suspend fun deleteWidgetsForWorkspace(workspaceId: Int): Result<Unit>
    fun getAvailableWidgetProviders(): List<WidgetProviderItem>
}

@Singleton
class DefaultWidgetRepository @Inject constructor(
    private val widgetDao: WidgetDao,
    private val widgetHostManager: WidgetHostManager
) : WidgetRepository {

    override val allWidgets: Flow<List<LauncherWidget>> = widgetDao.getAllWidgets().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getWidgetsForWorkspace(workspaceId: Int): Flow<List<LauncherWidget>> {
        return widgetDao.getWidgetsForWorkspace(workspaceId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getWidgetById(id: String): LauncherWidget? {
        return widgetDao.getWidgetById(id)?.toDomain()
    }

    override suspend fun placeWidget(
        workspaceId: Int,
        appWidgetId: Int,
        providerPackage: String,
        providerClass: String,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int,
        label: String
    ): Result<LauncherWidget> {
        val clampedSpanX = spanX.coerceIn(1, 8)
        val clampedSpanY = spanY.coerceIn(1, 12)
        val clampedCellX = cellX.coerceAtLeast(0)
        val clampedCellY = cellY.coerceAtLeast(0)

        val entity = WidgetEntity(
            id = UUID.randomUUID().toString(),
            appWidgetId = appWidgetId,
            workspaceId = workspaceId,
            providerPackage = providerPackage,
            providerClass = providerClass,
            cellX = clampedCellX,
            cellY = clampedCellY,
            spanX = clampedSpanX,
            spanY = clampedSpanY,
            label = label.ifBlank { "Widget" },
            createdAt = System.currentTimeMillis()
        )

        return runCatching {
            widgetDao.upsertWidget(entity)
            entity.toDomain()
        }
    }

    override suspend fun moveWidget(id: String, newCellX: Int, newCellY: Int): Result<Unit> {
        val clampedX = newCellX.coerceAtLeast(0)
        val clampedY = newCellY.coerceAtLeast(0)
        return runCatching {
            val rows = widgetDao.updateWidgetPosition(id, clampedX, clampedY)
            if (rows <= 0) throw IllegalArgumentException("Widget with id $id not found")
        }
    }

    override suspend fun resizeWidget(id: String, newSpanX: Int, newSpanY: Int): Result<Unit> {
        val clampedSpanX = newSpanX.coerceIn(1, 8)
        val clampedSpanY = newSpanY.coerceIn(1, 12)
        return runCatching {
            val rows = widgetDao.updateWidgetSpan(id, clampedSpanX, clampedSpanY)
            if (rows <= 0) throw IllegalArgumentException("Widget with id $id not found")
        }
    }

    override suspend fun removeWidget(id: String): Result<Unit> {
        val entity = widgetDao.getWidgetById(id) ?: return Result.success(Unit)
        return runCatching {
            runCatching { widgetHostManager.deleteAppWidgetId(entity.appWidgetId) }
            widgetDao.deleteWidget(id)
            Unit
        }
    }

    override suspend fun removeWidgetByAppWidgetId(appWidgetId: Int): Result<Unit> {
        return runCatching {
            runCatching { widgetHostManager.deleteAppWidgetId(appWidgetId) }
            widgetDao.deleteWidgetByAppWidgetId(appWidgetId)
            Unit
        }
    }

    override suspend fun deleteWidgetsForWorkspace(workspaceId: Int): Result<Unit> {
        return runCatching {
            widgetDao.deleteWidgetsForWorkspace(workspaceId)
            Unit
        }
    }

    override fun getAvailableWidgetProviders(): List<WidgetProviderItem> {
        return widgetHostManager.getAvailableProviders()
    }

    private fun WidgetEntity.toDomain(): LauncherWidget {
        return LauncherWidget(
            id = id,
            appWidgetId = appWidgetId,
            workspaceId = workspaceId,
            providerPackage = providerPackage,
            providerClass = providerClass,
            cellX = cellX,
            cellY = cellY,
            spanX = spanX,
            spanY = spanY,
            label = label,
            createdAt = createdAt
        )
    }
}
