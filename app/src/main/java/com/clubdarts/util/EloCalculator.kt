package com.clubdarts.util

import com.clubdarts.data.model.Player
import kotlin.math.pow

/**
 * Pure Elo calculation logic extracted from EloRepository so it can be
 * unit-tested without any database or Android dependencies.
 */
object EloCalculator {

    /**
     * Compute signed Elo changes for all players using pairwise normalisation.
     *
     * For every pair (i, j):
     *   score_i = 1.0 if i is winner, 0.0 if j is winner, 0.5 if neither (draw among losers)
     *   delta_i += K × (score_i − expected_i_vs_j)
     *
     * The sum is divided by (N − 1) so the effective K-factor is constant regardless
     * of player count. For N = 2 this reduces exactly to the standard Elo formula.
     *
     * @param players  All players in the match, each carrying their current [Player.elo].
     * @param winnerId The [Player.id] of the match winner.
     * @param kFactor  Elo sensitivity factor (e.g. 32 for standard, 64 for aggressive).
     * @return Map of playerId → signed rating change (positive = gain, negative = loss).
     */
    fun computeChanges(
        players: List<Player>,
        winnerId: Long,
        kFactor: Double
    ): Map<Long, Double> {
        require(players.size >= 2) { "Need at least 2 players" }
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
     * Returns the ranked leaderboard: players with at least [minMatches] played,
     * sorted descending by Elo rating.
     */
    fun getLeaderboard(players: List<Player>, minMatches: Int): List<Player> =
        players
            .filter { it.matchesPlayed >= minMatches }
            .sortedByDescending { it.elo }
}
