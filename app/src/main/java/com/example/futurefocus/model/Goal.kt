package com.example.futurefocus.model

import java.util.UUID

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val totalHours: Float,
    val remainingHours: Float = totalHours,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (totalHours > 0f) ((totalHours - remainingHours) / totalHours).coerceIn(0f, 1f) else 0f
}

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
