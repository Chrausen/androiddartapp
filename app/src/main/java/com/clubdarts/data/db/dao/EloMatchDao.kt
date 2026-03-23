package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.EloMatch

@Dao
interface EloMatchDao {
    @Insert
    suspend fun insert(match: EloMatch): Long

    @Query("SELECT * FROM elo_matches ORDER BY playedAt DESC")
    suspend fun getAll(): List<EloMatch>

    @Query("SELECT * FROM elo_matches WHERE id IN (:ids)")
    suspend fun getMatchesByIds(ids: List<Long>): List<EloMatch>

    @Query("DELETE FROM elo_matches")
    suspend fun deleteAll()
}
