package com.example.futurefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals WHERE dateKey = :dateKey AND userId = :userId")
    fun observeByDate(dateKey: String, userId: String): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals WHERE dateKey = :dateKey AND userId = :userId")
    suspend fun getByDate(dateKey: String, userId: String): DailyGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingItems(userId: String): List<DailyGoalEntity>

    @Query("DELETE FROM daily_goals WHERE dateKey = :dateKey AND userId = :userId")
    suspend fun deleteByDateKey(dateKey: String, userId: String)

    @Query("UPDATE daily_goals SET syncStatus = 'SYNCED' WHERE dateKey = :dateKey AND userId = :userId")
    suspend fun markSynced(dateKey: String, userId: String)
}
