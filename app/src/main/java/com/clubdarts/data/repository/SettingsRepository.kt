package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.AppSettingsDao
import com.clubdarts.data.model.AppSettings
import com.clubdarts.data.model.CheckoutRule
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
}
