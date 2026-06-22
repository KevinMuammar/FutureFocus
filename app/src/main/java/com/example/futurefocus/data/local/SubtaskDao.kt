package com.example.futurefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE goalId = :goalId AND userId = :userId")
    fun observeByGoalId(goalId: String, userId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE goalId = :goalId AND userId = :userId")
    suspend fun getByGoalId(goalId: String, userId: String): List<SubtaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Boolean)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM subtasks WHERE goalId = :goalId AND userId = :userId")
    suspend fun deleteByGoalId(goalId: String, userId: String)

    @Query("SELECT * FROM subtasks WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingItems(userId: String): List<SubtaskEntity>

    @Query("UPDATE subtasks SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}
