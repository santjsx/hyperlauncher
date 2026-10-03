package com.hyprlauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.RiceDao
import com.hyprlauncher.data.database.dao.WidgetDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.database.entity.RiceEntity
import com.hyprlauncher.data.database.entity.WidgetEntity
import com.hyprlauncher.data.database.entity.WorkspaceEntity

@Database(
    entities = [
        AppEntity::class,
        WorkspaceEntity::class,
        RiceEntity::class,
        WidgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class HyprDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun riceDao(): RiceDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        const val DATABASE_NAME = "hyprlauncher.db"
    }
}
