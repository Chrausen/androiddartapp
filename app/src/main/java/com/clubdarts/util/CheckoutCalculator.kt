package com.clubdarts.util

import com.clubdarts.data.model.CheckoutRule

/**
 * Checkout hints and checkout-validity checks for x01 darts.
 *
 * Checkout paths are computed algorithmically at startup rather than maintained as a static
 * lookup table. All valid finishes reachable within 3 darts are precomputed and cached.
 *
 * ## Dart notation used in suggestion strings
 * - `D14`           = double 14 (28 pts)
 * - `T20`           = triple 20 (60 pts)
 * - `Bull`          = bullseye  (50 pts, i.e. double 25)
 * - `Bull (single)` = single bull (25 pts)
 * - `·`             = separator between darts in a single visit (e.g. `"T20 · D20"` = 2-dart finish)
 */
object CheckoutCalculator {

    private data class Dart(val score: Int, val notation: String)

    /**
     * All valid dart scores ordered by preference for use as intermediate (non-finishing) darts.
     * Higher-value darts come first so that computed paths prefer T20-heavy routes.
     */
    private val intermediateDarts: List<Dart> = buildList {
        for (i in 20 downTo 1) add(Dart(i * 3, "T$i"))   // T20(60) … T1(3)
        add(Dart(50, "Bull"))                               // Bullseye = 50
        for (i in 20 downTo 1) add(Dart(i, "$i"))          // 20 … 1
        for (i in 20 downTo 1) add(Dart(i * 2, "D$i"))    // D20(40) … D1(2)
        add(Dart(25, "Bull (single)"))                      // Single bull = 25
    }

    /** Valid finishing darts for Double Out: must end on a double. */
    private val finishingDoubles: List<Dart> = buildList {
        for (i in 20 downTo 1) add(Dart(i * 2, "D$i"))    // D20(40) … D1(2)
        add(Dart(50, "Bull"))                               // Bull counts as D25
    }

    /** Valid finishing darts for Triple Out: must end on a triple. */
    private val finishingTriples: List<Dart> =
        (20 downTo 1).map { i -> Dart(i * 3, "T$i") }

    /** Valid finishing darts for Straight Out: any scoring dart. */
    private val finishingStraight: List<Dart> = buildList {
        for (i in 20 downTo 1) add(Dart(i * 3, "T$i"))    // T20(60) … T1(3)
        add(Dart(50, "Bull"))                               // Bull = 50
        for (i in 20 downTo 1) add(Dart(i * 2, "D$i"))    // D20(40) … D1(2)
        for (i in 20 downTo 1) add(Dart(i, "$i"))          // 20 … 1
        add(Dart(25, "Bull (single)"))                      // Single bull = 25
    }

    private val doubleOutPaths: Map<Int, String> = computePaths(finishingDoubles, maxTotal = 170)
    private val tripleOutPaths: Map<Int, String> = computePaths(finishingTriples, maxTotal = 180)
    private val straightOutPaths: Map<Int, String> = computePaths(finishingStraight, maxTotal = 180)

    /**
     * Precomputes all achievable checkout paths up to [maxTotal] points using at most 3 darts,
     * ending with a dart from [finishers]. The first (highest-preference) path found for each
     * total wins, so ordering of [intermediateDarts] and [finishers] determines suggestions.
     */
    private fun computePaths(finishers: List<Dart>, maxTotal: Int): Map<Int, String> {
        val paths = mutableMapOf<Int, String>()

        // 1-dart finishes
        for (fin in finishers) {
            if (fin.score !in paths) paths[fin.score] = fin.notation
        }

        // 2-dart finishes
        for (first in intermediateDarts) {
            for (fin in finishers) {
                val total = first.score + fin.score
                if (total in 2..maxTotal && total !in paths) {
                    paths[total] = "${first.notation} · ${fin.notation}"
                }
            }
        }

        // 3-dart finishes
        for (first in intermediateDarts) {
            for (second in intermediateDarts) {
                for (fin in finishers) {
                    val total = first.score + second.score + fin.score
                    if (total in 2..maxTotal && total !in paths) {
                        paths[total] = "${first.notation} · ${second.notation} · ${fin.notation}"
                    }
                }
            }
        }

        return paths
    }

    /**
     * Returns a checkout suggestion for [score] under [rule], or null if none exists.
     * [maxDarts] limits the suggestion to paths that fit within the remaining darts
     * of the current visit (1–3). Suggestions with more steps than [maxDarts] are
     * suppressed so the hint always reflects what is still achievable this turn.
     */
    fun suggest(score: Int, rule: CheckoutRule, maxDarts: Int = 3): String? {
        if (score < 1 || score > 170) return null
        val raw = when (rule) {
            CheckoutRule.DOUBLE -> doubleOutPaths[score]
            CheckoutRule.STRAIGHT -> straightOutPaths[score]
            CheckoutRule.TRIPLE -> tripleOutPaths[score]
        }
        if (raw != null && raw.split(" · ").size > maxDarts) return null
        return raw
    }

    /**
     * Returns true when [score] can still be finished under [rule] within a single visit (3 darts).
     */
    fun isCheckoutPossible(score: Int, rule: CheckoutRule): Boolean {
        if (score < 1) return false
        return when (rule) {
            CheckoutRule.DOUBLE -> doubleOutPaths.containsKey(score)
            CheckoutRule.STRAIGHT -> straightOutPaths.containsKey(score)
            CheckoutRule.TRIPLE -> tripleOutPaths.containsKey(score)
        }
    }

    fun isValidCheckout(
        lastDartScore: Int,
        lastDartMult: Int,
        remainingAfter: Int,
        rule: CheckoutRule
    ): Boolean {
        if (remainingAfter != 0) return false
        return when (rule) {
            CheckoutRule.STRAIGHT -> lastDartScore > 0
            CheckoutRule.DOUBLE -> lastDartMult == 2 && lastDartScore > 0
            CheckoutRule.TRIPLE -> lastDartMult == 3 && lastDartScore > 0
        }
    }
}
