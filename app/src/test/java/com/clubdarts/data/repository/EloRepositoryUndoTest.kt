package com.clubdarts.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.EloMatchDao
import com.clubdarts.data.db.dao.EloMatchEntryDao
import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.EloMatch
import com.clubdarts.data.model.EloMatchEntry
import com.clubdarts.data.model.Player
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [EloRepository.revertMatch] — verifies that ELO changes applied by a ranked
 * match are correctly reversed: player ratings and win/loss/matchesPlayed counters are restored to
 * their pre-match values, and the EloMatch + EloMatchEntry records are deleted.
 */
class EloRepositoryUndoTest {

    private lateinit var db: AppDatabase
    private lateinit var playerDao: PlayerDao
    private lateinit var eloMatchDao: EloMatchDao
    private lateinit var eloMatchEntryDao: EloMatchEntryDao
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repo: EloRepository

    @Before
    fun setUp() {
        db = mockk(relaxed = true)
        playerDao = mockk(relaxed = true)
        eloMatchDao = mockk(relaxed = true)
        eloMatchEntryDao = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Make withTransaction execute the block immediately (no real DB needed)
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            secondArg<suspend () -> Any?>().invoke()
        }

        repo = EloRepository(db, playerDao, eloMatchDao, eloMatchEntryDao, settingsRepository)
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun player(
        id: Long,
        elo: Double,
        matchesPlayed: Int = 5,
        wins: Int = 3,
        losses: Int = 2
    ) = Player(id = id, name = "P$id", elo = elo, matchesPlayed = matchesPlayed, wins = wins, losses = losses)

    private fun entry(
        matchId: Long,
        playerId: Long,
        eloBefore: Double,
        eloAfter: Double
    ) = EloMatchEntry(
        id = playerId,
        matchId = matchId,
        playerId = playerId,
        eloBefore = eloBefore,
        eloAfter = eloAfter,
        eloChange = eloAfter - eloBefore
    )

    // -----------------------------------------------------------------------
    // revertMatch — Elo restoration
    // -----------------------------------------------------------------------

