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

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    suspend fun getAllGamesList(): List<Game>

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

    @Query("UPDATE game_players SET placement = :placement WHERE gameId = :gameId AND playerId = :playerId")
    suspend fun updateGamePlayerPlacement(gameId: Long, playerId: Long, placement: Int)

    @Query("UPDATE game_players SET placement = NULL WHERE gameId = :gameId")
    suspend fun clearGamePlayerPlacements(gameId: Long)

    @Delete
    suspend fun deleteGame(game: Game)

    @Query("SELECT * FROM game_players")
    suspend fun getAllGamePlayers(): List<GamePlayer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<Game>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGamePlayers(gamePlayers: List<GamePlayer>)

    @Query("DELETE FROM games")
    suspend fun deleteAll()

    // ── Stats 1–4: game-level counts ────────────────────────────────────────

    @Query("""
        SELECT COUNT(DISTINCT gp.gameId) FROM game_players gp
        INNER JOIN games g ON gp.gameId = g.id
        WHERE gp.playerId = :playerId AND g.finishedAt IS NOT NULL
    """)
    suspend fun getGamesPlayed(playerId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM games g
        WHERE g.winnerId = :playerId AND g.finishedAt IS NOT NULL
    """)
    suspend fun getWins(playerId: Long): Int

    @Query("""
        SELECT winnerId as playerId, COUNT(*) as value FROM games
        WHERE winnerId IS NOT NULL AND finishedAt IS NOT NULL
        GROUP BY winnerId
    """)
    suspend fun getAllPlayerWins(): List<PlayerIntStat>

    @Query("""
        SELECT gp.playerId, COUNT(DISTINCT gp.gameId) as value FROM game_players gp
        INNER JOIN games g ON gp.gameId = g.id
        WHERE g.finishedAt IS NOT NULL
        GROUP BY gp.playerId
    """)
    suspend fun getAllPlayerGamesPlayed(): List<PlayerIntStat>

    @Query("""
        SELECT COUNT(*) FROM game_players gp
        INNER JOIN games g ON gp.gameId = g.id
        WHERE gp.playerId = :playerId AND gp.placement = 2 AND g.finishedAt IS NOT NULL
          AND (SELECT COUNT(*) FROM game_players gp2 WHERE gp2.gameId = gp.gameId) >= 3
    """)
    suspend fun getSecondPlaceCount(playerId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM game_players gp
        INNER JOIN games g ON gp.gameId = g.id
        WHERE gp.playerId = :playerId AND gp.placement = 3 AND g.finishedAt IS NOT NULL
          AND (SELECT COUNT(*) FROM game_players gp2 WHERE gp2.gameId = gp.gameId) >= 4
    """)
    suspend fun getThirdPlaceCount(playerId: Long): Int

    @Query("SELECT SUM(finishedAt - createdAt) FROM games WHERE finishedAt IS NOT NULL")
    suspend fun getTotalGamePlaytimeMs(): Long?

    // ── Stats 16–18: social stats ────────────────────────────────────────────

    @Query("""
        SELECT p.name FROM game_players gp1
        INNER JOIN game_players gp2 ON gp1.gameId = gp2.gameId AND gp2.playerId != :playerId
        INNER JOIN games g ON gp1.gameId = g.id
        INNER JOIN players p ON gp2.playerId = p.id
        WHERE gp1.playerId = :playerId AND g.finishedAt IS NOT NULL
        GROUP BY gp2.playerId
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getBestBuddy(playerId: Long): String?

    @Query("""
        SELECT p.name FROM game_players gp1
        INNER JOIN game_players gp2 ON gp1.gameId = gp2.gameId AND gp2.playerId != :playerId
        INNER JOIN games g ON gp1.gameId = g.id
        INNER JOIN players p ON gp2.playerId = p.id
        WHERE gp1.playerId = :playerId AND g.finishedAt IS NOT NULL
          AND gp1.placement IS NOT NULL AND gp2.placement IS NOT NULL
          AND gp2.placement < gp1.placement
        GROUP BY gp2.playerId
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getRival(playerId: Long): String?

    @Query("""
        SELECT p.name FROM game_players gp1
        INNER JOIN game_players gp2 ON gp1.gameId = gp2.gameId AND gp2.playerId != :playerId
        INNER JOIN games g ON gp1.gameId = g.id
        INNER JOIN players p ON gp2.playerId = p.id
        WHERE gp1.playerId = :playerId AND g.finishedAt IS NOT NULL
          AND gp1.placement IS NOT NULL AND gp2.placement IS NOT NULL
          AND gp1.placement < gp2.placement
        GROUP BY gp2.playerId
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getEasyWin(playerId: Long): String?
}
