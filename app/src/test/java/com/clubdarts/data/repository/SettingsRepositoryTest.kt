package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.AppSettingsDao
import com.clubdarts.data.model.AppSettings
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.SettingsDefaults
import com.clubdarts.data.model.SettingsKeys
import com.clubdarts.data.model.TtsPhrase
import com.clubdarts.data.model.TtsScoreSetting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Unit tests for [SettingsRepository] using a MockK-mocked [AppSettingsDao]. */
class SettingsRepositoryTest {

    private lateinit var dao: AppSettingsDao
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        dao  = mockk(relaxed = true)
        repo = SettingsRepository(dao)
    }

    /** Convenience: make [dao.get] return [value] for any key. */
    private fun stubGet(value: String?) {
        coEvery { dao.get(any()) } returns value?.let { AppSettings("key", it) }
    }

    /** Convenience: stub a specific key → value. */
    private fun stubKey(key: String, value: String?) {
        coEvery { dao.get(key) } returns value?.let { AppSettings(key, it) }
    }

    // -----------------------------------------------------------------------
    // get / set
    // -----------------------------------------------------------------------

    @Test
    fun `get returns dao value when present`() = runTest {
        stubGet("hello")
        assertEquals("hello", repo.get("any_key", "default"))
    }

    @Test
    fun `get returns default when dao returns null`() = runTest {
        stubGet(null)
        assertEquals("default", repo.get("any_key", "default"))
    }

    // -----------------------------------------------------------------------
    // getLastStartScore
    // -----------------------------------------------------------------------

    @Test
    fun `getLastStartScore parses stored integer`() = runTest {
        stubKey(SettingsKeys.LAST_START_SCORE, "501")
        assertEquals(501, repo.getLastStartScore())
    }

    @Test
    fun `getLastStartScore falls back to 301 when value is not a number`() = runTest {
        stubKey(SettingsKeys.LAST_START_SCORE, "garbage")
        assertEquals(301, repo.getLastStartScore())
    }

    @Test
    fun `getLastStartScore uses default string when key missing`() = runTest {
        stubKey(SettingsKeys.LAST_START_SCORE, null)
        // SettingsDefaults.START_SCORE = "301"
        assertEquals(301, repo.getLastStartScore())
    }

    // -----------------------------------------------------------------------
    // getLastCheckoutRule
    // -----------------------------------------------------------------------

    @Test
    fun `getLastCheckoutRule parses DOUBLE`() = runTest {
        stubKey(SettingsKeys.LAST_CHECKOUT_RULE, "DOUBLE")
        assertEquals(CheckoutRule.DOUBLE, repo.getLastCheckoutRule())
    }

    @Test
    fun `getLastCheckoutRule parses STRAIGHT`() = runTest {
        stubKey(SettingsKeys.LAST_CHECKOUT_RULE, "STRAIGHT")
        assertEquals(CheckoutRule.STRAIGHT, repo.getLastCheckoutRule())
    }

    @Test
    fun `getLastCheckoutRule defaults to DOUBLE on unknown value`() = runTest {
        stubKey(SettingsKeys.LAST_CHECKOUT_RULE, "INVALID_RULE")
        assertEquals(CheckoutRule.DOUBLE, repo.getLastCheckoutRule())
    }

    // -----------------------------------------------------------------------
    // getLastLegsToWin
    // -----------------------------------------------------------------------

    @Test
    fun `getLastLegsToWin parses stored value`() = runTest {
        stubKey(SettingsKeys.LAST_LEGS_TO_WIN, "3")
        assertEquals(3, repo.getLastLegsToWin())
    }

    @Test
    fun `getLastLegsToWin defaults to 1 on bad value`() = runTest {
        stubKey(SettingsKeys.LAST_LEGS_TO_WIN, "bad")
        assertEquals(1, repo.getLastLegsToWin())
    }

    // -----------------------------------------------------------------------
    // getLastRandomOrder
    // -----------------------------------------------------------------------

    @Test
    fun `getLastRandomOrder returns true`() = runTest {
        stubKey(SettingsKeys.LAST_RANDOM_ORDER, "true")
        assertTrue(repo.getLastRandomOrder())
    }

    @Test
    fun `getLastRandomOrder returns false for false string`() = runTest {
        stubKey(SettingsKeys.LAST_RANDOM_ORDER, "false")
        assertFalse(repo.getLastRandomOrder())
    }

    // -----------------------------------------------------------------------
    // getRecentPlayerIds
    // -----------------------------------------------------------------------

    @Test
    fun `getRecentPlayerIds parses comma-separated longs`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "1,2,3")
        assertEquals(listOf(1L, 2L, 3L), repo.getRecentPlayerIds())
    }

    @Test
    fun `getRecentPlayerIds returns empty list for blank string`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "")
        assertTrue(repo.getRecentPlayerIds().isEmpty())
    }

    @Test
    fun `getRecentPlayerIds skips non-numeric tokens`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "1,abc,3")
        assertEquals(listOf(1L, 3L), repo.getRecentPlayerIds())
    }

    @Test
    fun `getRecentPlayerIds trims whitespace around tokens`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, " 4 , 5 , 6 ")
        assertEquals(listOf(4L, 5L, 6L), repo.getRecentPlayerIds())
    }

    @Test
    fun `getRecentPlayerIds returns empty when key is absent`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, null)
        // Default is SettingsDefaults.RECENT_IDS = "" → blank → empty list
        assertTrue(repo.getRecentPlayerIds().isEmpty())
    }

    // -----------------------------------------------------------------------
    // addRecentPlayer
    // -----------------------------------------------------------------------

    @Test
    fun `addRecentPlayer prepends ID to empty list`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "")
        val savedSlot = slot<AppSettings>()
        coEvery { dao.set(capture(savedSlot)) } returns Unit

        repo.addRecentPlayer(7L)

        assertEquals(SettingsKeys.RECENT_PLAYER_IDS, savedSlot.captured.key)
        assertEquals("7", savedSlot.captured.value)
    }

    @Test
    fun `addRecentPlayer prepends to existing list`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "2,3,4")
        val savedSlot = slot<AppSettings>()
        coEvery { dao.set(capture(savedSlot)) } returns Unit

        repo.addRecentPlayer(1L)

        assertEquals("1,2,3,4", savedSlot.captured.value)
    }

    @Test
    fun `addRecentPlayer deduplicates — moves existing ID to front`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "1,2,3")
        val savedSlot = slot<AppSettings>()
        coEvery { dao.set(capture(savedSlot)) } returns Unit

        repo.addRecentPlayer(2L)

        assertEquals("2,1,3", savedSlot.captured.value)
    }

    @Test
    fun `addRecentPlayer trims list to max 5 entries`() = runTest {
        stubKey(SettingsKeys.RECENT_PLAYER_IDS, "1,2,3,4,5")
        val savedSlot = slot<AppSettings>()
        coEvery { dao.set(capture(savedSlot)) } returns Unit

        repo.addRecentPlayer(6L)

        val ids = savedSlot.captured.value.split(",")
        assertEquals(5, ids.size)
        assertEquals("6", ids.first())
    }

    // -----------------------------------------------------------------------
    // getTtsScoreSettings / saveTtsScoreSettings
    // -----------------------------------------------------------------------

    @Test
    fun `getTtsScoreSettings returns empty list for empty JSON array`() = runTest {
        stubKey(SettingsKeys.TTS_SCORE_PHRASES, "[]")
        assertTrue(repo.getTtsScoreSettings().isEmpty())
    }

    @Test
    fun `getTtsScoreSettings returns empty list on malformed JSON`() = runTest {
        stubKey(SettingsKeys.TTS_SCORE_PHRASES, "not-json")
        assertTrue(repo.getTtsScoreSettings().isEmpty())
    }

    @Test
    fun `getTtsScoreSettings parses valid JSON correctly`() = runTest {
        val json = """[{"score":180,"phrases":[{"before":"Amazing","after":"score"}]}]"""
        stubKey(SettingsKeys.TTS_SCORE_PHRASES, json)

        val result = repo.getTtsScoreSettings()
        assertEquals(1, result.size)
        assertEquals(180, result[0].score)
        assertEquals(1, result[0].phrases.size)
        assertEquals(TtsPhrase("Amazing", "score"), result[0].phrases[0])
    }

    @Test
    fun `saveTtsScoreSettings serialises and persists settings`() = runTest {
        val settings = listOf(
            TtsScoreSetting(
                score = 100,
                phrases = listOf(TtsPhrase("Ton", ""))
            )
        )
        val savedSlot = slot<AppSettings>()
        coEvery { dao.set(capture(savedSlot)) } returns Unit

        repo.saveTtsScoreSettings(settings)

        assertEquals(SettingsKeys.TTS_SCORE_PHRASES, savedSlot.captured.key)
        // Re-parse and verify round-trip
        stubKey(SettingsKeys.TTS_SCORE_PHRASES, savedSlot.captured.value)
        val roundTrip = repo.getTtsScoreSettings()
        assertEquals(1, roundTrip.size)
        assertEquals(100, roundTrip[0].score)
        assertEquals(TtsPhrase("Ton", ""), roundTrip[0].phrases[0])
    }

    @Test
    fun `saveTtsScoreSettings handles empty list`() = runTest {
        val savedSlot = slot<AppSettings>()
        coEvery { dao.set(capture(savedSlot)) } returns Unit

        repo.saveTtsScoreSettings(emptyList())

        assertEquals("[]", savedSlot.captured.value)
    }

    // -----------------------------------------------------------------------
    // Ranking settings
    // -----------------------------------------------------------------------

    @Test
    fun `getRankingEnabled returns true`() = runTest {
        stubKey(SettingsKeys.RANKING_ENABLED, "true")
        assertTrue(repo.getRankingEnabled())
    }

    @Test
    fun `getRankingEnabled returns true by default`() = runTest {
        stubKey(SettingsKeys.RANKING_ENABLED, null)
        // SettingsDefaults.RANKING_ENABLED = "true"
        assertTrue(repo.getRankingEnabled())
    }

    @Test
    fun `getRankingKFactor parses stored value`() = runTest {
        stubKey(SettingsKeys.RANKING_K_FACTOR, "64")
        assertEquals(64, repo.getRankingKFactor())
    }

    @Test
    fun `getRankingKFactor defaults to 32 on parse failure`() = runTest {
        stubKey(SettingsKeys.RANKING_K_FACTOR, "NaN")
        assertEquals(32, repo.getRankingKFactor())
    }

    @Test
    fun `getRankingStartScore parses stored value`() = runTest {
        stubKey(SettingsKeys.RANKING_START_SCORE, "701")
        assertEquals(701, repo.getRankingStartScore())
    }

    @Test
    fun `getRankingStartScore defaults to 501`() = runTest {
        stubKey(SettingsKeys.RANKING_START_SCORE, "bad")
        assertEquals(501, repo.getRankingStartScore())
    }

    @Test
    fun `getRankingCheckoutRule parses TRIPLE`() = runTest {
        stubKey(SettingsKeys.RANKING_CHECKOUT_RULE, "TRIPLE")
        assertEquals(CheckoutRule.TRIPLE, repo.getRankingCheckoutRule())
    }

    @Test
    fun `getRankingCheckoutRule defaults to DOUBLE on bad value`() = runTest {
        stubKey(SettingsKeys.RANKING_CHECKOUT_RULE, "WRONG")
        assertEquals(CheckoutRule.DOUBLE, repo.getRankingCheckoutRule())
    }

    @Test
    fun `getRankingLegsToWin parses stored value`() = runTest {
        stubKey(SettingsKeys.RANKING_LEGS_TO_WIN, "5")
        assertEquals(5, repo.getRankingLegsToWin())
    }

    @Test
    fun `getRankingLegsToWin defaults to 1 on bad value`() = runTest {
        stubKey(SettingsKeys.RANKING_LEGS_TO_WIN, "")
        assertEquals(1, repo.getRankingLegsToWin())
    }

    // -----------------------------------------------------------------------
    // setLastGameConfig
    // -----------------------------------------------------------------------

    @Test
    fun `setLastGameConfig persists all four values`() = runTest {
        repo.setLastGameConfig(501, CheckoutRule.STRAIGHT, 3, true)
        coVerify { dao.set(AppSettings(SettingsKeys.LAST_START_SCORE, "501")) }
        coVerify { dao.set(AppSettings(SettingsKeys.LAST_CHECKOUT_RULE, "STRAIGHT")) }
        coVerify { dao.set(AppSettings(SettingsKeys.LAST_LEGS_TO_WIN, "3")) }
        coVerify { dao.set(AppSettings(SettingsKeys.LAST_RANDOM_ORDER, "true")) }
    }

    // -----------------------------------------------------------------------
    // getShowHistory / setShowHistory
    // -----------------------------------------------------------------------

    @Test
    fun `getShowHistory returns false by default`() = runTest {
        stubKey(SettingsKeys.SHOW_HISTORY, null)
        assertFalse(repo.getShowHistory())
    }

    @Test
    fun `setShowHistory persists true`() = runTest {
        repo.setShowHistory(true)
        coVerify { dao.set(AppSettings(SettingsKeys.SHOW_HISTORY, "true")) }
    }

    // -----------------------------------------------------------------------
    // observe
    // -----------------------------------------------------------------------

    @Test
    fun `observe emits stored value`() = runTest {
        every { dao.observe("key") } returns flowOf(AppSettings("key", "stored"))
        assertEquals("stored", repo.observe("key", "default").first())
    }

    @Test
    fun `observe emits default when dao emits null`() = runTest {
        every { dao.observe("key") } returns flowOf(null)
        assertEquals("default", repo.observe("key", "default").first())
    }

    // -----------------------------------------------------------------------
    // getLastGameMode / setLastGameMode
    // -----------------------------------------------------------------------

    @Test
    fun `getLastGameMode returns stored value`() = runTest {
        stubKey(SettingsKeys.LAST_GAME_MODE, "TEAMS")
        assertEquals("TEAMS", repo.getLastGameMode())
    }

    @Test
    fun `getLastGameMode returns default when key absent`() = runTest {
        stubKey(SettingsKeys.LAST_GAME_MODE, null)
        assertEquals(SettingsDefaults.GAME_MODE, repo.getLastGameMode())
    }

    @Test
    fun `setLastGameMode persists value`() = runTest {
        repo.setLastGameMode("TEAMS")
        coVerify { dao.set(AppSettings(SettingsKeys.LAST_GAME_MODE, "TEAMS")) }
    }

    // -----------------------------------------------------------------------
    // getSoundEffectsMuted / setSoundEffectsMuted
    // -----------------------------------------------------------------------

    @Test
    fun `getSoundEffectsMuted returns true when stored`() = runTest {
        stubKey(SettingsKeys.SOUND_EFFECTS_MUTED, "true")
        assertTrue(repo.getSoundEffectsMuted())
    }

    @Test
    fun `getSoundEffectsMuted returns false by default`() = runTest {
        stubKey(SettingsKeys.SOUND_EFFECTS_MUTED, null)
        assertFalse(repo.getSoundEffectsMuted())
    }

    @Test
    fun `setSoundEffectsMuted persists value`() = runTest {
        repo.setSoundEffectsMuted(true)
        coVerify { dao.set(AppSettings(SettingsKeys.SOUND_EFFECTS_MUTED, "true")) }
    }

    // -----------------------------------------------------------------------
    // getSoundEffectsVolume / setSoundEffectsVolume
    // -----------------------------------------------------------------------

    @Test
    fun `getSoundEffectsVolume returns stored value`() = runTest {
        stubKey(SettingsKeys.SOUND_EFFECTS_VOLUME, "0.5")
        assertEquals(0.5f, repo.getSoundEffectsVolume(), 0.001f)
    }

    @Test
    fun `getSoundEffectsVolume defaults to 1f on bad value`() = runTest {
        stubKey(SettingsKeys.SOUND_EFFECTS_VOLUME, "bad")
        assertEquals(1f, repo.getSoundEffectsVolume(), 0.001f)
    }

    @Test
    fun `getSoundEffectsVolume clamps above 1f to 1f`() = runTest {
        stubKey(SettingsKeys.SOUND_EFFECTS_VOLUME, "2.0")
        assertEquals(1f, repo.getSoundEffectsVolume(), 0.001f)
    }

    @Test
    fun `setSoundEffectsVolume persists value`() = runTest {
        repo.setSoundEffectsVolume(0.75f)
        coVerify { dao.set(AppSettings(SettingsKeys.SOUND_EFFECTS_VOLUME, "0.75")) }
    }

    // -----------------------------------------------------------------------
    // Ranking setters
    // -----------------------------------------------------------------------

    @Test
    fun `setRankingEnabled persists value`() = runTest {
        repo.setRankingEnabled(true)
        coVerify { dao.set(AppSettings(SettingsKeys.RANKING_ENABLED, "true")) }
    }

    @Test
    fun `setRankingKFactor persists value`() = runTest {
        repo.setRankingKFactor(64)
        coVerify { dao.set(AppSettings(SettingsKeys.RANKING_K_FACTOR, "64")) }
    }

    @Test
    fun `setRankingStartScore persists value`() = runTest {
        repo.setRankingStartScore(1001)
        coVerify { dao.set(AppSettings(SettingsKeys.RANKING_START_SCORE, "1001")) }
    }

    @Test
    fun `setRankingCheckoutRule persists value`() = runTest {
        repo.setRankingCheckoutRule(CheckoutRule.STRAIGHT)
        coVerify { dao.set(AppSettings(SettingsKeys.RANKING_CHECKOUT_RULE, "STRAIGHT")) }
    }

    @Test
    fun `setRankingLegsToWin persists value`() = runTest {
        repo.setRankingLegsToWin(5)
        coVerify { dao.set(AppSettings(SettingsKeys.RANKING_LEGS_TO_WIN, "5")) }
    }

    // -----------------------------------------------------------------------
    // observe Flow methods
    // -----------------------------------------------------------------------

    @Test
    fun `observeRankingEnabled emits true when stored`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_ENABLED) } returns
            flowOf(AppSettings(SettingsKeys.RANKING_ENABLED, "true"))
        assertTrue(repo.observeRankingEnabled().first())
    }

    @Test
    fun `observeRankingEnabled emits true by default`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_ENABLED) } returns flowOf(null)
        assertTrue(repo.observeRankingEnabled().first())
    }

    @Test
    fun `observeRankingStartScore emits parsed value`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_START_SCORE) } returns
            flowOf(AppSettings(SettingsKeys.RANKING_START_SCORE, "701"))
        assertEquals(701, repo.observeRankingStartScore().first())
    }

    @Test
    fun `observeRankingStartScore emits 501 on bad value`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_START_SCORE) } returns flowOf(null)
        assertEquals(501, repo.observeRankingStartScore().first())
    }

    @Test
    fun `observeRankingCheckoutRule emits parsed rule`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_CHECKOUT_RULE) } returns
            flowOf(AppSettings(SettingsKeys.RANKING_CHECKOUT_RULE, "STRAIGHT"))
        assertEquals(CheckoutRule.STRAIGHT, repo.observeRankingCheckoutRule().first())
    }

    @Test
    fun `observeRankingCheckoutRule emits DOUBLE on bad value`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_CHECKOUT_RULE) } returns
            flowOf(AppSettings(SettingsKeys.RANKING_CHECKOUT_RULE, "BAD"))
        assertEquals(CheckoutRule.DOUBLE, repo.observeRankingCheckoutRule().first())
    }

    @Test
    fun `observeRankingLegsToWin emits parsed value`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_LEGS_TO_WIN) } returns
            flowOf(AppSettings(SettingsKeys.RANKING_LEGS_TO_WIN, "3"))
        assertEquals(3, repo.observeRankingLegsToWin().first())
    }

    @Test
    fun `observeRankingLegsToWin emits 1 by default`() = runTest {
        every { dao.observe(SettingsKeys.RANKING_LEGS_TO_WIN) } returns flowOf(null)
        assertEquals(1, repo.observeRankingLegsToWin().first())
    }

    @Test
    fun `observeTtsScoreSettings parses valid JSON`() = runTest {
        val json = """[{"score":180,"phrases":[{"before":"Max","after":""}]}]"""
        every { dao.observe(SettingsKeys.TTS_SCORE_PHRASES) } returns
            flowOf(AppSettings(SettingsKeys.TTS_SCORE_PHRASES, json))
        val result = repo.observeTtsScoreSettings().first()
        assertEquals(1, result.size)
        assertEquals(180, result[0].score)
        assertEquals(TtsPhrase("Max", ""), result[0].phrases[0])
    }

    @Test
    fun `observeTtsScoreSettings returns empty list on malformed JSON`() = runTest {
        every { dao.observe(SettingsKeys.TTS_SCORE_PHRASES) } returns
            flowOf(AppSettings(SettingsKeys.TTS_SCORE_PHRASES, "not-json"))
        assertTrue(repo.observeTtsScoreSettings().first().isEmpty())
    }
}
