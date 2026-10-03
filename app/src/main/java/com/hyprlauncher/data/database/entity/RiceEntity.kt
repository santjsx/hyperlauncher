package com.hyprlauncher.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rice_profiles")
data class RiceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val version: Int = 1,
    val isActive: Boolean = false,
    val jsonPayload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
