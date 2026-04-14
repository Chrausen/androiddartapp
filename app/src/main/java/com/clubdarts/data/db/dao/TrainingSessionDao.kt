package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.TrainingSession
import kotlinx.coroutines.flow.Flow

data class BestSessionWithPlayer(
    val session: TrainingSession,
    val playerName: String
)

/** Flat projection used by the JOIN queries below. Room needs this to be non-private. */
data class BestSessionRow(
    val id: Long,
    val playerId: Long,
    val mode: String,
    val difficulty: String,
    val result: Int,
    val completedCount: Int,
    val completedAt: Long,
    val playerName: String
) {
    fun toBestSessionWithPlayer() = BestSessionWithPlayer(
        session = TrainingSession(id, playerId, mode, difficulty, result, completedCount, completedAt),
        playerName = playerName
    )
}

/** One row in the club leaderboard: player's best result for a mode/difficulty. */
data class LeaderboardEntry(
    val playerId: Long,
    val playerName: String,
    val bestResult: Int
)

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

    /** Best result for modes where FEWER darts = better (TARGET_FIELD, AROUND_THE_CLOCK). */
    @Query("""
        SELECT ts.id, ts.playerId, ts.mode, ts.difficulty, ts.result, ts.completedCount, ts.completedAt,
               p.name AS playerName
        FROM training_sessions ts
        JOIN players p ON ts.playerId = p.id
        WHERE ts.mode = :mode AND ts.difficulty = :difficulty
        ORDER BY ts.result ASC
        LIMIT 1
    """)
    suspend fun getBestSessionAscending(mode: String, difficulty: String): BestSessionRow?

    /** Best result for modes where MORE points = better (SCORING_ROUNDS). */
    @Query("""
        SELECT ts.id, ts.playerId, ts.mode, ts.difficulty, ts.result, ts.completedCount, ts.completedAt,
               p.name AS playerName
        FROM training_sessions ts
        JOIN players p ON ts.playerId = p.id
        WHERE ts.mode = :mode AND ts.difficulty = :difficulty
        ORDER BY ts.result DESC
        LIMIT 1
    """)
    suspend fun getBestSessionDescending(mode: String, difficulty: String): BestSessionRow?

    /** Club leaderboard for modes where FEWER darts = better (TARGET_FIELD, AROUND_THE_CLOCK). */
    @Query("""
        SELECT ts.playerId, p.name AS playerName, MIN(ts.result) AS bestResult
        FROM training_sessions ts
        JOIN players p ON ts.playerId = p.id
        WHERE ts.mode = :mode AND ts.difficulty = :difficulty
        GROUP BY ts.playerId
        ORDER BY MIN(ts.result) ASC
    """)
    suspend fun getClubLeaderboardAscending(mode: String, difficulty: String): List<LeaderboardEntry>

    /** Club leaderboard for modes where MORE points = better (SCORING_ROUNDS). */
    @Query("""
        SELECT ts.playerId, p.name AS playerName, MAX(ts.result) AS bestResult
        FROM training_sessions ts
        JOIN players p ON ts.playerId = p.id
        WHERE ts.mode = :mode AND ts.difficulty = :difficulty
        GROUP BY ts.playerId
        ORDER BY MAX(ts.result) DESC
    """)
    suspend fun getClubLeaderboardDescending(mode: String, difficulty: String): List<LeaderboardEntry>

    @Query("SELECT SUM(completedAt - startedAt) FROM training_sessions WHERE startedAt > 0 AND completedAt > startedAt")
    suspend fun getTotalTrainingPlaytimeMs(): Long?

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAll()

    @Query("SELECT * FROM training_sessions")
    suspend fun getAll(): List<TrainingSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<TrainingSession>)
}
