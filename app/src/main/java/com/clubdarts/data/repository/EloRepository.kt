package com.clubdarts.data.repository

import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.EloMatchDao
import com.clubdarts.data.db.dao.EloMatchEntryDao
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.EloMatch
import com.clubdarts.data.model.EloMatchEntry
import com.clubdarts.data.model.Player
import com.clubdarts.util.EloCalculator
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

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

    data class EloMatchResult(val matchId: Long, val changes: Map<Long, Double>)

    /**
     * Record a ranked match for 2+ players and update their Elo ratings atomically.
     * Returns an EloMatchResult with the match ID and per-player signed Elo changes.
     */
    suspend fun recordMatch(
        players: List<Player>,
        winnerId: Long,
        playedAt: Long = System.currentTimeMillis()
    ): EloMatchResult {
        require(players.size >= 2) { "Need at least 2 players" }
        require(players.map { it.id }.distinct().size == players.size) { "Duplicate players" }
        require(players.any { it.id == winnerId }) { "Winner not in player list" }

        val kFactor = settingsRepository.getRankingKFactor().toDouble()

        return db.withTransaction {
            // Reload fresh ratings inside the transaction to avoid stale data
            val freshPlayers = players.map {
                playerDao.getPlayerById(it.id) ?: error("Player ${it.id} not found")
            }
            val changes = EloCalculator.computeChanges(freshPlayers, winnerId, kFactor)

            val matchId = eloMatchDao.insert(EloMatch(winnerId = winnerId, playedAt = playedAt))

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

            EloMatchResult(matchId = matchId, changes = changes)
        }
    }

    /**
     * Revert a previously recorded ranked match: restore player Elo/stats to pre-match values
     * and delete the EloMatch + EloMatchEntry records.
     */
    suspend fun revertMatch(matchId: Long) {
        db.withTransaction {
            val entries = eloMatchEntryDao.getEntriesForMatch(matchId)
            val playerIds = entries.map { it.playerId }
            val freshPlayers = playerDao.getPlayersByIds(playerIds)
            val playerMap = freshPlayers.associateBy { it.id }

            val revertedPlayers = entries.mapNotNull { entry ->
                val player = playerMap[entry.playerId] ?: return@mapNotNull null
                val wasWinner = entry.eloChange > 0
                player.copy(
                    elo = entry.eloBefore,
                    matchesPlayed = (player.matchesPlayed - 1).coerceAtLeast(0),
                    wins = if (wasWinner) (player.wins - 1).coerceAtLeast(0) else player.wins,
                    losses = if (!wasWinner) (player.losses - 1).coerceAtLeast(0) else player.losses
                )
            }
            playerDao.updateAll(revertedPlayers)
            eloMatchEntryDao.deleteByMatchId(matchId)
            eloMatchDao.deleteById(matchId)
        }
    }

    fun getLeaderboard(players: List<Player>): List<Player> =
        EloCalculator.getLeaderboard(players, LEADERBOARD_MIN_MATCHES)

    suspend fun resetAllRatings() {
        db.withTransaction {
            val players = playerDao.getAllPlayers().first()
            playerDao.updateAll(players.map {
                it.copy(elo = STARTING_ELO, matchesPlayed = 0, wins = 0, losses = 0)
            })
        }
    }
}
