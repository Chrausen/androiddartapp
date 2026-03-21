package com.clubdarts.data.db.dao

import androidx.room.*
import com.clubdarts.data.model.Game
import com.clubdarts.data.model.GamePlayer
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Insert
    suspend fun insertGamePlayers(list: List<GamePlayer>)

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Long): Game?

    @Query("SELECT * FROM games WHERE winnerId IS NULL ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveGame(): Game?

    @Query("SELECT * FROM game_players WHERE gameId = :gameId ORDER BY throwOrder ASC")
    suspend fun getGamePlayers(gameId: Long): List<GamePlayer>

    @Query("DELETE FROM game_players WHERE gameId = :gameId")
    suspend fun deleteGamePlayers(gameId: Long)

    @Delete
    suspend fun deleteGame(game: Game)
}
