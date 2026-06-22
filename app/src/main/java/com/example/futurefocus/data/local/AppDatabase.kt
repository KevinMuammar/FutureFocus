package com.example.futurefocus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [QuoteEntity::class, SessionEntity::class, GoalEntity::class, SubtaskEntity::class, DailyGoalEntity::class, SessionAchievementEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun sessionDao(): SessionDao
    abstract fun goalDao(): GoalDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun sessionAchievementDao(): SessionAchievementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "futurefocus_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
