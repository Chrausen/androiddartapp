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
    val playerIds: List<Long>   // in throw order
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
    suspend fun getGamePlayers(gameId: Long): List<GamePlayer> = gameDao.getGamePlayers(gameId)
    suspend fun deleteGamePlayers(gameId: Long) = gameDao.deleteGamePlayers(gameId)

    suspend fun insertLeg(leg: Leg): Long = legDao.insertLeg(leg)
    suspend fun updateLeg(leg: Leg) = legDao.updateLeg(leg)
    suspend fun getLegsForGame(gameId: Long): List<Leg> = legDao.getLegsForGame(gameId)
    fun getLegsForGameFlow(gameId: Long): Flow<List<Leg>> = legDao.getLegsForGameFlow(gameId)
    suspend fun getActiveLeg(gameId: Long): Leg? = legDao.getActiveLeg(gameId)
    suspend fun getLegById(id: Long): Leg? = legDao.getLegById(id)
    suspend fun getLegWinsForPlayer(playerId: Long): Int = legDao.getLegWinsForPlayer(playerId)

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
            isSolo = config.isSolo
        )
        val gameId = gameDao.insertGame(game)
        val gamePlayers = config.playerIds.mapIndexed { index, playerId ->
            GamePlayer(gameId = gameId, playerId = playerId, throwOrder = index)
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

    suspend fun finishGame(gameId: Long, winnerId: Long) {
        val game = gameDao.getGameById(gameId) ?: return
        gameDao.updateGame(game.copy(winnerId = winnerId, finishedAt = System.currentTimeMillis()))
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
