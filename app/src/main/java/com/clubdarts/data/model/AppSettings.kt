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

    // Sound effects
    const val SOUND_EFFECTS_MUTED  = "sound_effects_muted"
    const val SOUND_EFFECTS_VOLUME = "sound_effects_volume"

    // Ranking system
    const val RANKING_ENABLED       = "ranking_enabled"
    const val RANKING_K_FACTOR      = "ranking_k_factor"       // "32" | "64"
    const val RANKING_START_SCORE   = "ranking_start_score"
    const val RANKING_CHECKOUT_RULE = "ranking_checkout_rule"
    const val RANKING_LEGS_TO_WIN   = "ranking_legs_to_win"

    // Fun mode
    const val FUN_MODE_ENABLED         = "fun_mode_enabled"
    const val FUN_MODE_INTERVAL_ROUNDS = "fun_mode_interval_rounds"
    const val FUN_MODE_DISABLED_RULES  = "fun_mode_disabled_rules"  // comma-separated rule IDs

    // Random commentary
    const val RANDOM_COMMENTARY_ENABLED = "random_commentary_enabled"
    const val COMMENTARY_PHRASES        = "commentary_phrases"       // JSON object
}

object SettingsDefaults {
    const val START_SCORE   = "301"
    const val CHECKOUT_RULE = "DOUBLE"
    const val LEGS_TO_WIN   = "1"
    const val RANDOM_ORDER  = "false"
    const val RECENT_IDS    = ""
    const val SHOW_HISTORY  = "false"
    const val GAME_MODE     = "SINGLE"

    // Sound effects
    const val SOUND_EFFECTS_MUTED  = "false"
    const val SOUND_EFFECTS_VOLUME = "1.0"

    // Ranking system
    const val RANKING_ENABLED       = "true"
    const val RANKING_K_FACTOR      = "32"
    const val RANKING_START_SCORE   = "501"
    const val RANKING_CHECKOUT_RULE = "DOUBLE"
    const val RANKING_LEGS_TO_WIN   = "1"

    // Fun mode
    const val FUN_MODE_ENABLED         = "false"
    const val FUN_MODE_INTERVAL_ROUNDS = "1"
    const val FUN_MODE_DISABLED_RULES  = ""

    // Random commentary
    const val RANDOM_COMMENTARY_ENABLED = "false"
}

/** Words spoken before/after the score number for a custom TTS phrase. */
data class TtsPhrase(val before: String, val after: String)

/** All custom phrases configured for a given score value. */
data class TtsScoreSetting(val score: Int, val phrases: List<TtsPhrase>)

/** Funny commentary phrases split by score tier. */
data class CommentaryPhrases(
    val bad: List<String>,
    val normal: List<String>,
    val good: List<String>
) {
    companion object {
        val DEFAULT = CommentaryPhrases(
            bad = listOf(
                "Ok, at least something.",
                "Badum tsss.",
                "My grandma throws better.",
                "Were you aiming for the wall?",
                "Bold strategy.",
                "That happened.",
                "Not your finest moment.",
                "At least you hit the board."
            ),
            normal = listOf(
                "Not bad, not great.",
                "I've seen worse.",
                "Respectable... barely.",
                "Solid mediocrity.",
                "Right in the average zone.",
                "The darts gods are neutral.",
                "Could be worse, could be better."
            ),
            good = listOf(
                "WHAT WAS THAT?!",
                "Absolutely unbelievable!",
                "Is this legal?",
                "Call the darts police!",
                "Someone stop this player!",
                "That's just showing off.",
                "Pure perfection!",
                "Legendary!"
            )
        )
    }
}
