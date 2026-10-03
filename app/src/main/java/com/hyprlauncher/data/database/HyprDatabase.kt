package com.hyprlauncher.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hyprlauncher.data.database.dao.AppDao
import com.hyprlauncher.data.database.dao.WorkspaceDao
import com.hyprlauncher.data.database.entity.AppEntity
import com.hyprlauncher.data.database.entity.WorkspaceEntity

@Database(
    entities = [
        AppEntity::class,
        WorkspaceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HyprDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun workspaceDao(): WorkspaceDao

    companion object {
        const val DATABASE_NAME = "hyprlauncher.db"
    }
}
