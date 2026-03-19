package com.clubdarts.data.repository

import com.clubdarts.data.db.dao.AppSettingsDao
import com.clubdarts.data.model.AppSettings
import com.clubdarts.data.model.CheckoutRule
import com.clubdarts.data.model.SettingsDefaults
import com.clubdarts.data.model.SettingsKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        get(SettingsKeys.LAST_START_SCORE, SettingsDefaults.START_SCORE).toIntOrNull() ?: 501

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

    suspend fun addRecentPlayer(playerId: Long) {
        val current = getRecentPlayerIds().toMutableList()
        current.remove(playerId)
        current.add(0, playerId)
        val trimmed = current.take(5)
        set(SettingsKeys.RECENT_PLAYER_IDS, trimmed.joinToString(","))
    }
}
