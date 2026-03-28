package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.TrainingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {

    @Insert
    suspend fun insertSession(session: TrainingSession): Long

    @Query("SELECT * FROM training_sessions WHERE playerId = :playerId AND mode = :mode ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentSessionsForPlayer(playerId: Long, mode: String, limit: Int): List<TrainingSession>

    @Query("SELECT * FROM training_sessions WHERE playerId = :playerId AND mode = :mode ORDER BY completedAt ASC")
    suspend fun getSessionsForPlayerAndMode(playerId: Long, mode: String): List<TrainingSession>

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TrainingSession?

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAll()
}
