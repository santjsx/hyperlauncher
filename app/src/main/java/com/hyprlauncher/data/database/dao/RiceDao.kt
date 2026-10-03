package com.hyprlauncher.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hyprlauncher.data.database.entity.RiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RiceDao {

    @Query("SELECT * FROM rice_profiles ORDER BY updatedAt DESC")
    fun getAllRices(): Flow<List<RiceEntity>>

    @Query("SELECT * FROM rice_profiles WHERE id = :id LIMIT 1")
    suspend fun getRiceById(id: String): RiceEntity?

    @Query("SELECT * FROM rice_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveRice(): Flow<RiceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rice: RiceEntity): Long

    @Query("DELETE FROM rice_profiles WHERE id = :id")
    suspend fun deleteRice(id: String): Int

    @Query("UPDATE rice_profiles SET isActive = 0")
    suspend fun clearActiveStatus(): Int

    @Query("UPDATE rice_profiles SET isActive = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun setActiveRice(id: String, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM rice_profiles")
    suspend fun getRiceCount(): Int
}
