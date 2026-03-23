package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.Throw
import kotlinx.coroutines.flow.Flow

data class ScoreFrequency(val visitTotal: Int, val frequency: Int)

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

    @Query("SELECT * FROM throws WHERE legId = :legId ORDER BY visitNumber DESC LIMIT 1")
    suspend fun getLastThrowInLeg(legId: Long): Throw?

    @Query("SELECT AVG(visitTotal) FROM throws WHERE playerId = :playerId AND isBust = 0")
    suspend fun getAverageForPlayer(playerId: Long): Double?

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
}
