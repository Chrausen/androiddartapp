package com.clubdarts.util

import com.clubdarts.data.model.CheckoutRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {

    // -------------------------------------------------------------------------
    // resolveVisit()
    // -------------------------------------------------------------------------

    @Test
    fun `normal score is deducted and not a bust`() {
        val result = ScoringEngine.resolveVisit(
            remaining = 301, visitTotal = 60,
            lastDartScore = 20, lastDartMult = 1,
            rule = CheckoutRule.STRAIGHT
        )
        assertFalse(result.isBust)
        assertEquals(60, result.effectiveScore)
    }

    @Test
    fun `over-score is a bust`() {
        // remaining=40, visitTotal=60 → afterVisit=-20
        val result = ScoringEngine.resolveVisit(
            remaining = 40, visitTotal = 60,
            lastDartScore = 20, lastDartMult = 2,
            rule = CheckoutRule.DOUBLE
        )
        assertTrue(result.isBust)
        assertEquals(0, result.effectiveScore)
        assertEquals(ScoringEngine.BustReason.OVER_SCORE, result.bustReason)
    }

    @Test
    fun `landing on 1 with DOUBLE rule is a bust`() {
        // remaining=41, visitTotal=40 → afterVisit=1
        val result = ScoringEngine.resolveVisit(
            remaining = 41, visitTotal = 40,
            lastDartScore = 20, lastDartMult = 1,
            rule = CheckoutRule.DOUBLE
        )
        assertTrue(result.isBust)
        assertEquals(0, result.effectiveScore)
        assertEquals(ScoringEngine.BustReason.LANDS_ON_ONE, result.bustReason)
    }

    @Test
    fun `landing on 1 with TRIPLE rule is a bust`() {
        // remaining=180, visitTotal=179 (e.g. T20+T20+T19) → afterVisit=1
        val result = ScoringEngine.resolveVisit(
            remaining = 180, visitTotal = 179,
            lastDartScore = 19, lastDartMult = 3,
            rule = CheckoutRule.TRIPLE
        )
        assertTrue(result.isBust)
        assertEquals(0, result.effectiveScore)
        assertEquals(ScoringEngine.BustReason.LANDS_ON_ONE, result.bustReason)
    }

    @Test
    fun `landing on 1 with STRAIGHT rule is NOT a bust`() {
        // remaining=2, visitTotal=1 → afterVisit=1; STRAIGHT allows finishing on 1
        val result = ScoringEngine.resolveVisit(
            remaining = 2, visitTotal = 1,
            lastDartScore = 1, lastDartMult = 1,
            rule = CheckoutRule.STRAIGHT
        )
        assertFalse(result.isBust)
        assertEquals(1, result.effectiveScore)
    }

    @Test
    fun `valid DOUBLE checkout is accepted`() {
        // D20 = 40, remaining=40 → checkout
        val result = ScoringEngine.resolveVisit(
            remaining = 40, visitTotal = 40,
            lastDartScore = 20, lastDartMult = 2,
            rule = CheckoutRule.DOUBLE
        )
        assertFalse(result.isBust)
        assertEquals(40, result.effectiveScore)
    }

    @Test
    fun `DOUBLE rule checkout with single dart is a bust`() {
        // remaining=40, visitTotal=40 but last dart is a single → invalid checkout
        val result = ScoringEngine.resolveVisit(
            remaining = 40, visitTotal = 40,
            lastDartScore = 40, lastDartMult = 1,
            rule = CheckoutRule.DOUBLE
        )
        assertTrue(result.isBust)
        assertEquals(0, result.effectiveScore)
        assertEquals(ScoringEngine.BustReason.INVALID_CHECKOUT_MULTIPLIER, result.bustReason)
    }

    @Test
    fun `valid STRAIGHT checkout is accepted with single dart`() {
        val result = ScoringEngine.resolveVisit(
            remaining = 20, visitTotal = 20,
            lastDartScore = 20, lastDartMult = 1,
            rule = CheckoutRule.STRAIGHT
        )
        assertFalse(result.isBust)
        assertEquals(20, result.effectiveScore)
    }

    @Test
    fun `valid TRIPLE checkout is accepted`() {
        // T20 = 60, remaining=60 → checkout
        val result = ScoringEngine.resolveVisit(
            remaining = 60, visitTotal = 60,
            lastDartScore = 20, lastDartMult = 3,
            rule = CheckoutRule.TRIPLE
        )
        assertFalse(result.isBust)
        assertEquals(60, result.effectiveScore)
    }

    @Test
    fun `TRIPLE rule checkout with double dart is a bust`() {
        val result = ScoringEngine.resolveVisit(
            remaining = 40, visitTotal = 40,
            lastDartScore = 20, lastDartMult = 2,
            rule = CheckoutRule.TRIPLE
        )
        assertTrue(result.isBust)
        assertEquals(ScoringEngine.BustReason.INVALID_CHECKOUT_MULTIPLIER, result.bustReason)
    }

    @Test
    fun `perfect 180 (T20+T20+T20) is not a bust when not a checkout`() {
        val result = ScoringEngine.resolveVisit(
            remaining = 301, visitTotal = 180,
            lastDartScore = 20, lastDartMult = 3,
            rule = CheckoutRule.STRAIGHT
        )
        assertFalse(result.isBust)
        assertEquals(180, result.effectiveScore)
    }

    // -------------------------------------------------------------------------
    // isImmediateBust()
    // -------------------------------------------------------------------------

    @Test
    fun `isImmediateBust is true when soFar exceeds remaining`() {
        assertTrue(ScoringEngine.isImmediateBust(remaining = 30, soFar = 40, rule = CheckoutRule.DOUBLE))
    }

    @Test
    fun `isImmediateBust is true when gap is 1 with DOUBLE rule`() {
        // remaining=41, soFar=40 → gap=1
        assertTrue(ScoringEngine.isImmediateBust(remaining = 41, soFar = 40, rule = CheckoutRule.DOUBLE))
    }

    @Test
    fun `isImmediateBust is true when gap is 1 with TRIPLE rule`() {
        assertTrue(ScoringEngine.isImmediateBust(remaining = 61, soFar = 60, rule = CheckoutRule.TRIPLE))
    }

    @Test
    fun `isImmediateBust is false when gap is 1 with STRAIGHT rule`() {
        assertFalse(ScoringEngine.isImmediateBust(remaining = 41, soFar = 40, rule = CheckoutRule.STRAIGHT))
    }

    @Test
    fun `isImmediateBust is false when gap is zero (checkout possible)`() {
        assertFalse(ScoringEngine.isImmediateBust(remaining = 40, soFar = 40, rule = CheckoutRule.DOUBLE))
    }

    @Test
    fun `isImmediateBust is false when score is within normal range`() {
        assertFalse(ScoringEngine.isImmediateBust(remaining = 301, soFar = 60, rule = CheckoutRule.DOUBLE))
    }
}
