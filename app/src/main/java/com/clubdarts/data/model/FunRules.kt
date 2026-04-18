package com.clubdarts.data.model

import com.clubdarts.R

object FunRules {
    val all: List<FunRule> = listOf(
        // ── Physical challenges ──────────────────────────────────────────────
        FunRule(
            id = "weak_hand",
            category = FunRuleCategory.PHYSICAL,
            emoji = "🤚",
            titleRes = R.string.fun_rule_weak_hand_title,
            descRes = R.string.fun_rule_weak_hand_desc,
        ),
        FunRule(
            id = "step_back",
            category = FunRuleCategory.PHYSICAL,
            emoji = "👣",
            titleRes = R.string.fun_rule_step_back_title,
            descRes = R.string.fun_rule_step_back_desc,
        ),
        FunRule(
            id = "one_leg",
            category = FunRuleCategory.PHYSICAL,
            emoji = "🦵",
            titleRes = R.string.fun_rule_one_leg_title,
            descRes = R.string.fun_rule_one_leg_desc,
        ),
        FunRule(
            id = "eyes_closed",
            category = FunRuleCategory.PHYSICAL,
            emoji = "🙈",
            titleRes = R.string.fun_rule_eyes_closed_title,
            descRes = R.string.fun_rule_eyes_closed_desc,
        ),
        FunRule(
            id = "jump_first",
            category = FunRuleCategory.PHYSICAL,
            emoji = "🤸",
            titleRes = R.string.fun_rule_jump_first_title,
            descRes = R.string.fun_rule_jump_first_desc,
        ),
        FunRule(
            id = "kneeling",
            category = FunRuleCategory.PHYSICAL,
            emoji = "🧎",
            titleRes = R.string.fun_rule_kneeling_title,
            descRes = R.string.fun_rule_kneeling_desc,
        ),
        FunRule(
            id = "two_fingers",
            category = FunRuleCategory.PHYSICAL,
            emoji = "🤏",
            titleRes = R.string.fun_rule_two_fingers_title,
            descRes = R.string.fun_rule_two_fingers_desc,
        ),

        // ── Scoring modifiers ────────────────────────────────────────────────
        FunRule(
            id = "swap_multipliers",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.SWAP_MULTIPLIERS,
            emoji = "🔀",
            titleRes = R.string.fun_rule_swap_multipliers_title,
            descRes = R.string.fun_rule_swap_multipliers_desc,
        ),
        FunRule(
            id = "double_score",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.DOUBLE_SCORE,
            emoji = "✖️",
            titleRes = R.string.fun_rule_double_score_title,
            descRes = R.string.fun_rule_double_score_desc,
        ),
        FunRule(
            id = "even_halved",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.EVEN_HALVED,
            emoji = "➗",
            titleRes = R.string.fun_rule_even_halved_title,
            descRes = R.string.fun_rule_even_halved_desc,
        ),
        FunRule(
            id = "odd_doubled",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.ODD_DOUBLED,
            emoji = "✌️",
            titleRes = R.string.fun_rule_odd_doubled_title,
            descRes = R.string.fun_rule_odd_doubled_desc,
        ),
        FunRule(
            id = "prime_boost",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.PRIME_BOOST,
            emoji = "🔢",
            titleRes = R.string.fun_rule_prime_boost_title,
            descRes = R.string.fun_rule_prime_boost_desc,
        ),
        FunRule(
            id = "fives_magic",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.FIVES_MAGIC,
            emoji = "⭐",
            titleRes = R.string.fun_rule_fives_magic_title,
            descRes = R.string.fun_rule_fives_magic_desc,
        ),
        FunRule(
            id = "twenty_tax",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.TWENTY_TAX,
            emoji = "💸",
            titleRes = R.string.fun_rule_twenty_tax_title,
            descRes = R.string.fun_rule_twenty_tax_desc,
        ),
        FunRule(
            id = "even_stolen",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.EVEN_STOLEN,
            emoji = "🎁",
            titleRes = R.string.fun_rule_even_stolen_title,
            descRes = R.string.fun_rule_even_stolen_desc,
        ),
        FunRule(
            id = "mirror_throw",
            category = FunRuleCategory.SCORING,
            scoreModifier = ScoreModifier.MIRROR_THROW,
            emoji = "🪞",
            titleRes = R.string.fun_rule_mirror_throw_title,
            descRes = R.string.fun_rule_mirror_throw_desc,
        ),
    )
}
