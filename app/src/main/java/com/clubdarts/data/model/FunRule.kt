package com.clubdarts.data.model

enum class FunRuleCategory { PHYSICAL, SCORING }

enum class ScoreModifier {
    NONE,
    SWAP_MULTIPLIERS,
    DOUBLE_SCORE,
    EVEN_HALVED,
    ODD_DOUBLED,
    PRIME_BOOST,
    FIVES_MAGIC,
    TWENTY_TAX,
    EVEN_STOLEN,
    MIRROR_THROW,
}

data class FunRule(
    val id: String,
    val category: FunRuleCategory,
    val scoreModifier: ScoreModifier = ScoreModifier.NONE,
    val emoji: String,
    val titleDe: String,
    val descDe: String,
    val teamsOnly: Boolean = false,
)
