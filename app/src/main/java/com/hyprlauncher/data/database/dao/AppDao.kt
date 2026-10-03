package com.hyprlauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hyprlauncher.data.database.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps WHERE isHidden = 0 ORDER BY label ASC")
    fun getAllVisibleApps(): Flow<List<AppEntity>>

    @Query("SELECT packageName FROM apps WHERE isHidden = 0")
    suspend fun getAllVisiblePackageNames(): List<String>

    @Query("SELECT * FROM apps WHERE isHidden = 0 AND workspaceId = :workspaceId ORDER BY label ASC")
    fun getAppsForWorkspace(workspaceId: Int): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isHidden = 0 AND isFavorite = 1 ORDER BY launchCount DESC, label ASC")
    fun getFavoriteApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isHidden = 0 ORDER BY launchCount DESC, lastUsedTimestamp DESC LIMIT :limit")
    fun getFrequentApps(limit: Int): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isHidden = 0 AND (label LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%') ORDER BY CASE WHEN label LIKE :query || '%' THEN 1 ELSE 2 END, launchCount DESC")
    fun searchApps(query: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Upsert
    suspend fun upsertApps(apps: List<AppEntity>): List<Long>

    @Upsert
    suspend fun upsertApp(app: AppEntity): Long

    @Query("UPDATE apps SET launchCount = launchCount + 1, lastUsedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun recordAppLaunch(packageName: String, timestamp: Long): Int

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String): Int

    @Query("DELETE FROM apps WHERE packageName IN (:packageNames)")
    suspend fun deleteApps(packageNames: List<String>): Int

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int
}
