package com.example.futurefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.futurefocus.model.DailyGoal

@Entity(
    tableName = "daily_goals",
    primaryKeys = ["dateKey", "userId"]
)
data class DailyGoalEntity(
    val dateKey: String,
    val userId: String,
    val targetMinutes: Int,
    val completedMinutes: Int,
    val updatedAt: Long,
    val syncStatus: String = "SYNCED"
) {
    fun toDomainModel() = DailyGoal(
        dateKey = dateKey,
        targetMinutes = targetMinutes,
        completedMinutes = completedMinutes,
        updatedAt = updatedAt
    )
}

fun DailyGoal.toDailyGoalEntity(userId: String, syncStatus: String = "PENDING") = DailyGoalEntity(
    dateKey = dateKey,
    userId = userId,
    targetMinutes = targetMinutes,
    completedMinutes = completedMinutes,
    updatedAt = updatedAt,
    syncStatus = syncStatus
)
