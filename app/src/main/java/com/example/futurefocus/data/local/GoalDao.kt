package com.example.futurefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<GoalEntity>)

    @Query("UPDATE goals SET remainingHours = :remainingHours, isCompleted = :isCompleted, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateHours(id: String, remainingHours: Float, isCompleted: Boolean)

    @Query("UPDATE goals SET isCompleted = :isCompleted, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Boolean)

    @Query("UPDATE goals SET title = :title, description = :description, totalHours = :totalHours, remainingHours = :remainingHours, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateGoal(id: String, title: String, description: String, totalHours: Float, remainingHours: Float)

    @Query("SELECT * FROM goals WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingItems(userId: String): List<GoalEntity>

    @Query("UPDATE goals SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
