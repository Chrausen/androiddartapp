package com.clubdarts.data.repository

import com.clubdarts.data.db.AppDatabase
import com.clubdarts.data.db.dao.*
import com.clubdarts.data.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BackupRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var playerDao: PlayerDao
    private lateinit var gameDao: GameDao
    private lateinit var legDao: LegDao
    private lateinit var throwDao: ThrowDao
    private lateinit var appSettingsDao: AppSettingsDao
    private lateinit var eloMatchDao: EloMatchDao
    private lateinit var eloMatchEntryDao: EloMatchEntryDao
    private lateinit var trainingSessionDao: TrainingSessionDao
    private lateinit var trainingThrowDao: TrainingThrowDao
    private lateinit var repo: BackupRepository

    @Before
    fun setUp() {
        db                  = mockk(relaxed = true)
        playerDao           = mockk(relaxed = true)
        gameDao             = mockk(relaxed = true)
        legDao              = mockk(relaxed = true)
        throwDao            = mockk(relaxed = true)
        appSettingsDao      = mockk(relaxed = true)
        eloMatchDao         = mockk(relaxed = true)
        eloMatchEntryDao    = mockk(relaxed = true)
        trainingSessionDao  = mockk(relaxed = true)
        trainingThrowDao    = mockk(relaxed = true)

        repo = BackupRepository(
            db, playerDao, gameDao, legDao, throwDao,
            appSettingsDao, eloMatchDao, eloMatchEntryDao,
            trainingSessionDao, trainingThrowDao
        )

        // Make withTransaction execute its lambda immediately
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            (firstArg<suspend () -> Any?>())()
        }
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @Test
    fun `exportJson contains all required top-level keys`() = runTest {
        stubAllEmpty()
        val json = repo.exportJson()
        val root = JSONObject(json)
        listOf(
            "backupVersion", "schemaVersion", "exportedAt",
            "players", "games", "gamePlayers", "legs", "throws",
            "appSettings", "eloMatches", "eloMatchEntries",
            "trainingSessions", "trainingThrows"
        ).forEach { key ->
            assertTrue("Missing key: $key", root.has(key))
        }
    }

    @Test
    fun `exportJson serializes players correctly`() = runTest {
        val player = Player(id = 7, name = "Alice", elo = 1025.5, matchesPlayed = 3, wins = 2, losses = 1, createdAt = 1000L)
        coEvery { playerDao.getAllPlayersList() } returns listOf(player)
        stubAllEmptyExcept("players")

        val root = JSONObject(repo.exportJson())
        val arr = root.getJSONArray("players")
        assertEquals(1, arr.length())
        val p = arr.getJSONObject(0)
        assertEquals(7L, p.getLong("id"))
        assertEquals("Alice", p.getString("name"))
        assertEquals(1025.5, p.getDouble("elo"), 0.001)
    }

    @Test
    fun `exportJson includes correct backupVersion and schemaVersion`() = runTest {
        stubAllEmpty()
        val root = JSONObject(repo.exportJson())
        assertEquals(BackupRepository.BACKUP_VERSION, root.getInt("backupVersion"))
        assertEquals(BackupRepository.SCHEMA_VERSION, root.getInt("schemaVersion"))
    }

    // ── Import ────────────────────────────────────────────────────────────────

    @Test
    fun `importJson calls deleteAll on every table before inserting`() = runTest {
        stubAllEmpty()
        val json = repo.exportJson()
        repo.importJson(json)

        coVerify { playerDao.deleteAll() }
        coVerify { gameDao.deleteAll() }
        coVerify { legDao.deleteAll() }
        coVerify { throwDao.deleteAll() }
        coVerify { appSettingsDao.deleteAll() }
        coVerify { eloMatchDao.deleteAll() }
        coVerify { eloMatchEntryDao.deleteAll() }
        coVerify { trainingSessionDao.deleteAll() }
        coVerify { trainingThrowDao.deleteAll() }
    }

    @Test
    fun `importJson round-trips a player`() = runTest {
        val original = Player(id = 42, name = "Bob", elo = 980.0, matchesPlayed = 5, wins = 2, losses = 3, createdAt = 999L)
        coEvery { playerDao.getAllPlayersList() } returns listOf(original)
        stubAllEmptyExcept("players")

        val json = repo.exportJson()

        // reset stubs so import path is clean
        stubAllEmpty()
        repo.importJson(json)

        val slot = slot<List<Player>>()
        coVerify { playerDao.insertAll(capture(slot)) }
        val restored = slot.captured.first()
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.elo, restored.elo, 0.001)
    }

    @Test(expected = BackupException::class)
    fun `importJson throws BackupException for non-JSON input`() = runTest {
        repo.importJson("this is not json at all")
    }

    @Test(expected = BackupException::class)
    fun `importJson throws BackupException when backupVersion key is missing`() = runTest {
        val noVersion = JSONObject().apply {
            put("players", org.json.JSONArray())
        }.toString()
        repo.importJson(noVersion)
    }

    @Test
    fun `parseSharedPreferences returns null for missing section`() {
        stubAllEmpty()
        val json = JSONObject().apply { put("backupVersion", 1) }.toString()
        assertNull(repo.parseSharedPreferences(json))
    }

    @Test
    fun `parseSharedPreferences returns preferences when present`() {
        val json = JSONObject().apply {
            put("backupVersion", 1)
            put("sharedPreferences", JSONObject().apply {
                put("app_language", "de")
                put("animations_enabled", false)
            })
        }.toString()
        val prefs = repo.parseSharedPreferences(json)
        assertNotNull(prefs)
        assertEquals("de", prefs!!.getString("app_language"))
        assertFalse(prefs.getBoolean("animations_enabled"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun stubAllEmpty() {
        coEvery { playerDao.getAllPlayersList() } returns emptyList()
        coEvery { gameDao.getAllGamesList() } returns emptyList()
        coEvery { gameDao.getAllGamePlayers() } returns emptyList()
        coEvery { legDao.getAll() } returns emptyList()
        coEvery { throwDao.getAll() } returns emptyList()
        coEvery { appSettingsDao.getAll() } returns emptyList()
        coEvery { eloMatchDao.getAll() } returns emptyList()
        coEvery { eloMatchEntryDao.getAll() } returns emptyList()
        coEvery { trainingSessionDao.getAll() } returns emptyList()
        coEvery { trainingThrowDao.getAll() } returns emptyList()
    }

    private fun stubAllEmptyExcept(vararg skip: String) {
        if ("players" !in skip)          coEvery { playerDao.getAllPlayersList() } returns emptyList()
        if ("games" !in skip)            coEvery { gameDao.getAllGamesList() } returns emptyList()
        if ("gamePlayers" !in skip)      coEvery { gameDao.getAllGamePlayers() } returns emptyList()
        if ("legs" !in skip)             coEvery { legDao.getAll() } returns emptyList()
        if ("throws" !in skip)           coEvery { throwDao.getAll() } returns emptyList()
        if ("appSettings" !in skip)      coEvery { appSettingsDao.getAll() } returns emptyList()
        if ("eloMatches" !in skip)       coEvery { eloMatchDao.getAll() } returns emptyList()
        if ("eloMatchEntries" !in skip)  coEvery { eloMatchEntryDao.getAll() } returns emptyList()
        if ("trainingSessions" !in skip) coEvery { trainingSessionDao.getAll() } returns emptyList()
        if ("trainingThrows" !in skip)   coEvery { trainingThrowDao.getAll() } returns emptyList()
    }
}
