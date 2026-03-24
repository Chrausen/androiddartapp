package com.clubdarts.util

import com.clubdarts.data.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Unit tests for [EloCalculator] — no Android or database dependencies. */
class EloCalculatorTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun player(id: Long, elo: Double, matches: Int = 10) =
        Player(id = id, name = "P$id", elo = elo, matchesPlayed = matches)

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 0.01) {
        assertTrue(
            "Expected $expected ± $tolerance but was $actual",
            abs(expected - actual) <= tolerance
        )
    }

    // -----------------------------------------------------------------------
    // computeChanges — 1-v-1 (standard Elo)
    // -----------------------------------------------------------------------

    @Test
    fun `1v1 equal ratings winner gains and loser loses symmetrically`() {
        val p1 = player(1, 1000.0)
        val p2 = player(2, 1000.0)
        val changes = EloCalculator.computeChanges(listOf(p1, p2), winnerId = 1, kFactor = 32.0)

        // expected_1_vs_2 = 0.5, score_1 = 1.0 → delta = 32*(1 - 0.5) = +16
        assertClose(+16.0, changes.getValue(1))
        assertClose(-16.0, changes.getValue(2))
    }

    @Test
    fun `1v1 higher rated player wins — gains less than when underdog wins`() {
        val pStrong = player(1, 1200.0)
        val pWeak   = player(2, 1000.0)

        val changesStrongWins = EloCalculator.computeChanges(
            listOf(pStrong, pWeak), winnerId = 1, kFactor = 32.0
        )
        val changesWeakWins = EloCalculator.computeChanges(
            listOf(pStrong, pWeak), winnerId = 2, kFactor = 32.0
        )

        // Strong player is favoured → wins less, loses more than equal case
        assertTrue(changesStrongWins.getValue(1) < 16.0)
        assertTrue(changesWeakWins.getValue(2) > 16.0)
    }

    @Test
    fun `1v1 lower rated player wins — gains more than 16`() {
        val pStrong = player(1, 1200.0)
        val pWeak   = player(2, 1000.0)
        val changes = EloCalculator.computeChanges(
            listOf(pStrong, pWeak), winnerId = 2, kFactor = 32.0
        )
        assertTrue(changes.getValue(2) > 16.0)
        assertTrue(changes.getValue(1) < -16.0)
    }

    @Test
    fun `1v1 changes sum to zero`() {
        val p1 = player(1, 1000.0)
        val p2 = player(2, 1100.0)
        val changes = EloCalculator.computeChanges(listOf(p1, p2), winnerId = 1, kFactor = 32.0)
        assertClose(0.0, changes.getValue(1) + changes.getValue(2))
    }

    @Test
    fun `1v1 kFactor scales result linearly`() {
        val p1 = player(1, 1000.0)
        val p2 = player(2, 1000.0)

        val k32 = EloCalculator.computeChanges(listOf(p1, p2), 1, 32.0).getValue(1)
        val k64 = EloCalculator.computeChanges(listOf(p1, p2), 1, 64.0).getValue(1)

        assertClose(k32 * 2.0, k64)
    }

    @Test
    fun `1v1 standard formula exact value for equal ratings k32`() {
        // expected = 0.5, score = 1, K = 32 → delta = 32*(1-0.5) = 16
        val changes = EloCalculator.computeChanges(
            listOf(player(1, 1000.0), player(2, 1000.0)),
            winnerId = 1, kFactor = 32.0
        )
        assertClose(16.0, changes.getValue(1))
    }

    // -----------------------------------------------------------------------
    // computeChanges — 3-player
    // -----------------------------------------------------------------------

    @Test
    fun `3 players all equal ratings winner gains and losers lose`() {
        val players = listOf(player(1, 1000.0), player(2, 1000.0), player(3, 1000.0))
        val changes = EloCalculator.computeChanges(players, winnerId = 1, kFactor = 32.0)

        assertTrue(changes.getValue(1) > 0)
        assertTrue(changes.getValue(2) < 0)
        assertTrue(changes.getValue(3) < 0)
    }

    @Test
    fun `3 players losers get same change when all equal ratings`() {
        val players = listOf(player(1, 1000.0), player(2, 1000.0), player(3, 1000.0))
        val changes = EloCalculator.computeChanges(players, winnerId = 1, kFactor = 32.0)
        assertClose(changes.getValue(2), changes.getValue(3))
    }

    @Test
    fun `3 players changes sum to approximately zero`() {
        val players = listOf(player(1, 1000.0), player(2, 1100.0), player(3, 900.0))
        val changes = EloCalculator.computeChanges(players, winnerId = 3, kFactor = 32.0)
        val total = changes.values.sum()
        assertClose(0.0, total, tolerance = 0.001)
    }

    @Test
    fun `3 players result map contains exactly all player IDs`() {
        val players = listOf(player(10, 1000.0), player(20, 1000.0), player(30, 1000.0))
        val changes = EloCalculator.computeChanges(players, winnerId = 10, kFactor = 32.0)
        assertEquals(setOf(10L, 20L, 30L), changes.keys)
    }

    // -----------------------------------------------------------------------
    // computeChanges — validation
    // -----------------------------------------------------------------------

    @Test
    fun `fewer than 2 players throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            EloCalculator.computeChanges(listOf(player(1, 1000.0)), winnerId = 1, kFactor = 32.0)
        }
    }

    // -----------------------------------------------------------------------
    // getLeaderboard
    // -----------------------------------------------------------------------

    @Test
    fun `getLeaderboard filters players below minMatches`() {
        val qualified   = player(1, 1200.0, matches = 5)
        val unqualified = player(2, 1500.0, matches = 3)
        val board = EloCalculator.getLeaderboard(listOf(qualified, unqualified), minMatches = 5)
        assertEquals(listOf(qualified), board)
    }

    @Test
    fun `getLeaderboard sorts by Elo descending`() {
        val low  = player(1, 900.0,  matches = 10)
        val mid  = player(2, 1000.0, matches = 10)
        val high = player(3, 1200.0, matches = 10)
        val board = EloCalculator.getLeaderboard(listOf(low, mid, high), minMatches = 5)
        assertEquals(listOf(high, mid, low), board)
    }

    @Test
    fun `getLeaderboard returns empty list when no players qualify`() {
        val p = player(1, 1500.0, matches = 2)
        val board = EloCalculator.getLeaderboard(listOf(p), minMatches = 5)
        assertTrue(board.isEmpty())
    }

    @Test
    fun `getLeaderboard with empty input returns empty`() {
        assertTrue(EloCalculator.getLeaderboard(emptyList(), minMatches = 5).isEmpty())
    }

    @Test
    fun `getLeaderboard includes player with exactly minMatches`() {
        val p = player(1, 1000.0, matches = 5)
        val board = EloCalculator.getLeaderboard(listOf(p), minMatches = 5)
        assertEquals(listOf(p), board)
    }
}
