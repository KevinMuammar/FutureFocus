package com.example.futurefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.futurefocus.model.Subtask

@Entity(
    tableName = "subtasks",
    indices = [androidx.room.Index("goalId")]
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val goalId: String,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val syncStatus: String = "SYNCED"
) {
    fun toDomainModel() = Subtask(
        id = id,
        title = title,
        isCompleted = isCompleted,
        createdAt = createdAt
    )
}

fun Subtask.toSubtaskEntity(userId: String, goalId: String, syncStatus: String = "PENDING") = SubtaskEntity(
    id = id,
    userId = userId,
    goalId = goalId,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
    syncStatus = syncStatus
)
