package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.DartCoordinate
import com.clubdarts.data.db.dao.ThrowDao
import com.clubdarts.data.db.dao.TrainingDartCoordinate
import com.clubdarts.data.db.dao.TrainingSessionDao
import com.clubdarts.data.db.dao.TrainingThrowDao
import com.clubdarts.data.model.TrainingDifficulty
import com.clubdarts.data.model.TrainingMode
import com.clubdarts.data.model.TrainingSession
import com.clubdarts.data.model.TrainingThrow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepository @Inject constructor(
    private val sessionDao: TrainingSessionDao,
    private val throwDao: TrainingThrowDao,
    private val gameThrowDao: ThrowDao
) {
    suspend fun saveSession(
        playerId: Long,
        mode: TrainingMode,
        difficulty: TrainingDifficulty,
        result: Int,
        completedCount: Int,
        throws: List<TrainingThrow>
    ): Long {
        val session = TrainingSession(
            playerId = playerId,
            mode = mode.name,
            difficulty = difficulty.name,
            result = result,
            completedCount = completedCount
        )
        val sessionId = sessionDao.insertSession(session)
        if (throws.isNotEmpty()) {
            throwDao.insertThrows(throws.map { it.copy(sessionId = sessionId) })
        }
        return sessionId
    }

    suspend fun getRecentResults(playerId: Long, mode: TrainingMode, limit: Int = 10): List<TrainingSession> =
        sessionDao.getRecentSessionsForPlayer(playerId, mode.name, limit)

    /** Returns sessions for a player+mode sorted chronologically (oldest first). */
    suspend fun getSessionsChronological(playerId: Long, mode: TrainingMode): List<TrainingSession> =
        sessionDao.getSessionsForPlayerAndMode(playerId, mode.name)

    /** Returns all training throws with coordinates for the given sessions (for dispersion). */
    suspend fun getCoordinatesForSessions(sessionIds: List<Long>): List<TrainingDartCoordinate> {
        if (sessionIds.isEmpty()) return emptyList()
        return throwDao.getCoordinatesForSessions(sessionIds)
    }

    /**
     * Returns dart positions (mm from board centre) for a player's games within
     * the specified game index range [fromIndex..toIndex] (1-based, inclusive).
     * Uses games sorted by createdAt ascending.
     */
    suspend fun getDartCoordinatesForHeatmap(
        playerId: Long,
        fromIndex: Int,
        toIndex: Int
    ): List<DartCoordinate> {
        val allGameIds = gameThrowDao.getFinishedGameIdsSorted()
        if (allGameIds.isEmpty()) return emptyList()
        val clamped = allGameIds.indices
            .filter { it + 1 in fromIndex..toIndex }
            .map { allGameIds[it] }
        if (clamped.isEmpty()) return emptyList()
        return gameThrowDao.getDartCoordinatesForPlayer(playerId, clamped)
    }

    suspend fun getTotalFinishedGameCount(): Int =
        gameThrowDao.getFinishedGameIdsSorted().size
}
