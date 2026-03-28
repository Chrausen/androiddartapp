package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.TrainingThrow

data class TrainingDartCoordinate(
    val targetX: Double,
    val targetY: Double,
    val actualX: Double,
    val actualY: Double
)

@Dao
interface TrainingThrowDao {

    @Insert
    suspend fun insertThrows(items: List<TrainingThrow>)

    @Query("SELECT * FROM training_throws WHERE sessionId = :sessionId ORDER BY throwIndex ASC")
    suspend fun getThrowsForSession(sessionId: Long): List<TrainingThrow>

    /**
     * Returns all training throws with complete coordinate data for a player across
     * specific sessions. Used for dispersion calculation.
     */
    @Query("""
        SELECT tt.targetX, tt.targetY, tt.actualX, tt.actualY
        FROM training_throws tt
        WHERE tt.sessionId IN (:sessionIds)
        AND tt.targetX IS NOT NULL AND tt.targetY IS NOT NULL
        AND tt.actualX IS NOT NULL AND tt.actualY IS NOT NULL
    """)
    suspend fun getCoordinatesForSessions(sessionIds: List<Long>): List<TrainingDartCoordinate>

    @Query("DELETE FROM training_throws")
    suspend fun deleteAll()
}
