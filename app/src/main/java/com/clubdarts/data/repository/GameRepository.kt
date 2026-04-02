package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.GameDao
import com.clubdarts.data.db.dao.LegDao
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.db.dao.ThrowDao
import com.clubdarts.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

data class GameConfig(
    val startScore: Int,
    val checkoutRule: CheckoutRule,
    val legsToWin: Int,
    val isSolo: Boolean,
    val playerIds: List<Long>,                           // in throw order (interleaved for team games)
    val isTeamGame: Boolean = false,
    val isRanked: Boolean = false,
    val teamAssignments: Map<Long, Int> = emptyMap()     // playerId → 0 (Team A) or 1 (Team B)
)

data class GameDetail(
    val game: Game,
    val players: List<Player>,
    val legs: List<LegDetail>
)

data class LegDetail(
    val leg: Leg,
    val throws: List<Throw>
)

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val legDao: LegDao,
    private val throwDao: ThrowDao,
    private val playerDao: PlayerDao
) {
    suspend fun insertGame(game: Game): Long = gameDao.insertGame(game)
    suspend fun updateGame(game: Game) = gameDao.updateGame(game)
    suspend fun insertGamePlayers(list: List<GamePlayer>) = gameDao.insertGamePlayers(list)
    fun getAllGames(): Flow<List<Game>> = gameDao.getAllGames()
    suspend fun getGameById(id: Long): Game? = gameDao.getGameById(id)
    suspend fun getActiveGame(): Game? = gameDao.getActiveGame()
    fun observeActiveGame(): Flow<Game?> = gameDao.observeActiveGame()
    fun observeActiveGamePlayerIds(): Flow<List<Long>> = gameDao.observeActiveGamePlayerIds()
    suspend fun deleteAll() = gameDao.deleteAll()
    suspend fun getGamePlayers(gameId: Long): List<GamePlayer> = gameDao.getGamePlayers(gameId)
    suspend fun getGamePlayersByGameIds(gameIds: List<Long>): List<GamePlayer> = gameDao.getGamePlayersByGameIds(gameIds)
    suspend fun deleteGamePlayers(gameId: Long) = gameDao.deleteGamePlayers(gameId)

    suspend fun insertLeg(leg: Leg): Long = legDao.insertLeg(leg)
    suspend fun updateLeg(leg: Leg) = legDao.updateLeg(leg)
    suspend fun getLegsForGame(gameId: Long): List<Leg> = legDao.getLegsForGame(gameId)
    fun getLegsForGameFlow(gameId: Long): Flow<List<Leg>> = legDao.getLegsForGameFlow(gameId)
    suspend fun getActiveLeg(gameId: Long): Leg? = legDao.getActiveLeg(gameId)
    suspend fun getLegById(id: Long): Leg? = legDao.getLegById(id)
    suspend fun getLegWinsForPlayer(playerId: Long): Int = legDao.getLegWinsForPlayer(playerId)
    suspend fun getGamesPlayed(playerId: Long): Int = gameDao.getGamesPlayed(playerId)
    suspend fun getWins(playerId: Long): Int = gameDao.getWins(playerId)
    suspend fun getSecondPlaceCount(playerId: Long): Int = gameDao.getSecondPlaceCount(playerId)
    suspend fun getThirdPlaceCount(playerId: Long): Int = gameDao.getThirdPlaceCount(playerId)
    suspend fun getBestBuddy(playerId: Long): String? = gameDao.getBestBuddy(playerId)
    suspend fun getRival(playerId: Long): String? = gameDao.getRival(playerId)
    suspend fun getEasyWin(playerId: Long): String? = gameDao.getEasyWin(playerId)

    suspend fun insertThrow(throw_: Throw): Long = throwDao.insertThrow(throw_)
    suspend fun deleteThrow(throw_: Throw) = throwDao.deleteThrow(throw_)
    suspend fun getThrowsForLeg(legId: Long): List<Throw> = throwDao.getThrowsForLeg(legId)
    fun getThrowsForLegFlow(legId: Long): Flow<List<Throw>> = throwDao.getThrowsForLegFlow(legId)
    suspend fun getThrowsForPlayerInLeg(legId: Long, playerId: Long): List<Throw> =
        throwDao.getThrowsForPlayerInLeg(legId, playerId)
    suspend fun getLastThrowInLeg(legId: Long): Throw? = throwDao.getLastThrowInLeg(legId)

    suspend fun startGame(config: GameConfig): Long {
        val game = Game(
            startScore = config.startScore,
            checkoutRule = config.checkoutRule,
            legsToWin = config.legsToWin,
            isSolo = config.isSolo,
            isTeamGame = config.isTeamGame,
            isRanked = config.isRanked
        )
        val gameId = gameDao.insertGame(game)
        val gamePlayers = config.playerIds.mapIndexed { index, playerId ->
            GamePlayer(
                gameId = gameId,
                playerId = playerId,
                throwOrder = index,
                teamIndex = config.teamAssignments[playerId] ?: -1
            )
        }
        gameDao.insertGamePlayers(gamePlayers)
        val leg = Leg(gameId = gameId, legNumber = 1)
        legDao.insertLeg(leg)
        return gameId
    }

    suspend fun finishLeg(legId: Long, winnerId: Long) {
        val leg = legDao.getLegById(legId) ?: return
        legDao.updateLeg(leg.copy(winnerId = winnerId, finishedAt = System.currentTimeMillis()))
    }

    suspend fun unfinishLeg(legId: Long) {
        val leg = legDao.getLegById(legId) ?: return
        legDao.updateLeg(leg.copy(winnerId = null, finishedAt = null))
    }

    suspend fun finishGame(gameId: Long, winnerId: Long?, winningTeamIndex: Int? = null) {
        val game = gameDao.getGameById(gameId) ?: return
        gameDao.updateGame(game.copy(
            winnerId = winnerId,
            winningTeamIndex = winningTeamIndex,
            finishedAt = System.currentTimeMillis()
        ))
        // Assign placements for non-team games
        if (!game.isTeamGame && winnerId != null) {
            val gamePlayers = gameDao.getGamePlayers(gameId)
            val legs = legDao.getLegsForGame(gameId)
            val legWinsByPlayer = legs.groupBy { it.winnerId }.mapValues { it.value.size }
            gameDao.updateGamePlayerPlacement(gameId, winnerId, 1)
            gamePlayers
                .filter { it.playerId != winnerId }
                .sortedByDescending { legWinsByPlayer[it.playerId] ?: 0 }
                .forEachIndexed { index, gp ->
                    gameDao.updateGamePlayerPlacement(gameId, gp.playerId, index + 2)
                }
        }
    }

    suspend fun unfinishGame(gameId: Long) {
        val game = gameDao.getGameById(gameId) ?: return
        gameDao.updateGame(game.copy(winnerId = null, winningTeamIndex = null, finishedAt = null))
        gameDao.clearGamePlayerPlacements(gameId)
    }

    suspend fun deleteGame(gameId: Long) {
        val game = gameDao.getGameById(gameId) ?: return
        gameDao.deleteGame(game)
    }

    suspend fun getFullGameDetail(gameId: Long): GameDetail? {
        val game = gameDao.getGameById(gameId) ?: return null
        val gamePlayers = gameDao.getGamePlayers(gameId)
        val players = playerDao.getPlayersByIds(gamePlayers.map { it.playerId })
        val legs = legDao.getLegsForGame(gameId)
        val legDetails = legs.map { leg ->
            val throws = throwDao.getThrowsForLeg(leg.id)
            LegDetail(leg = leg, throws = throws)
        }
        return GameDetail(game = game, players = players, legs = legDetails)
    }
}
