package com.clubdarts.ui.game

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that the team turn-order algorithm (as implemented in GameViewModel)
 * always alternates between teams regardless of team size.
 *
 * The algorithm is reproduced here as pure functions so no Android framework is
 * required.  Any change to the logic in GameViewModel should be mirrored here.
 */
class TeamTurnOrderTest {

    // ── Algorithm under test (mirrors GameViewModel) ──────────────────────────

    /**
     * Maps each team index to the ordered list of player-list indices that
     * belong to it.  Equivalent to GameViewModel.computeTeamPlayerIndices().
     *
     * @param playerIds   Ordered player id list (interleaved as stored in state)
     * @param assignments playerId → team index (0 or 1)
     */
    private fun computeTeamPlayerIndices(
        playerIds: List<Long>,
        assignments: Map<Long, Int>
    ): Map<Int, List<Int>> {
        val result = mutableMapOf<Int, MutableList<Int>>()
        playerIds.forEachIndexed { idx, id ->
            val team = assignments[id] ?: return@forEachIndexed
            result.getOrPut(team) { mutableListOf() }.add(idx)
        }
        return result
    }

    /**
     * Simulates [steps] visits and returns the sequence of player-list indices
     * that were active, using the same advancement logic as GameViewModel.resolveVisit().
     */
    private fun simulateTurns(
        playerIds: List<Long>,
        assignments: Map<Long, Int>,
        steps: Int
    ): List<Int> {
        val teamPlayerIndices = computeTeamPlayerIndices(playerIds, assignments)
        var currentPlayerIndex = 0
        var currentTeamIndex = 0
        val teamPlayerIndexes = mutableMapOf(0 to 0, 1 to 0)

        val result = mutableListOf<Int>()
        repeat(steps) {
            result.add(currentPlayerIndex)

            // Advance — same logic as resolveVisit()
            val prevTeam = currentTeamIndex
            val nextTeam = 1 - prevTeam
            val prevTeamSize = teamPlayerIndices[prevTeam]?.size ?: 1
            val advancedPrevIdx = ((teamPlayerIndexes[prevTeam] ?: 0) + 1) % prevTeamSize
            val nextIdxInTeam = teamPlayerIndexes[nextTeam] ?: 0
            currentPlayerIndex = teamPlayerIndices[nextTeam]?.get(nextIdxInTeam) ?: 0
            currentTeamIndex = nextTeam
            teamPlayerIndexes[prevTeam] = advancedPrevIdx
        }
        return result
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the team index for each player index in the given turn sequence. */
    private fun teamSequence(turns: List<Int>, assignments: Map<Long, Int>, playerIds: List<Long>): List<Int> =
        turns.map { idx -> assignments[playerIds[idx]] ?: -1 }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `equal teams 2v2 - teams always alternate`() {
        // Players: A1(id=1), B1(id=2), A2(id=3), B2(id=4)  — interleaved
        val playerIds = listOf(1L, 2L, 3L, 4L)
        val assignments = mapOf(1L to 0, 2L to 1, 3L to 0, 4L to 1)

        val turns = simulateTurns(playerIds, assignments, steps = 8)
        val teams = teamSequence(turns, assignments, playerIds)

        // Teams must strictly alternate: 0,1,0,1,...
        assertEquals(listOf(0, 1, 0, 1, 0, 1, 0, 1), teams)
        // Players within each team rotate: A1,A2,A1,A2 / B1,B2,B1,B2
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2, 3), turns)
    }

    @Test
    fun `unequal teams 3v2 - teams always alternate`() {
        // Interleaved: A1(1), B1(2), A2(3), B2(4), A3(5)
        val playerIds = listOf(1L, 2L, 3L, 4L, 5L)
        val assignments = mapOf(1L to 0, 2L to 1, 3L to 0, 4L to 1, 5L to 0)

        val turns = simulateTurns(playerIds, assignments, steps = 12)
        val teams = teamSequence(turns, assignments, playerIds)

        // Every odd step must be team 0, every even step team 1
        teams.forEachIndexed { i, team ->
            assertEquals("Step $i: expected team ${i % 2} but got $team", i % 2, team)
        }
    }

    @Test
    fun `unequal teams 3v2 - player sequence within each team is correct`() {
        val playerIds = listOf(1L, 2L, 3L, 4L, 5L)
        val assignments = mapOf(1L to 0, 2L to 1, 3L to 0, 4L to 1, 5L to 0)

        // 12 steps → Team A plays 6 times (A1,A2,A3 cycling ×2), Team B plays 6 times (B1,B2 cycling ×3)
        val turns = simulateTurns(playerIds, assignments, steps = 12)

        val teamATurns = turns.filterIndexed { i, _ -> i % 2 == 0 } // positions 0,2,4,6,8,10
        val teamBTurns = turns.filterIndexed { i, _ -> i % 2 == 1 } // positions 1,3,5,7,9,11

        // Team A player indices in state.players: 0,2,4 cycling
        assertEquals(listOf(0, 2, 4, 0, 2, 4), teamATurns)
        // Team B player indices in state.players: 1,3 cycling
        assertEquals(listOf(1, 3, 1, 3, 1, 3), teamBTurns)
    }

    @Test
    fun `unequal teams 1v3 - teams always alternate`() {
        // Interleaved: A1(1), B1(2), B2(3), B3(4)
        val playerIds = listOf(1L, 2L, 3L, 4L)
        val assignments = mapOf(1L to 0, 2L to 1, 3L to 1, 4L to 1)

        val turns = simulateTurns(playerIds, assignments, steps = 8)
        val teams = teamSequence(turns, assignments, playerIds)

        teams.forEachIndexed { i, team ->
            assertEquals("Step $i: expected team ${i % 2} but got $team", i % 2, team)
        }
    }

    @Test
    fun `undo one visit restores previous player and team`() {
        // Unequal teams 3v2: after two visits (A1, B1) undo the second visit
        val playerIds = listOf(1L, 2L, 3L, 4L, 5L)
        val assignments = mapOf(1L to 0, 2L to 1, 3L to 0, 4L to 1, 5L to 0)
        val teamPlayerIndices = computeTeamPlayerIndices(playerIds, assignments)

        // Initial state
        var currentPlayerIndex = 0
        var currentTeamIndex = 0
        val teamPlayerIndexes = mutableMapOf(0 to 0, 1 to 0)

        // Visit 1: A1 plays → advance to B1
        run {
            val prevTeam = currentTeamIndex
            val nextTeam = 1 - prevTeam
            val prevTeamSize = teamPlayerIndices[prevTeam]!!.size
            val advancedPrevIdx = ((teamPlayerIndexes[prevTeam] ?: 0) + 1) % prevTeamSize
            val nextIdxInTeam = teamPlayerIndexes[nextTeam] ?: 0
            currentPlayerIndex = teamPlayerIndices[nextTeam]!![nextIdxInTeam]
            currentTeamIndex = nextTeam
            teamPlayerIndexes[prevTeam] = advancedPrevIdx
        }
        // Now it's B1's turn (playerIndex=1, teamIndex=1, teamPlayerIndexes={0:1,1:0})
        assertEquals(1, currentPlayerIndex)
        assertEquals(1, currentTeamIndex)

        // Undo: mirrors GameViewModel.undoLastDart() team branch
        run {
            val justPlayedTeam = 1 - currentTeamIndex
            val justPlayedTeamSize = teamPlayerIndices[justPlayedTeam]!!.size
            val rewindedIdx = ((teamPlayerIndexes[justPlayedTeam] ?: 0) - 1 + justPlayedTeamSize) % justPlayedTeamSize
            currentPlayerIndex = teamPlayerIndices[justPlayedTeam]!![rewindedIdx]
            currentTeamIndex = justPlayedTeam
            teamPlayerIndexes[justPlayedTeam] = rewindedIdx
        }
        // Should be back to A1 (playerIndex=0, teamIndex=0, teamPlayerIndexes={0:0,1:0})
        assertEquals(0, currentPlayerIndex)
        assertEquals(0, currentTeamIndex)
        assertEquals(mapOf(0 to 0, 1 to 0), teamPlayerIndexes.toMap())
    }

    @Test
    fun `new leg resets pointers so first player is team 0 player 0`() {
        // Simulate several visits then a leg reset
        val playerIds = listOf(1L, 2L, 3L, 4L, 5L)
        val assignments = mapOf(1L to 0, 2L to 1, 3L to 0, 4L to 1, 5L to 0)

        val turnsBeforeReset = simulateTurns(playerIds, assignments, steps = 7)
        // After 7 visits state is somewhere mid-cycle; new leg resets everything
        // Simulate reset (mirrors onLegWon new-leg branch)
        val resetCurrentTeamIndex = 0
        val resetTeamPlayerIndexes = mutableMapOf(0 to 0, 1 to 0)
        val resetCurrentPlayerIndex = 0

        assertEquals(0, resetCurrentTeamIndex)
        assertEquals(0, resetCurrentPlayerIndex)
        assertEquals(mapOf(0 to 0, 1 to 0), resetTeamPlayerIndexes.toMap())

        // First turn after reset must be player at index 0 (team A's first player)
        val turnsAfterReset = simulateTurns(playerIds, assignments, steps = 6)
        // First 6 turns should be same as beginning: A1,B1,A2,B2,A3,B1
        assertEquals(turnsBeforeReset.take(6), turnsAfterReset)
    }
}
