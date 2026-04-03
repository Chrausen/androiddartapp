package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.EloMatchEntry

@Dao
interface EloMatchEntryDao {
    @Insert
    suspend fun insert(entry: EloMatchEntry): Long

    @Query("SELECT * FROM elo_match_entries WHERE matchId = :matchId")
    suspend fun getEntriesForMatch(matchId: Long): List<EloMatchEntry>

    @Query("SELECT * FROM elo_match_entries WHERE matchId IN (:matchIds)")
    suspend fun getEntriesForMatches(matchIds: List<Long>): List<EloMatchEntry>

    @Query("SELECT * FROM elo_match_entries WHERE playerId = :playerId ORDER BY rowid DESC")
    suspend fun getEntriesForPlayer(playerId: Long): List<EloMatchEntry>

    @Query("DELETE FROM elo_match_entries WHERE matchId = :matchId")
    suspend fun deleteByMatchId(matchId: Long)

    @Query("DELETE FROM elo_match_entries")
    suspend fun deleteAll()
}
