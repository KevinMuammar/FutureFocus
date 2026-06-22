package com.example.futurefocus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.futurefocus.model.Achievement
import com.example.futurefocus.model.AchievementType

@Entity(tableName = "session_achievements")
data class SessionAchievementEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val achievementId: String,
    val typeName: String,
    val icon: String,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel() = Achievement(
        id = achievementId,
        type = try { AchievementType.valueOf(typeName) } catch (_: Exception) { AchievementType.FIRST_SESSION },
        title = title,
        description = description,
        value = 0,
        isUnlocked = true,
        unlockedAt = createdAt
    )

    companion object {
        fun fromAchievement(sessionId: String, achievement: Achievement): SessionAchievementEntity =
            SessionAchievementEntity(
                id = "${sessionId}_${achievement.id}",
                sessionId = sessionId,
                achievementId = achievement.id,
                typeName = achievement.type.name,
                icon = achievement.type.icon,
                title = achievement.title,
                description = achievement.description,
                createdAt = achievement.unlockedAt ?: System.currentTimeMillis()
            )
    }
}
