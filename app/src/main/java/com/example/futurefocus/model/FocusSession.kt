package com.example.futurefocus.model

import java.util.UUID

enum class SessionStatus {
    Running,
    Success,
    Failed
}

data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val durationMinutes: Int,
    val attemptCount: Int = 0,
    val status: SessionStatus = SessionStatus.Running,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val goalId: String? = null,
    val goalTitle: String? = null
)

data class FocusStats(
    val totalFocusMinutes: Int = 0,
    val successfulSessions: Int = 0,
    val failedSessions: Int = 0,
    val focusStreak: Int = 0,
    val todayFocusMinutes: Int = 0,
    val dailyGoalMinutes: Int = 120
)

data class DailyGoal(
    val dateKey: String,
    val targetMinutes: Int = 120,
    val completedMinutes: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
