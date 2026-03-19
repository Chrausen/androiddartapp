package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.Leg
import kotlinx.coroutines.flow.Flow

@Dao
interface LegDao {
    @Insert
    suspend fun insertLeg(leg: Leg): Long

    @Update
    suspend fun updateLeg(leg: Leg)

    @Query("SELECT * FROM legs WHERE gameId = :gameId ORDER BY legNumber ASC")
    suspend fun getLegsForGame(gameId: Long): List<Leg>

    @Query("SELECT * FROM legs WHERE gameId = :gameId ORDER BY legNumber ASC")
    fun getLegsForGameFlow(gameId: Long): Flow<List<Leg>>

    @Query("SELECT * FROM legs WHERE gameId = :gameId AND winnerId IS NULL LIMIT 1")
    suspend fun getActiveLeg(gameId: Long): Leg?

    @Query("SELECT * FROM legs WHERE id = :id")
    suspend fun getLegById(id: Long): Leg?
}
