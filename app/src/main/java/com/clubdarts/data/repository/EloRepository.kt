package com.clubdarts.data.repository

import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.EloMatchDao
import com.clubdarts.data.db.dao.EloMatchEntryDao
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.EloMatch
import com.clubdarts.data.model.EloMatchEntry
import com.clubdarts.data.model.Player
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class EloRepository @Inject constructor(
    private val db: AppDatabase,
    private val playerDao: PlayerDao,
    private val eloMatchDao: EloMatchDao,
    private val eloMatchEntryDao: EloMatchEntryDao,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        const val STARTING_ELO = 1000.0
        const val LEADERBOARD_MIN_MATCHES = 5
    }

    /**
     * Compute signed Elo changes for all players using pairwise normalization.
     *
     * For every pair (i, j):
     *   score_i = 1.0 if i is winner, 0.0 if j is winner, 0.5 if neither is winner (draw among losers)
     *   delta_i += K * (score_i - expected_i_vs_j)
     *
     * The sum is then divided by (N-1) so the effective K-factor is constant regardless of player count.
     * For N=2 this reduces exactly to the standard Elo formula.
     */
    private fun computeChanges(
        players: List<Player>,
        winnerId: Long,
        kFactor: Double
    ): Map<Long, Double> {
        val n = players.size
        return players.associate { player ->
            var delta = 0.0
            players.forEach { opponent ->
                if (player.id != opponent.id) {
                    val expected = 1.0 / (1.0 + 10.0.pow((opponent.elo - player.elo) / 400.0))
                    val score = when {
                        player.id == winnerId   -> 1.0
                        opponent.id == winnerId -> 0.0
                        else                    -> 0.5   // draw among losers
                    }
                    delta += kFactor * (score - expected)
                }
            }
            player.id to (delta / (n - 1))
        }
    }

    /**
     * Record a ranked match for 2+ players and update their Elo ratings atomically.
     * Returns a map of playerId → signed Elo change (positive = gain, negative = loss).
     */
    suspend fun recordMatch(players: List<Player>, winnerId: Long): Map<Long, Double> {
        require(players.size >= 2) { "Need at least 2 players" }
        require(players.map { it.id }.distinct().size == players.size) { "Duplicate players" }
        require(players.any { it.id == winnerId }) { "Winner not in player list" }

        val kFactor = settingsRepository.getRankingKFactor().toDouble()

        return db.withTransaction {
            // Reload fresh ratings inside the transaction to avoid stale data
            val freshPlayers = players.map {
                playerDao.getPlayerById(it.id) ?: error("Player ${it.id} not found")
            }
            val changes = computeChanges(freshPlayers, winnerId, kFactor)

            val matchId = eloMatchDao.insert(EloMatch(winnerId = winnerId))

            val updatedPlayers = freshPlayers.map { p ->
                val delta = changes[p.id] ?: 0.0
                eloMatchEntryDao.insert(
                    EloMatchEntry(
                        matchId = matchId,
                        playerId = p.id,
                        eloBefore = p.elo,
                        eloAfter = p.elo + delta,
                        eloChange = delta
                    )
                )
                p.copy(
                    elo = p.elo + delta,
                    matchesPlayed = p.matchesPlayed + 1,
                    wins = if (p.id == winnerId) p.wins + 1 else p.wins,
                    losses = if (p.id != winnerId) p.losses + 1 else p.losses
                )
            }
            playerDao.updateAll(updatedPlayers)

            changes
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
            eloMatchEntryDao.deleteAll()
            eloMatchDao.deleteAll()
        }
    }
}
