package com.example.futurefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.futurefocus.model.Goal

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val totalHours: Float,
    val remainingHours: Float,
    val isCompleted: Boolean,
    val createdAt: Long,
    val syncStatus: String = "SYNCED"
) {
    fun toDomainModel() = Goal(
        id = id,
        title = title,
        description = description,
        totalHours = totalHours,
        remainingHours = remainingHours,
        isCompleted = isCompleted,
        createdAt = createdAt
    )
}

fun Goal.toGoalEntity(userId: String, syncStatus: String = "PENDING") = GoalEntity(
    id = id,
    userId = userId,
    title = title,
    description = description,
    totalHours = totalHours,
    remainingHours = remainingHours,
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = syncStatus
)