    @Test
    fun `revertMatch restores winner elo to pre-match value`() = runTest {
        val matchId = 10L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0),  // winner
            entry(matchId, playerId = 2, eloBefore = 1000.0, eloAfter = 984.0)    // loser
        )
        val players = listOf(
            player(id = 1, elo = 1016.0, wins = 4, losses = 1),
            player(id = 2, elo = 984.0,  wins = 3, losses = 3)
        )
        val updatedSlot = slot<List<Player>>()

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(listOf(1L, 2L)) } returns players
        coEvery { playerDao.updateAll(capture(updatedSlot)) } just Runs

        repo.revertMatch(matchId)

        val winner = updatedSlot.captured.first { it.id == 1L }
        assertEquals(1000.0, winner.elo, 0.001)
    }

    @Test
    fun `revertMatch restores loser elo to pre-match value`() = runTest {
        val matchId = 10L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0),
            entry(matchId, playerId = 2, eloBefore = 1000.0, eloAfter = 984.0)
        )
        val players = listOf(
            player(id = 1, elo = 1016.0, wins = 4, losses = 1),
            player(id = 2, elo = 984.0,  wins = 3, losses = 3)
        )
        val updatedSlot = slot<List<Player>>()

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(listOf(1L, 2L)) } returns players
        coEvery { playerDao.updateAll(capture(updatedSlot)) } just Runs

        repo.revertMatch(matchId)

        val loser = updatedSlot.captured.first { it.id == 2L }
        assertEquals(1000.0, loser.elo, 0.001)
    }

    @Test
    fun `revertMatch decrements matchesPlayed for all players`() = runTest {
        val matchId = 11L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1100.0, eloAfter = 1116.0),
            entry(matchId, playerId = 2, eloBefore = 900.0,  eloAfter = 884.0)
        )
        val players = listOf(
            player(id = 1, elo = 1116.0, matchesPlayed = 6),
            player(id = 2, elo = 884.0,  matchesPlayed = 6)
        )
        val updatedSlot = slot<List<Player>>()

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(listOf(1L, 2L)) } returns players
        coEvery { playerDao.updateAll(capture(updatedSlot)) } just Runs

        repo.revertMatch(matchId)

        updatedSlot.captured.forEach { p ->
            assertEquals(5, p.matchesPlayed)
        }
    }

    @Test
    fun `revertMatch decrements wins for the winner`() = runTest {
        val matchId = 12L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0),  // winner (positive change)
            entry(matchId, playerId = 2, eloBefore = 1000.0, eloAfter = 984.0)
        )
        val players = listOf(
            player(id = 1, elo = 1016.0, wins = 4, losses = 1),
            player(id = 2, elo = 984.0,  wins = 3, losses = 3)
        )
        val updatedSlot = slot<List<Player>>()

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(listOf(1L, 2L)) } returns players
        coEvery { playerDao.updateAll(capture(updatedSlot)) } just Runs

        repo.revertMatch(matchId)

        val winner = updatedSlot.captured.first { it.id == 1L }
        assertEquals(3, winner.wins)  // 4 - 1 = 3
    }

    @Test
    fun `revertMatch decrements losses for the loser`() = runTest {
        val matchId = 13L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0),
            entry(matchId, playerId = 2, eloBefore = 1000.0, eloAfter = 984.0)   // loser (negative change)
        )
        val players = listOf(
            player(id = 1, elo = 1016.0, wins = 4, losses = 1),
            player(id = 2, elo = 984.0,  wins = 3, losses = 3)
        )
        val updatedSlot = slot<List<Player>>()

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(listOf(1L, 2L)) } returns players
        coEvery { playerDao.updateAll(capture(updatedSlot)) } just Runs

        repo.revertMatch(matchId)

        val loser = updatedSlot.captured.first { it.id == 2L }
        assertEquals(2, loser.losses)  // 3 - 1 = 2
    }

    @Test
    fun `revertMatch deletes EloMatchEntries for the match`() = runTest {
        val matchId = 14L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0),
            entry(matchId, playerId = 2, eloBefore = 1000.0, eloAfter = 984.0)
        )
        val players = listOf(
            player(id = 1, elo = 1016.0),
            player(id = 2, elo = 984.0)
        )

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(any()) } returns players

        repo.revertMatch(matchId)

        coVerify { eloMatchEntryDao.deleteByMatchId(matchId) }
    }

    @Test
    fun `revertMatch deletes the EloMatch record`() = runTest {
        val matchId = 15L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0),
            entry(matchId, playerId = 2, eloBefore = 1000.0, eloAfter = 984.0)
        )
        val players = listOf(
            player(id = 1, elo = 1016.0),
            player(id = 2, elo = 984.0)
        )

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(any()) } returns players

        repo.revertMatch(matchId)

        coVerify { eloMatchDao.deleteById(matchId) }
    }

    @Test
    fun `revertMatch never decrements matchesPlayed below zero`() = runTest {
        val matchId = 16L
        val entries = listOf(
            entry(matchId, playerId = 1, eloBefore = 1000.0, eloAfter = 1016.0)
        )
        // Player with matchesPlayed already at 0 (edge case)
        val players = listOf(player(id = 1, elo = 1016.0, matchesPlayed = 0))
        val updatedSlot = slot<List<Player>>()

        coEvery { eloMatchEntryDao.getEntriesForMatch(matchId) } returns entries
        coEvery { playerDao.getPlayersByIds(any()) } returns players
        coEvery { playerDao.updateAll(capture(updatedSlot)) } just Runs

        repo.revertMatch(matchId)

        assertEquals(0, updatedSlot.captured.first().matchesPlayed)
    }

    // -----------------------------------------------------------------------
    // recordMatch return type — includes matchId
    // -----------------------------------------------------------------------

    @Test
    fun `recordMatch result contains matchId from inserted EloMatch`() = runTest {
        coEvery { settingsRepository.getRankingKFactor() } returns 32
        val freshPlayers = listOf(
            player(id = 1, elo = 1000.0),
            player(id = 2, elo = 1000.0)
        )
        coEvery { playerDao.getPlayerById(1L) } returns freshPlayers[0]
        coEvery { playerDao.getPlayerById(2L) } returns freshPlayers[1]
        coEvery { eloMatchDao.insert(any()) } returns 99L
        coEvery { eloMatchEntryDao.insert(any()) } returns 1L

        val result = repo.recordMatch(freshPlayers, winnerId = 1L)

        assertEquals(99L, result.matchId)
    }

    @Test
    fun `recordMatch result changes map contains all player IDs`() = runTest {
        coEvery { settingsRepository.getRankingKFactor() } returns 32
        val freshPlayers = listOf(
            player(id = 1, elo = 1000.0),
            player(id = 2, elo = 1000.0)
        )
        coEvery { playerDao.getPlayerById(1L) } returns freshPlayers[0]
        coEvery { playerDao.getPlayerById(2L) } returns freshPlayers[1]
        coEvery { eloMatchDao.insert(any()) } returns 1L
        coEvery { eloMatchEntryDao.insert(any()) } returns 1L

        val result = repo.recordMatch(freshPlayers, winnerId = 1L)

        assertEquals(setOf(1L, 2L), result.changes.keys)
    }
}
