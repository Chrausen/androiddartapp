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

    @Query("SELECT * FROM games WHERE finishedAt IS NULL ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveGame(): Game?

    @Query("SELECT * FROM games WHERE finishedAt IS NULL ORDER BY createdAt DESC LIMIT 1")
    fun observeActiveGame(): Flow<Game?>

    @Query("""
        SELECT gp.playerId FROM game_players gp
        INNER JOIN games g ON gp.gameId = g.id
        WHERE g.finishedAt IS NULL
    """)
    fun observeActiveGamePlayerIds(): Flow<List<Long>>

    @Query("SELECT * FROM game_players WHERE gameId = :gameId ORDER BY throwOrder ASC")
    suspend fun getGamePlayers(gameId: Long): List<GamePlayer>

    @Query("SELECT * FROM game_players WHERE gameId IN (:gameIds) ORDER BY throwOrder ASC")
    suspend fun getGamePlayersByGameIds(gameIds: List<Long>): List<GamePlayer>

    @Query("DELETE FROM game_players WHERE gameId = :gameId")
    suspend fun deleteGamePlayers(gameId: Long)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("DELETE FROM games")
    suspend fun deleteAll()
}
