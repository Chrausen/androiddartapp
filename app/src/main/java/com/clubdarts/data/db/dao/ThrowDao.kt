package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.Throw
import kotlinx.coroutines.flow.Flow

data class ScoreFrequency(val visitTotal: Int, val frequency: Int)

data class PlayerAverage(val playerId: Long, val average: Double)

data class PlayerIntStat(val playerId: Long, val value: Int)

data class PlayerDoubleStat(val playerId: Long, val value: Double)

/** Batch leaderboard aggregate — one row per player, all numeric stats in one query. */
data class PlayerLeaderboardRow(
    val playerId: Long,
    val average: Double?,
    val avgPerDart: Double?,
    val avgPerRound: Double?,
    val count180s: Int,
    val hundredPlus: Int,
    val highestFinish: Int?,
    val highestRound: Int?,
    val totalDarts: Int,
    val totalScoreThrown: Long,
    val checkoutAttempts: Int,
    val successfulCheckouts: Int
)

data class DartCoordinate(val x: Double, val y: Double)

data class PlayerStatsAggregate(
    val average: Double?,
    val count180s: Int,
    val hundredPlus: Int,
    val checkoutAttempts: Int,
    val successfulCheckouts: Int,
    val highestFinish: Int?,
    val bucketHigh: Int,
    val bucketMid: Int,
    val bucketLow: Int,
    val bucketVeryLow: Int,
    val bucketBusts: Int
)

/**
 * Extended per-throw / per-dart statistics for Stats 5–15.
 * Computed in a single JOIN query for efficiency.
 */
data class ExtendedStatsAggregate(
    /** Stat 5: total non-padding darts thrown */
    val totalDarts: Int,
    /** Stat 6: avg score per dart (excl. bust + checkout rounds) */
    val avgPerDart: Double?,
    /** Stat 7: avg score per round (excl. bust + checkout rounds) */
    val avgPerRound: Double?,
    /** Stat 10: highest single-round total (excl. bust) */
    val highestRound: Int?,
    /** Stat 11 numerator: darts with multiplier = 2 */
    val doubleCount: Int,
    /** Stat 12 numerator: darts with multiplier = 3 */
    val tripleCount: Int,
    /** Stat 13 numerator: non-padding darts with score = 0 */
    val outOfBoundsCount: Int,
    /** Stat 14: rounds where visitTotal < 10 (excl. bust) */
    val roundsUnder10Count: Int,
    /** Stat 14 denominator: all non-bust rounds */
    val nonBustRoundsCount: Int,
    /** Gesamt geworfene Punktzahl: sum of all visitTotals */
    val totalScoreThrown: Long
)

@Dao
interface ThrowDao {
    @Insert
    suspend fun insertThrow(throw_: Throw): Long

    @Delete
    suspend fun deleteThrow(throw_: Throw)

    @Query("SELECT * FROM throws WHERE legId = :legId ORDER BY visitNumber ASC")
    suspend fun getThrowsForLeg(legId: Long): List<Throw>

    @Query("SELECT * FROM throws WHERE legId = :legId ORDER BY visitNumber ASC")
    fun getThrowsForLegFlow(legId: Long): Flow<List<Throw>>

    @Query("SELECT * FROM throws WHERE legId = :legId AND playerId = :playerId ORDER BY visitNumber ASC")
    suspend fun getThrowsForPlayerInLeg(legId: Long, playerId: Long): List<Throw>

    @Query("SELECT * FROM throws WHERE legId = :legId ORDER BY id DESC LIMIT 1")
    suspend fun getLastThrowInLeg(legId: Long): Throw?

    @Query("SELECT AVG(visitTotal) FROM throws WHERE playerId = :playerId AND isBust = 0")
    suspend fun getAverageForPlayer(playerId: Long): Double?

    @Query("""
        SELECT playerId, AVG(CASE WHEN isBust = 0 THEN visitTotal ELSE NULL END) as average
        FROM throws GROUP BY playerId
    """)
    suspend fun getAllPlayerAverages(): List<PlayerAverage>

