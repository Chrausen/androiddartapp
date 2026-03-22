package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.EloMatch

@Dao
interface EloMatchDao {
    @Insert
    suspend fun insert(match: EloMatch): Long

    @Query("SELECT * FROM elo_matches WHERE playerAId = :id OR playerBId = :id ORDER BY playedAt DESC")
    suspend fun getMatchesForPlayer(id: Long): List<EloMatch>

    @Query("DELETE FROM elo_matches")
    suspend fun deleteAll()
}
