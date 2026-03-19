package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.Player
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val dao: PlayerDao
) {
    fun getAllPlayers(): Flow<List<Player>> = dao.getAllPlayers()

    suspend fun getPlayersByIds(ids: List<Long>): List<Player> = dao.getPlayersByIds(ids)

    suspend fun getPlayerById(id: Long): Player? = dao.getPlayerById(id)

    suspend fun insertPlayer(player: Player): Long = dao.insertPlayer(player)

    suspend fun updatePlayer(player: Player) = dao.updatePlayer(player)

    suspend fun deletePlayer(player: Player) = dao.deletePlayer(player)

    suspend fun getPlayerCount(): Int = dao.getPlayerCount()

    suspend fun getRecentPlayers(ids: List<Long>): List<Player> {
        if (ids.isEmpty()) return emptyList()
        val players = dao.getPlayersByIds(ids)
        val playerMap = players.associateBy { it.id }
        return ids.mapNotNull { playerMap[it] }
    }
}
