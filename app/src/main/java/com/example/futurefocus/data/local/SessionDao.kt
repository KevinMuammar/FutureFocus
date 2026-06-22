package com.example.futurefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Query("UPDATE sessions SET status = :status, completedAt = :completedAt, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long)

    @Query("UPDATE sessions SET attemptCount = :count, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun updateAttemptCount(id: String, count: Int)

    @Query("SELECT * FROM sessions WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingItems(userId: String): List<SessionEntity>

    @Query("UPDATE sessions SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sessions WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAllUnsafe()
}
