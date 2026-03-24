package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.PlayerDao
import com.clubdarts.data.model.Player
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit tests for [PlayerRepository] using MockK-mocked [PlayerDao]. */
class PlayerRepositoryTest {

    private lateinit var dao: PlayerDao
    private lateinit var repo: PlayerRepository

    @Before
    fun setUp() {
        dao  = mockk(relaxed = true)
        repo = PlayerRepository(dao)
    }

    private fun player(id: Long, name: String = "P$id") =
        Player(id = id, name = name)

    // -----------------------------------------------------------------------
    // getRecentPlayers — ordering + filtering
    // -----------------------------------------------------------------------

    @Test
    fun `getRecentPlayers preserves the order of the given ID list`() = runTest {
        val p1 = player(1); val p2 = player(2); val p3 = player(3)
        coEvery { dao.getPlayersByIds(any()) } returns listOf(p1, p2, p3)

        // Request order: 3 → 1 → 2
        val result = repo.getRecentPlayers(listOf(3L, 1L, 2L))
        assertEquals(listOf(p3, p1, p2), result)
    }

    @Test
    fun `getRecentPlayers skips IDs not found in the database`() = runTest {
        val p1 = player(1)
        coEvery { dao.getPlayersByIds(any()) } returns listOf(p1)

        val result = repo.getRecentPlayers(listOf(99L, 1L, 42L))
        assertEquals(listOf(p1), result)
    }

    @Test
    fun `getRecentPlayers returns empty list when IDs list is empty`() = runTest {
        val result = repo.getRecentPlayers(emptyList())
        assertTrue(result.isEmpty())
        // DAO must NOT be called for an empty list
        coVerify(exactly = 0) { dao.getPlayersByIds(any()) }
    }

    @Test
    fun `getRecentPlayers returns empty when none of the IDs match`() = runTest {
        coEvery { dao.getPlayersByIds(any()) } returns emptyList()
        val result = repo.getRecentPlayers(listOf(5L, 6L, 7L))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getRecentPlayers handles single ID`() = runTest {
        val p = player(42)
        coEvery { dao.getPlayersByIds(any()) } returns listOf(p)
        val result = repo.getRecentPlayers(listOf(42L))
        assertEquals(listOf(p), result)
    }

    // -----------------------------------------------------------------------
    // Simple delegation — verifies repository proxies to DAO
    // -----------------------------------------------------------------------

    @Test
    fun `getPlayersByIds delegates to DAO`() = runTest {
        val expected = listOf(player(1), player(2))
        coEvery { dao.getPlayersByIds(listOf(1L, 2L)) } returns expected
        assertEquals(expected, repo.getPlayersByIds(listOf(1L, 2L)))
    }

    @Test
    fun `getPlayerById returns player when found`() = runTest {
        val p = player(7)
        coEvery { dao.getPlayerById(7L) } returns p
        assertEquals(p, repo.getPlayerById(7L))
    }

    @Test
    fun `getPlayerById returns null when not found`() = runTest {
        coEvery { dao.getPlayerById(any()) } returns null
        assertEquals(null, repo.getPlayerById(99L))
    }

    @Test
    fun `insertPlayer delegates to DAO and returns generated ID`() = runTest {
        val p = player(0, "Alice")
        coEvery { dao.insertPlayer(p) } returns 42L
        assertEquals(42L, repo.insertPlayer(p))
    }

    @Test
    fun `updatePlayer delegates to DAO`() = runTest {
        val p = player(1, "Bob")
        repo.updatePlayer(p)
        coVerify { dao.updatePlayer(p) }
    }

    @Test
    fun `deletePlayer delegates to DAO`() = runTest {
        val p = player(3)
        repo.deletePlayer(p)
        coVerify { dao.deletePlayer(p) }
    }

    @Test
    fun `getPlayerCount delegates to DAO`() = runTest {
        coEvery { dao.getPlayerCount() } returns 7
        assertEquals(7, repo.getPlayerCount())
    }
}
