package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayersList(): List<Player>

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<Long>): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): Player?

    @Insert
    suspend fun insertPlayer(player: Player): Long

    @Update
    suspend fun updatePlayer(player: Player)

    @Delete
    suspend fun deletePlayer(player: Player)

    @Update
    suspend fun updateAll(players: List<Player>)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int

    @Query("DELETE FROM players")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<Player>)
}
