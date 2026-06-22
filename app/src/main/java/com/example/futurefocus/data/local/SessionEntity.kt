package com.example.futurefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.SessionStatus

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val durationMinutes: Int,
    val attemptCount: Int,
    val status: String,
    val createdAt: Long,
    val completedAt: Long?,
    val goalId: String?,
    val goalTitle: String?,
    val syncStatus: String = "SYNCED"
) {
    fun toDomainModel() = FocusSession(
        id = id,
        durationMinutes = durationMinutes,
        attemptCount = attemptCount,
        status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.Running),
        createdAt = createdAt,
        completedAt = completedAt,
        goalId = goalId,
        goalTitle = goalTitle
    )
}

fun FocusSession.toSessionEntity(userId: String, syncStatus: String = "PENDING") = SessionEntity(
    id = id,
    userId = userId,
    durationMinutes = durationMinutes,
    attemptCount = attemptCount,
    status = status.name,
    createdAt = createdAt,
    completedAt = completedAt,
    goalId = goalId,
    goalTitle = goalTitle,
    syncStatus = syncStatus
)
