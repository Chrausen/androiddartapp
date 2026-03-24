package com.clubdarts.util

import com.clubdarts.data.model.CheckoutRule

/**
 * Pure scoring logic extracted from GameViewModel so it can be unit-tested
 * without any Android or Hilt dependencies.
 */
object ScoringEngine {

    enum class BustReason { OVER_SCORE, LANDS_ON_ONE, INVALID_CHECKOUT_MULTIPLIER }

    data class VisitResult(
        val effectiveScore: Int,
        val isBust: Boolean,
        val bustReason: BustReason? = null
    )

    /**
     * Resolves a completed visit and returns how many points are deducted (0 if bust).
     *
     * @param remaining  Score before this visit.
     * @param visitTotal Sum of all dart values in this visit.
     * @param lastDartScore  Face value of the last dart thrown (0 for a miss).
     * @param lastDartMult   Multiplier of the last dart thrown (1 = single, 2 = double, 3 = triple).
     * @param rule       Checkout rule in effect.
     */
    fun resolveVisit(
        remaining: Int,
        visitTotal: Int,
        lastDartScore: Int,
        lastDartMult: Int,
        rule: CheckoutRule
    ): VisitResult {
        val afterVisit = remaining - visitTotal

        return when {
            afterVisit < 0 ->
                VisitResult(effectiveScore = 0, isBust = true, bustReason = BustReason.OVER_SCORE)

            afterVisit == 1 && (rule == CheckoutRule.DOUBLE || rule == CheckoutRule.TRIPLE) ->
                VisitResult(effectiveScore = 0, isBust = true, bustReason = BustReason.LANDS_ON_ONE)

            afterVisit == 0 -> {
                val validCheckout = CheckoutCalculator.isValidCheckout(
                    lastDartScore = lastDartScore,
                    lastDartMult = lastDartMult,
                    remainingAfter = 0,
                    rule = rule
                )
                if (validCheckout) {
                    VisitResult(effectiveScore = visitTotal, isBust = false)
                } else {
                    VisitResult(effectiveScore = 0, isBust = true, bustReason = BustReason.INVALID_CHECKOUT_MULTIPLIER)
                }
            }

            else ->
                VisitResult(effectiveScore = visitTotal, isBust = false)
        }
    }

    /**
     * Checks for an immediate (mid-visit) bust after each dart, before all 3 darts
     * have been thrown.  Returns true if no legal finish is possible with the score
     * already accumulated.
     *
     * @param remaining Score before this visit.
     * @param soFar     Points accumulated by darts thrown so far this visit.
     * @param rule      Checkout rule in effect.
     */
    fun isImmediateBust(remaining: Int, soFar: Int, rule: CheckoutRule): Boolean {
        val gap = remaining - soFar
        return when {
            gap < 0 -> true
            gap == 1 && (rule == CheckoutRule.DOUBLE || rule == CheckoutRule.TRIPLE) -> true
            else -> false
        }
    }
}
