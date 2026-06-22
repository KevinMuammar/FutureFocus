package com.example.futurefocus.model

enum class AchievementType(val displayName: String, val icon: String) {
    STREAK("Streak", "🔥"),
    SINGLE_SESSION("Sesi Terbaik", "⚡"),
    DAILY_FOCUS("Fokus Harian", "📅"),
    TOTAL_SESSIONS("Total Sesi", "🎯"),
    TOTAL_HOURS("Total Jam", "⭐"),
    FIRST_SESSION("Pertama", "🚀"),
}

data class Achievement(
    val id: String,
    val type: AchievementType,
    val title: String,
    val description: String,
    val value: Int,
    val unit: String = "",
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
)

data class NewAchievement(
    val achievement: Achievement,
    val currentValue: Int,
)
