package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.AppSettingsDao
import com.clubdarts.data.model.AppSettings
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.CommentaryPhrases
import com.clubdarts.data.model.SettingsDefaults
import com.clubdarts.data.model.SettingsKeys
import com.clubdarts.data.model.TtsPhrase
import com.clubdarts.data.model.TtsScoreSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dao: AppSettingsDao
) {
    suspend fun get(key: String, default: String): String =
        dao.get(key)?.value ?: default

    suspend fun set(key: String, value: String) =
        dao.set(AppSettings(key, value))

    fun observe(key: String, default: String): Flow<String> =
        dao.observe(key).map { it?.value ?: default }

    suspend fun getLastStartScore(): Int =
        get(SettingsKeys.LAST_START_SCORE, SettingsDefaults.START_SCORE).toIntOrNull() ?: 301

    suspend fun getLastCheckoutRule(): CheckoutRule =
        try { CheckoutRule.valueOf(get(SettingsKeys.LAST_CHECKOUT_RULE, SettingsDefaults.CHECKOUT_RULE)) }
        catch (e: Exception) { CheckoutRule.DOUBLE }

    suspend fun getLastLegsToWin(): Int =
        get(SettingsKeys.LAST_LEGS_TO_WIN, SettingsDefaults.LEGS_TO_WIN).toIntOrNull() ?: 1

    suspend fun getLastRandomOrder(): Boolean =
        get(SettingsKeys.LAST_RANDOM_ORDER, SettingsDefaults.RANDOM_ORDER).toBoolean()

    suspend fun getRecentPlayerIds(): List<Long> {
        val raw = get(SettingsKeys.RECENT_PLAYER_IDS, SettingsDefaults.RECENT_IDS)
        return if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    suspend fun setLastGameConfig(score: Int, rule: CheckoutRule, legs: Int, random: Boolean) {
        set(SettingsKeys.LAST_START_SCORE, score.toString())
        set(SettingsKeys.LAST_CHECKOUT_RULE, rule.name)
        set(SettingsKeys.LAST_LEGS_TO_WIN, legs.toString())
        set(SettingsKeys.LAST_RANDOM_ORDER, random.toString())
    }

    suspend fun getLastGameMode(): String =
        get(SettingsKeys.LAST_GAME_MODE, SettingsDefaults.GAME_MODE)

    suspend fun setLastGameMode(modeString: String) =
        set(SettingsKeys.LAST_GAME_MODE, modeString)

    suspend fun getShowHistory(): Boolean =
        get(SettingsKeys.SHOW_HISTORY, SettingsDefaults.SHOW_HISTORY).toBoolean()

    suspend fun setShowHistory(value: Boolean) =
        set(SettingsKeys.SHOW_HISTORY, value.toString())

    suspend fun addRecentPlayer(playerId: Long) {
        val current = getRecentPlayerIds().toMutableList()
        current.remove(playerId)
        current.add(0, playerId)
        val trimmed = current.take(5)
        set(SettingsKeys.RECENT_PLAYER_IDS, trimmed.joinToString(","))
    }

    /**
     * Deserialises the TTS score-phrase customisations stored under [SettingsKeys.TTS_SCORE_PHRASES].
     *
     * Stored as a JSON array with the following schema:
     * ```json
     * [
     *   {
     *     "score": 100,
     *     "phrases": [
     *       { "before": "ton",  "after": "" },
     *       { "before": "100!", "after": "nice" }
     *     ]
     *   }
     * ]
     * ```
     * - `score`   — the visit total that triggers these phrases (e.g. 100, 180).
     * - `phrases` — one or more [TtsPhrase] objects; a random one is chosen on each announcement.
     * - `before`  — spoken *before* the score number; `after` — spoken *after* it.
     *
     * Returns an empty list on any parse failure so the game continues without announcements.
     *
     * @see saveTtsScoreSettings for the matching serialiser.
     */
    suspend fun getTtsScoreSettings(): List<TtsScoreSetting> {
        val raw = get(SettingsKeys.TTS_SCORE_PHRASES, "[]")
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val phrasesArr = obj.getJSONArray("phrases")
                val phrases = (0 until phrasesArr.length()).map { j ->
                    val p = phrasesArr.getJSONObject(j)
                    TtsPhrase(before = p.optString("before"), after = p.optString("after"))
                }
                TtsScoreSetting(score = obj.getInt("score"), phrases = phrases)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Serialises TTS score-phrase customisations to JSON. See [getTtsScoreSettings] for the schema. */
    suspend fun saveTtsScoreSettings(settings: List<TtsScoreSetting>) {
        val arr = JSONArray()
        settings.forEach { setting ->
            val obj = JSONObject()
            obj.put("score", setting.score)
            val phrasesArr = JSONArray()
            setting.phrases.forEach { phrase ->
                val p = JSONObject()
                p.put("before", phrase.before)
                p.put("after", phrase.after)
                phrasesArr.put(p)
            }
            obj.put("phrases", phrasesArr)
            arr.put(obj)
        }
        set(SettingsKeys.TTS_SCORE_PHRASES, arr.toString())
    }

    fun observeTtsScoreSettings(): Flow<List<TtsScoreSetting>> =
        observe(SettingsKeys.TTS_SCORE_PHRASES, "[]").map { raw ->
            try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    val phrasesArr = obj.getJSONArray("phrases")
                    val phrases = (0 until phrasesArr.length()).map { j ->
                        val p = phrasesArr.getJSONObject(j)
                        TtsPhrase(before = p.optString("before"), after = p.optString("after"))
                    }
                    TtsScoreSetting(score = obj.getInt("score"), phrases = phrases)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    // ---- Sound effects settings ----

    suspend fun getSoundEffectsMuted(): Boolean =
        get(SettingsKeys.SOUND_EFFECTS_MUTED, SettingsDefaults.SOUND_EFFECTS_MUTED).toBoolean()

    suspend fun setSoundEffectsMuted(value: Boolean) =
        set(SettingsKeys.SOUND_EFFECTS_MUTED, value.toString())

    suspend fun getSoundEffectsVolume(): Float =
        get(SettingsKeys.SOUND_EFFECTS_VOLUME, SettingsDefaults.SOUND_EFFECTS_VOLUME)
            .toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f

    suspend fun setSoundEffectsVolume(value: Float) =
        set(SettingsKeys.SOUND_EFFECTS_VOLUME, value.toString())

    // ---- Ranking system settings ----

    fun observeRankingEnabled(): Flow<Boolean> =
        observe(SettingsKeys.RANKING_ENABLED, SettingsDefaults.RANKING_ENABLED).map { it.toBoolean() }

    suspend fun getRankingEnabled(): Boolean =
        get(SettingsKeys.RANKING_ENABLED, SettingsDefaults.RANKING_ENABLED).toBoolean()

    suspend fun setRankingEnabled(v: Boolean) =
        set(SettingsKeys.RANKING_ENABLED, v.toString())

    suspend fun getRankingKFactor(): Int =
        get(SettingsKeys.RANKING_K_FACTOR, SettingsDefaults.RANKING_K_FACTOR).toIntOrNull() ?: 32

    suspend fun setRankingKFactor(v: Int) =
        set(SettingsKeys.RANKING_K_FACTOR, v.toString())

    suspend fun getRankingStartScore(): Int =
        get(SettingsKeys.RANKING_START_SCORE, SettingsDefaults.RANKING_START_SCORE).toIntOrNull() ?: 501

    fun observeRankingStartScore(): Flow<Int> =
        observe(SettingsKeys.RANKING_START_SCORE, SettingsDefaults.RANKING_START_SCORE)
            .map { it.toIntOrNull() ?: 501 }

    suspend fun setRankingStartScore(v: Int) =
        set(SettingsKeys.RANKING_START_SCORE, v.toString())

    suspend fun getRankingCheckoutRule(): CheckoutRule =
        try { CheckoutRule.valueOf(get(SettingsKeys.RANKING_CHECKOUT_RULE, SettingsDefaults.RANKING_CHECKOUT_RULE)) }
        catch (e: Exception) { CheckoutRule.DOUBLE }

    fun observeRankingCheckoutRule(): Flow<CheckoutRule> =
        observe(SettingsKeys.RANKING_CHECKOUT_RULE, SettingsDefaults.RANKING_CHECKOUT_RULE)
            .map { try { CheckoutRule.valueOf(it) } catch (e: Exception) { CheckoutRule.DOUBLE } }

    suspend fun setRankingCheckoutRule(v: CheckoutRule) =
        set(SettingsKeys.RANKING_CHECKOUT_RULE, v.name)

    suspend fun getRankingLegsToWin(): Int =
        get(SettingsKeys.RANKING_LEGS_TO_WIN, SettingsDefaults.RANKING_LEGS_TO_WIN).toIntOrNull() ?: 1

    fun observeRankingLegsToWin(): Flow<Int> =
        observe(SettingsKeys.RANKING_LEGS_TO_WIN, SettingsDefaults.RANKING_LEGS_TO_WIN)
            .map { it.toIntOrNull() ?: 1 }

    suspend fun setRankingLegsToWin(v: Int) =
        set(SettingsKeys.RANKING_LEGS_TO_WIN, v.toString())

    // ---- Random commentary settings ----

    suspend fun getRandomCommentaryEnabled(): Boolean =
        get(SettingsKeys.RANDOM_COMMENTARY_ENABLED, SettingsDefaults.RANDOM_COMMENTARY_ENABLED).toBoolean()

    suspend fun setRandomCommentaryEnabled(v: Boolean) =
        set(SettingsKeys.RANDOM_COMMENTARY_ENABLED, v.toString())

    fun observeRandomCommentaryEnabled(): Flow<Boolean> =
        observe(SettingsKeys.RANDOM_COMMENTARY_ENABLED, SettingsDefaults.RANDOM_COMMENTARY_ENABLED)
            .map { it.toBoolean() }

    suspend fun getCommentaryPhrases(): CommentaryPhrases =
        parseCommentaryPhrases(get(SettingsKeys.COMMENTARY_PHRASES, ""))

    suspend fun saveCommentaryPhrases(phrases: CommentaryPhrases) =
        set(SettingsKeys.COMMENTARY_PHRASES, serializeCommentaryPhrases(phrases))

    fun observeCommentaryPhrases(): Flow<CommentaryPhrases> =
        observe(SettingsKeys.COMMENTARY_PHRASES, "").map { parseCommentaryPhrases(it) }

    private fun parseCommentaryPhrases(raw: String): CommentaryPhrases {
        if (raw.isBlank()) return CommentaryPhrases.DEFAULT
        return try {
            val obj = JSONObject(raw)
            fun JSONObject.strings(key: String): List<String> {
                val arr = optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).map { arr.getString(it) }
            }
            val bad    = obj.strings("bad").ifEmpty { CommentaryPhrases.DEFAULT.bad }
            val normal = obj.strings("normal").ifEmpty { CommentaryPhrases.DEFAULT.normal }
            val good   = obj.strings("good").ifEmpty { CommentaryPhrases.DEFAULT.good }
            CommentaryPhrases(bad = bad, normal = normal, good = good)
        } catch (e: Exception) {
            CommentaryPhrases.DEFAULT
        }
    }

    private fun serializeCommentaryPhrases(phrases: CommentaryPhrases): String {
        val obj = JSONObject()
        fun List<String>.toJsonArray() = JSONArray().also { arr -> forEach { arr.put(it) } }
        obj.put("bad",    phrases.bad.toJsonArray())
        obj.put("normal", phrases.normal.toJsonArray())
        obj.put("good",   phrases.good.toJsonArray())
        return obj.toString()
    }

    // ---- Fun mode settings ----

    suspend fun getFunModeEnabled(): Boolean =
        get(SettingsKeys.FUN_MODE_ENABLED, SettingsDefaults.FUN_MODE_ENABLED).toBoolean()

    suspend fun setFunModeEnabled(v: Boolean) =
        set(SettingsKeys.FUN_MODE_ENABLED, v.toString())

    suspend fun getFunModeIntervalRounds(): Int =
        get(SettingsKeys.FUN_MODE_INTERVAL_ROUNDS, SettingsDefaults.FUN_MODE_INTERVAL_ROUNDS)
            .toIntOrNull()?.coerceIn(1, 9) ?: 1

    suspend fun setFunModeIntervalRounds(v: Int) =
        set(SettingsKeys.FUN_MODE_INTERVAL_ROUNDS, v.coerceIn(1, 9).toString())

    suspend fun getFunModeDisabledRules(): List<String> {
        val raw = get(SettingsKeys.FUN_MODE_DISABLED_RULES, SettingsDefaults.FUN_MODE_DISABLED_RULES)
        return if (raw.isBlank()) emptyList()
        else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    suspend fun setFunModeDisabledRules(ids: List<String>) =
        set(SettingsKeys.FUN_MODE_DISABLED_RULES, ids.joinToString(","))
}
