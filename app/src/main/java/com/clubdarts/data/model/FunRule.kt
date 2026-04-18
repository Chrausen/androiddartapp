package com.clubdarts.data.model

import androidx.annotation.StringRes

enum class FunRuleCategory { PHYSICAL, SCORING }

private val PRIME_SEGMENTS = setOf(2, 3, 5, 7, 11, 13, 17, 19)

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
    MIRROR_THROW;

    fun apply(score: Int, multiplier: Int): Int {
        if (score == 0) return 0
        val faceValue = score * multiplier
        return when (this) {
            NONE             -> faceValue
            SWAP_MULTIPLIERS -> when (multiplier) { 2 -> score * 3; 3 -> score * 2; else -> faceValue }
            DOUBLE_SCORE     -> faceValue * 2
            EVEN_HALVED      -> if (score % 2 == 0) faceValue / 2 else faceValue
            ODD_DOUBLED      -> if (score % 2 != 0) faceValue * 2 else faceValue
            PRIME_BOOST      -> if (score in PRIME_SEGMENTS) faceValue * 3 else faceValue
            FIVES_MAGIC      -> if (score % 5 == 0) faceValue * 2 else faceValue
            TWENTY_TAX       -> if (score == 20) faceValue / 2 else faceValue
            EVEN_STOLEN      -> if (score % 2 == 0) faceValue / 2 else faceValue
            MIRROR_THROW     -> faceValue
        }
    }

    fun applyMultiplier(multiplier: Int): Int = when (this) {
        SWAP_MULTIPLIERS -> when (multiplier) { 2 -> 3; 3 -> 2; else -> multiplier }
        else -> multiplier
    }
}

data class FunRule(
    val id: String,
    val category: FunRuleCategory,
    val scoreModifier: ScoreModifier = ScoreModifier.NONE,
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val teamsOnly: Boolean = false,
)
