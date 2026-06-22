package com.example.futurefocus.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.futurefocus.model.Achievement
import com.example.futurefocus.model.AchievementType
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.FocusStats
import com.example.futurefocus.model.NewAchievement
import com.example.futurefocus.model.SessionStatus

class AchievementManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("achievements", Context.MODE_PRIVATE)

    private val currentUserId: String
        get() = AuthManager.currentUser?.uid ?: "local"

    companion object {
        private val STREAK_ACHIEVEMENTS = listOf(
            1 to "Konsisten 1 Hari",
            3 to "Konsisten 3 Hari",
            7 to "Konsisten Seminggu",
            14 to "Dua Pekan Fokus",
            30 to "Sebulan Penuh!",
            60 to "Dua Bulan!",
            100 to "Century! 🔥"
        )
        private val SESSION_ACHIEVEMENTS = listOf(
            1 to "Sesi Pertama",
            5 to "5 Sesi",
            10 to "10 Sesi!",
            25 to "25 Sesi!",
            50 to "Si Fokus Sejati",
            100 to "100 Sesi!",
            250 to "Legenda Fokus",
            500 to "Tak Terhentikan"
        )
        private val HOUR_ACHIEVEMENTS = listOf(
            1 to "1 Jam Fokus",
            5 to "5 Jam Fokus",
            10 to "10 Jam!",
            25 to "25 Jam!",
            50 to "50 Jam!",
            100 to "100 Jam!",
            250 to "250 Jam!",
            500 to "500 Jam!",
            1000 to "1000 Jam! 🏆"
        )
        private val SINGLE_SESSION_ACHIEVEMENTS = listOf(
            1 to "Fokus 1 Menit",
            45 to "Fokus 45 Menit",
            60 to "Satu Jam Fokus!",
            90 to "Fokus 1.5 Jam",
            120 to "Dua Jam! 🧘"
        )
        private val DAILY_ACHIEVEMENTS = listOf(
            1 to "1 Menit Hari Ini",
            60 to "1 Jam Hari Ini",
            120 to "2 Jam Hari Ini!",
            180 to "3 Jam Hari Ini!",
            240 to "4 Jam! Luar Biasa"
        )

        fun allAchievements(): List<Achievement> {
            val list = mutableListOf<Achievement>()

            STREAK_ACHIEVEMENTS.forEachIndexed { i, (value, title) ->
                list.add(
                    Achievement(
                        id = "streak_$value",
                        type = AchievementType.STREAK,
                        title = title,
                        description = "Streak $value hari berturut-turut",
                        value = value,
                        unit = "hari"
                    )
                )
            }

            SESSION_ACHIEVEMENTS.forEachIndexed { i, (value, title) ->
                list.add(
                    Achievement(
                        id = "sessions_$value",
                        type = AchievementType.TOTAL_SESSIONS,
                        title = title,
                        description = "Selesaikan $value sesi fokus",
                        value = value,
                        unit = "sesi"
                    )
                )
            }

            HOUR_ACHIEVEMENTS.forEachIndexed { i, (value, title) ->
                list.add(
                    Achievement(
                        id = "hours_$value",
                        type = AchievementType.TOTAL_HOURS,
                        title = title,
                        description = "Total $value jam fokus",
                        value = value,
                        unit = "jam"
                    )
                )
            }

            SINGLE_SESSION_ACHIEVEMENTS.forEachIndexed { i, (value, title) ->
                list.add(
                    Achievement(
                        id = "single_$value",
                        type = AchievementType.SINGLE_SESSION,
                        title = title,
                        description = "Sesi fokus $value menit dalam sekali duduk",
                        value = value,
                        unit = "menit"
                    )
                )
            }

            DAILY_ACHIEVEMENTS.forEachIndexed { i, (value, title) ->
                list.add(
                    Achievement(
                        id = "daily_$value",
                        type = AchievementType.DAILY_FOCUS,
                        title = title,
                        description = "Fokus $value menit dalam sehari",
                        value = value,
                        unit = "menit"
                    )
                )
            }

            list.add(
                Achievement(
                    id = "first_goal",
                    type = AchievementType.FIRST_SESSION,
                    title = "Langkah Pertama",
                    description = "Selesaikan sesi fokus pertamamu",
                    value = 1,
                    unit = ""
                )
            )

            return list
        }
    }

    private val unlockedKey: String
        get() = "unlocked_$currentUserId"

    private val unlockedSet: MutableSet<String>
        get() = prefs.getStringSet(unlockedKey, emptySet())?.toMutableSet() ?: mutableSetOf()

    fun isUnlocked(id: String): Boolean = id in unlockedSet

    fun getUnlockedAchievements(): List<Achievement> {
        val unlocked = unlockedSet
        return allAchievements().filter { it.id in unlocked }
    }

    fun getAllWithStatus(): List<Achievement> {
        val unlocked = unlockedSet
        return allAchievements().map { it.copy(isUnlocked = it.id in unlocked) }
    }

    fun checkNewAchievements(
        stats: FocusStats,
        completedSession: FocusSession,
        allSessions: List<FocusSession>
    ): List<NewAchievement> {
        val unlocked = unlockedSet
        val successful = allSessions.filter { it.status == SessionStatus.Success }
        val totalMinutes = successful.sumOf { it.durationMinutes }
        val totalHours = totalMinutes / 60
        val totalSessions = successful.size
        val streak = stats.focusStreak

        val newlyUnlocked = mutableListOf<NewAchievement>()

        fun check(achievements: List<Pair<Int, String>>, prefix: String, currentValue: Int, unit: String) {
            achievements.forEach { (value, _) ->
                val id = "${prefix}_$value"
                if (id !in unlocked && currentValue >= value) {
                    val ach = allAchievements().first { it.id == id }
                    newlyUnlocked.add(NewAchievement(ach, currentValue))
                    markUnlocked(id)
                }
            }
        }

        check(STREAK_ACHIEVEMENTS, "streak", streak, "hari")
        check(SESSION_ACHIEVEMENTS, "sessions", totalSessions, "sesi")
        check(HOUR_ACHIEVEMENTS, "hours", totalHours, "jam")
        check(SINGLE_SESSION_ACHIEVEMENTS, "single", completedSession.durationMinutes, "menit")
        check(DAILY_ACHIEVEMENTS, "daily", stats.todayFocusMinutes, "menit")

        if ("first_goal" !in unlocked && totalSessions >= 1) {
            val ach = allAchievements().first { it.id == "first_goal" }
            newlyUnlocked.add(NewAchievement(ach, 1))
            markUnlocked("first_goal")
        }

        return newlyUnlocked
    }

    private fun markUnlocked(id: String) {
        val set = unlockedSet
        set.add(id)
        prefs.edit { putStringSet(unlockedKey, set) }
    }
}