    @Query("""
        SELECT MAX(t.visitTotal) FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId
        AND t.isCheckoutAttempt = 1
        AND t.isBust = 0
        AND l.winnerId = t.playerId
    """)
    suspend fun getHighestFinishForPlayer(playerId: Long): Int?

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND visitTotal = 180")
    suspend fun get180sForPlayer(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND visitTotal >= 100 AND isBust = 0")
    suspend fun getHundredPlusForPlayer(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND isCheckoutAttempt = 1")
    suspend fun getCheckoutAttemptsForPlayer(playerId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId
        AND t.isCheckoutAttempt = 1
        AND l.winnerId = t.playerId
    """)
    suspend fun getSuccessfulCheckoutsForPlayer(playerId: Long): Int

    @Query("""
        SELECT visitTotal, COUNT(*) as frequency
        FROM throws
        WHERE playerId = :playerId AND isBust = 0
        GROUP BY visitTotal
        ORDER BY frequency DESC
        LIMIT 10
    """)
    suspend fun getVisitScoreFrequencyForPlayer(playerId: Long): List<ScoreFrequency>

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND visitTotal >= 100 AND visitTotal <= 180 AND isBust = 0")
    suspend fun getBucketHigh(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND visitTotal >= 60 AND visitTotal <= 99 AND isBust = 0")
    suspend fun getBucketMid(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND visitTotal >= 40 AND visitTotal <= 59 AND isBust = 0")
    suspend fun getBucketLow(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND visitTotal >= 1 AND visitTotal <= 39 AND isBust = 0")
    suspend fun getBucketVeryLow(playerId: Long): Int

    @Query("SELECT COUNT(*) FROM throws WHERE playerId = :playerId AND isBust = 1")
    suspend fun getBucketBusts(playerId: Long): Int

    @Query("""
        SELECT
            AVG(CASE WHEN t.isBust = 0 THEN t.visitTotal ELSE NULL END) AS average,
            COUNT(CASE WHEN t.visitTotal = 180 THEN 1 ELSE NULL END) AS count180s,
            COUNT(CASE WHEN t.visitTotal >= 100 AND t.isBust = 0 THEN 1 ELSE NULL END) AS hundredPlus,
            COUNT(CASE WHEN t.isCheckoutAttempt = 1 THEN 1 ELSE NULL END) AS checkoutAttempts,
            COUNT(CASE WHEN t.isCheckoutAttempt = 1 AND l.winnerId = t.playerId THEN 1 ELSE NULL END) AS successfulCheckouts,
            MAX(CASE WHEN t.isCheckoutAttempt = 1 AND t.isBust = 0 AND l.winnerId = t.playerId THEN t.visitTotal ELSE NULL END) AS highestFinish,
            COUNT(CASE WHEN t.visitTotal >= 100 AND t.visitTotal <= 180 AND t.isBust = 0 THEN 1 ELSE NULL END) AS bucketHigh,
            COUNT(CASE WHEN t.visitTotal >= 60 AND t.visitTotal <= 99 AND t.isBust = 0 THEN 1 ELSE NULL END) AS bucketMid,
            COUNT(CASE WHEN t.visitTotal >= 40 AND t.visitTotal <= 59 AND t.isBust = 0 THEN 1 ELSE NULL END) AS bucketLow,
            COUNT(CASE WHEN t.visitTotal >= 1 AND t.visitTotal <= 39 AND t.isBust = 0 THEN 1 ELSE NULL END) AS bucketVeryLow,
            COUNT(CASE WHEN t.isBust = 1 THEN 1 ELSE NULL END) AS bucketBusts
        FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId
    """)
    suspend fun getPlayerStatsAggregate(playerId: Long): PlayerStatsAggregate

    /**
     * Stats 5–14 in a single JOIN query. "Checkout round" = isCheckoutAttempt=1 AND leg won by player.
     */
    @Query("""
        SELECT
            COALESCE(SUM(t.dartsUsed), 0) AS totalDarts,
            CAST(
                SUM(CASE WHEN t.isBust = 0 AND (t.isCheckoutAttempt = 0 OR l.winnerId != t.playerId)
                    THEN t.visitTotal ELSE 0 END) AS REAL
            ) / NULLIF(
                SUM(CASE WHEN t.isBust = 0 AND (t.isCheckoutAttempt = 0 OR l.winnerId != t.playerId)
                    THEN t.dartsUsed ELSE 0 END), 0
            ) AS avgPerDart,
            AVG(CASE WHEN t.isBust = 0 AND (t.isCheckoutAttempt = 0 OR l.winnerId != t.playerId)
                THEN CAST(t.visitTotal AS REAL) ELSE NULL END) AS avgPerRound,
            MAX(CASE WHEN t.isBust = 0 THEN t.visitTotal ELSE NULL END) AS highestRound,
            (COALESCE(SUM(CASE WHEN t.dart1Mult = 2 THEN 1 ELSE 0 END), 0)
             + COALESCE(SUM(CASE WHEN t.dart2Mult = 2 AND t.dartsUsed >= 2 THEN 1 ELSE 0 END), 0)
             + COALESCE(SUM(CASE WHEN t.dart3Mult = 2 AND t.dartsUsed >= 3 THEN 1 ELSE 0 END), 0)) AS doubleCount,
            (COALESCE(SUM(CASE WHEN t.dart1Mult = 3 THEN 1 ELSE 0 END), 0)
             + COALESCE(SUM(CASE WHEN t.dart2Mult = 3 AND t.dartsUsed >= 2 THEN 1 ELSE 0 END), 0)
             + COALESCE(SUM(CASE WHEN t.dart3Mult = 3 AND t.dartsUsed >= 3 THEN 1 ELSE 0 END), 0)) AS tripleCount,
            (COALESCE(SUM(CASE WHEN t.dart1Score = 0 THEN 1 ELSE 0 END), 0)
             + COALESCE(SUM(CASE WHEN t.dart2Score = 0 AND t.dartsUsed >= 2 THEN 1 ELSE 0 END), 0)
             + COALESCE(SUM(CASE WHEN t.dart3Score = 0 AND t.dartsUsed >= 3 THEN 1 ELSE 0 END), 0)) AS outOfBoundsCount,
            COUNT(CASE WHEN t.isBust = 0 AND t.visitTotal < 10 THEN 1 ELSE NULL END) AS roundsUnder10Count,
            COUNT(CASE WHEN t.isBust = 0 THEN 1 ELSE NULL END) AS nonBustRoundsCount,
            COALESCE(SUM(t.visitTotal), 0) AS totalScoreThrown
        FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId
    """)
    suspend fun getExtendedPlayerStats(playerId: Long): ExtendedStatsAggregate

    /**
     * Stat 8: First-9 average — for each leg, sum the player's first 3 visits, then average over legs.
     * Bust and checkout rounds are included if they fall within the first 3 visits.
     */
    @Query("""
        SELECT AVG(CAST(legFirstNine AS REAL)) FROM (
            SELECT l.id, SUM(t.visitTotal) AS legFirstNine
            FROM throws t
            INNER JOIN legs l ON t.legId = l.id
            WHERE t.playerId = :playerId AND t.visitNumber <= 3
            GROUP BY l.id
        )
    """)
    suspend fun getFirst9Avg(playerId: Long): Double?

    /**
     * Returns all individual dart positions (mm from board centre) for a player
     * in the given game IDs. Null coordinates (numpad-entered darts) are excluded.
     */
    @Query("""
        SELECT t.dart1X as x, t.dart1Y as y
        FROM throws t INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId AND l.gameId IN (:gameIds) AND t.dart1X IS NOT NULL
        UNION ALL
        SELECT t.dart2X, t.dart2Y
        FROM throws t INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId AND l.gameId IN (:gameIds) AND t.dart2X IS NOT NULL AND t.dartsUsed >= 2
        UNION ALL
        SELECT t.dart3X, t.dart3Y
        FROM throws t INNER JOIN legs l ON t.legId = l.id
        WHERE t.playerId = :playerId AND l.gameId IN (:gameIds) AND t.dart3X IS NOT NULL AND t.dartsUsed >= 3
    """)
    suspend fun getDartCoordinatesForPlayer(playerId: Long, gameIds: List<Long>): List<DartCoordinate>

    @Query("""
        SELECT g.id FROM games g
        WHERE g.finishedAt IS NOT NULL
        ORDER BY g.createdAt ASC
    """)
    suspend fun getFinishedGameIdsSorted(): List<Long>

    @Query("SELECT playerId, COUNT(*) as value FROM throws WHERE visitTotal = 180 GROUP BY playerId")
    suspend fun getAllPlayer180s(): List<PlayerIntStat>

    @Query("""
        SELECT t.playerId, MAX(t.visitTotal) as value FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        WHERE t.isCheckoutAttempt = 1 AND t.isBust = 0 AND l.winnerId = t.playerId
        GROUP BY t.playerId
    """)
    suspend fun getAllPlayerHighestFinish(): List<PlayerIntStat>

    @Query("""
        SELECT
            t.playerId,
            AVG(CASE WHEN t.isBust = 0 THEN t.visitTotal ELSE NULL END) AS average,
            CAST(SUM(CASE WHEN t.isBust = 0 AND (t.isCheckoutAttempt = 0 OR l.winnerId != t.playerId)
                THEN t.visitTotal ELSE 0 END) AS REAL)
                / NULLIF(SUM(CASE WHEN t.isBust = 0 AND (t.isCheckoutAttempt = 0 OR l.winnerId != t.playerId)
                THEN t.dartsUsed ELSE 0 END), 0) AS avgPerDart,
            AVG(CASE WHEN t.isBust = 0 AND (t.isCheckoutAttempt = 0 OR l.winnerId != t.playerId)
                THEN CAST(t.visitTotal AS REAL) ELSE NULL END) AS avgPerRound,
            COUNT(CASE WHEN t.visitTotal = 180 THEN 1 ELSE NULL END) AS count180s,
            COUNT(CASE WHEN t.visitTotal >= 100 AND t.isBust = 0 THEN 1 ELSE NULL END) AS hundredPlus,
            MAX(CASE WHEN t.isCheckoutAttempt = 1 AND t.isBust = 0 AND l.winnerId = t.playerId
                THEN t.visitTotal ELSE NULL END) AS highestFinish,
            MAX(CASE WHEN t.isBust = 0 THEN t.visitTotal ELSE NULL END) AS highestRound,
            COALESCE(SUM(t.dartsUsed), 0) AS totalDarts,
            COALESCE(SUM(t.visitTotal), 0) AS totalScoreThrown,
            COUNT(CASE WHEN t.isCheckoutAttempt = 1 THEN 1 ELSE NULL END) AS checkoutAttempts,
            COUNT(CASE WHEN t.isCheckoutAttempt = 1 AND l.winnerId = t.playerId
                THEN 1 ELSE NULL END) AS successfulCheckouts
        FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        GROUP BY t.playerId
    """)
    suspend fun getAllPlayerLeaderboardRows(): List<PlayerLeaderboardRow>

    @Query("""
        SELECT t.playerId, AVG(CAST(legFirstNine AS REAL)) AS value FROM (
            SELECT t2.playerId, t2.legId, SUM(t2.visitTotal) AS legFirstNine
            FROM throws t2
            WHERE t2.visitNumber <= 3
            GROUP BY t2.playerId, t2.legId
        ) t
        GROUP BY t.playerId
    """)
    suspend fun getAllPlayerFirst9Avg(): List<PlayerDoubleStat>

    @Query("SELECT COUNT(*) FROM throws WHERE visitTotal = 180")
    suspend fun getTotalClub180s(): Int

    @Query("""
        SELECT MAX(t.visitTotal) FROM throws t
        INNER JOIN legs l ON t.legId = l.id
        WHERE t.isCheckoutAttempt = 1
        AND t.isBust = 0
        AND l.winnerId = t.playerId
    """)
    suspend fun getClubHighestFinish(): Int?

    @Query("SELECT * FROM throws")
    suspend fun getAll(): List<Throw>

    @Query("DELETE FROM throws")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(throws: List<Throw>)
}
