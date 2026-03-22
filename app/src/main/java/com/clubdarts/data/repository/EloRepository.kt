package com.clubdarts.data.repository

import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.EloMatchDao
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.EloMatch
import com.clubdarts.data.model.Player
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.pow

@Singleton
class EloRepository @Inject constructor(
    private val db: AppDatabase,
    private val playerDao: PlayerDao,
    private val eloMatchDao: EloMatchDao,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        const val STARTING_ELO = 1000.0
        const val LEADERBOARD_MIN_MATCHES = 5
    }

    private fun calculateElo(
        ratingA: Double,
        ratingB: Double,
        winnerId: Long,
        playerAId: Long,
        kFactor: Double
    ): Triple<Double, Double, Double> {
        val expectedA = 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))
        val expectedB = 1.0 - expectedA
        val scoreA = if (winnerId == playerAId) 1.0 else 0.0
        val scoreB = 1.0 - scoreA
        val newA = ratingA + kFactor * (scoreA - expectedA)
        val newB = ratingB + kFactor * (scoreB - expectedB)
        val change = (kFactor * (scoreA - expectedA)).absoluteValue
        return Triple(newA, newB, change)
    }

    suspend fun recordMatch(playerAId: Long, playerBId: Long, winnerId: Long): EloMatch {
        require(playerAId != playerBId) { "Players must be different" }
        val kFactor = settingsRepository.getRankingKFactor().toDouble()

        return db.withTransaction {
            val playerA = playerDao.getPlayerById(playerAId)
                ?: error("Player $playerAId not found")
            val playerB = playerDao.getPlayerById(playerBId)
                ?: error("Player $playerBId not found")

            val (newA, newB, change) = calculateElo(
                playerA.elo, playerB.elo, winnerId, playerAId, kFactor
            )

            val match = EloMatch(
                playerAId = playerAId,
                playerBId = playerBId,
                winnerId = winnerId,
                playerAEloBefore = playerA.elo,
                playerBEloBefore = playerB.elo,
                playerAEloAfter = newA,
                playerBEloAfter = newB,
                eloChange = change
            )
            eloMatchDao.insert(match)

            playerDao.updateAll(listOf(
                playerA.copy(
                    elo = newA,
                    matchesPlayed = playerA.matchesPlayed + 1,
                    wins = if (winnerId == playerAId) playerA.wins + 1 else playerA.wins,
                    losses = if (winnerId != playerAId) playerA.losses + 1 else playerA.losses
                ),
                playerB.copy(
                    elo = newB,
                    matchesPlayed = playerB.matchesPlayed + 1,
                    wins = if (winnerId == playerBId) playerB.wins + 1 else playerB.wins,
                    losses = if (winnerId != playerBId) playerB.losses + 1 else playerB.losses
                )
            ))

            match
        }
    }

    fun getLeaderboard(players: List<Player>): List<Player> =
        players
            .filter { it.matchesPlayed >= LEADERBOARD_MIN_MATCHES }
            .sortedByDescending { it.elo }

    suspend fun resetAllRatings() {
        db.withTransaction {
            val players = playerDao.getAllPlayers().first()
            playerDao.updateAll(players.map {
                it.copy(elo = STARTING_ELO, matchesPlayed = 0, wins = 0, losses = 0)
            })
            eloMatchDao.deleteAll()
        }
    }
}
