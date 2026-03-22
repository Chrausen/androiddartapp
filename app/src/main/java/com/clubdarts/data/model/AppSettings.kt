package com.clubdarts.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(@PrimaryKey val key: String, val value: String)

object SettingsKeys {
    const val LAST_START_SCORE   = "last_start_score"
    const val LAST_CHECKOUT_RULE = "last_checkout_rule"
    const val LAST_LEGS_TO_WIN   = "last_legs_to_win"
    const val LAST_RANDOM_ORDER  = "last_random_order"
    const val RECENT_PLAYER_IDS  = "recent_player_ids"  // comma-separated, max 5
    const val TTS_SCORE_PHRASES  = "tts_score_phrases"  // JSON array
    const val SHOW_HISTORY       = "show_history"
    const val LAST_GAME_MODE     = "last_game_mode"   // "SINGLE" | "TEAMS"
}

object SettingsDefaults {
    const val START_SCORE   = "301"
    const val CHECKOUT_RULE = "DOUBLE"
    const val LEGS_TO_WIN   = "1"
    const val RANDOM_ORDER  = "false"
    const val RECENT_IDS    = ""
    const val SHOW_HISTORY  = "false"
    const val GAME_MODE     = "SINGLE"
}

/** Words spoken before/after the score number for a custom TTS phrase. */
data class TtsPhrase(val before: String, val after: String)

/** All custom phrases configured for a given score value. */
data class TtsScoreSetting(val score: Int, val phrases: List<TtsPhrase>)
