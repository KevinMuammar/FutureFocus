package com.example.futurefocus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionAchievementDao {
    @Query("SELECT * FROM session_achievements WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getBySessionId(sessionId: String): List<SessionAchievementEntity>

    @Query("SELECT * FROM session_achievements WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeBySessionId(sessionId: String): Flow<List<SessionAchievementEntity>>

    @Query("SELECT * FROM session_achievements ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SessionAchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SessionAchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SessionAchievementEntity>)

    @Query("DELETE FROM session_achievements WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
