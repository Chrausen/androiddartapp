package com.clubdarts.util

import com.clubdarts.data.model.CheckoutRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class CheckoutCalculatorTest {

    // -------------------------------------------------------------------------
    // suggest()
    // -------------------------------------------------------------------------

    @Test
    fun `suggest returns hint for maximum double-out score (170)`() {
        val hint = CheckoutCalculator.suggest(170, CheckoutRule.DOUBLE)
        assertEquals("T20 · T20 · Bull", hint)
    }

    @Test
    fun `suggest returns hint for classic double-out (40 = D20)`() {
        val hint = CheckoutCalculator.suggest(40, CheckoutRule.DOUBLE)
        assertEquals("D20", hint)
    }

    @Test
    fun `suggest returns null for score above 170 (double rule)`() {
        assertNull(CheckoutCalculator.suggest(171, CheckoutRule.DOUBLE))
    }

    @Test
    fun `suggest returns null for score 0`() {
        assertNull(CheckoutCalculator.suggest(0, CheckoutRule.DOUBLE))
    }

    @Test
    fun `suggest returns null for negative score`() {
        assertNull(CheckoutCalculator.suggest(-1, CheckoutRule.DOUBLE))
    }

    @Test
    fun `suggest suppresses 3-dart hint when only 2 darts remain`() {
        // 101 requires 3 darts (T17 · T10 · D20); with maxDarts=2 it should return null
        assertNull(CheckoutCalculator.suggest(101, CheckoutRule.DOUBLE, maxDarts = 2))
    }

    @Test
    fun `suggest returns 2-dart hint when 2 darts remain and score fits`() {
        // 100 = T20 · D20 — exactly 2 darts, should still be returned with maxDarts=2
        assertNotNull(CheckoutCalculator.suggest(100, CheckoutRule.DOUBLE, maxDarts = 2))
    }

    @Test
    fun `suggest for straight-out single digit returns just the number`() {
        val hint = CheckoutCalculator.suggest(18, CheckoutRule.STRAIGHT)
        assertEquals("18", hint)
    }

    @Test
    fun `suggest for straight-out single bull`() {
        val hint = CheckoutCalculator.suggest(25, CheckoutRule.STRAIGHT)
        assertEquals("Bull (single)", hint)
    }

    @Test
    fun `suggest for triple-out divisible by 3`() {
        val hint = CheckoutCalculator.suggest(60, CheckoutRule.TRIPLE)
        assertEquals("T20", hint)
    }

    // -------------------------------------------------------------------------
    // isCheckoutPossible()
    // -------------------------------------------------------------------------

    @Test
    fun `isCheckoutPossible DOUBLE is true for even score within 170`() {
        assertTrue(CheckoutCalculator.isCheckoutPossible(40, CheckoutRule.DOUBLE))
    }

    @Test
    fun `isCheckoutPossible DOUBLE is false for score 1 (cannot finish on double)`() {
        assertFalse(CheckoutCalculator.isCheckoutPossible(1, CheckoutRule.DOUBLE))
    }

    @Test
    fun `isCheckoutPossible DOUBLE is false for impossible score 169`() {
        assertFalse(CheckoutCalculator.isCheckoutPossible(169, CheckoutRule.DOUBLE))
    }

    @Test
    fun `isCheckoutPossible DOUBLE is false for score above 170`() {
        assertFalse(CheckoutCalculator.isCheckoutPossible(171, CheckoutRule.DOUBLE))
    }

    @Test
    fun `isCheckoutPossible STRAIGHT is true for score up to 60`() {
        assertTrue(CheckoutCalculator.isCheckoutPossible(60, CheckoutRule.STRAIGHT))
    }

    @Test
    fun `isCheckoutPossible STRAIGHT is true for score 61 reachable in 2 darts`() {
        // T20 (60) + single 1 = 61 — a valid 2-dart Straight Out finish
        assertTrue(CheckoutCalculator.isCheckoutPossible(61, CheckoutRule.STRAIGHT))
    }

    @Test
    fun `isCheckoutPossible STRAIGHT is false for score above 180`() {
        assertFalse(CheckoutCalculator.isCheckoutPossible(181, CheckoutRule.STRAIGHT))
    }

    @Test
    fun `isCheckoutPossible TRIPLE is true for score up to 180`() {
        assertTrue(CheckoutCalculator.isCheckoutPossible(180, CheckoutRule.TRIPLE))
    }

    @Test
    fun `isCheckoutPossible is false for score 0 or negative`() {
        assertFalse(CheckoutCalculator.isCheckoutPossible(0, CheckoutRule.DOUBLE))
        assertFalse(CheckoutCalculator.isCheckoutPossible(-1, CheckoutRule.STRAIGHT))
    }

    // -------------------------------------------------------------------------
    // isValidCheckout()
    // -------------------------------------------------------------------------

    @Test
    fun `isValidCheckout DOUBLE is true when last dart is a double`() {
        assertTrue(
            CheckoutCalculator.isValidCheckout(
                lastDartScore = 20,
                lastDartMult = 2,
                remainingAfter = 0,
                rule = CheckoutRule.DOUBLE
            )
        )
    }

    @Test
    fun `isValidCheckout DOUBLE is false when last dart is a single`() {
        assertFalse(
            CheckoutCalculator.isValidCheckout(
                lastDartScore = 20,
                lastDartMult = 1,
                remainingAfter = 0,
                rule = CheckoutRule.DOUBLE
            )
        )
    }

    @Test
    fun `isValidCheckout DOUBLE is false when last dart is a triple`() {
        assertFalse(
            CheckoutCalculator.isValidCheckout(
                lastDartScore = 20,
                lastDartMult = 3,
                remainingAfter = 0,
                rule = CheckoutRule.DOUBLE
            )
        )
    }

    @Test
    fun `isValidCheckout STRAIGHT is true for any non-zero single`() {
        assertTrue(
            CheckoutCalculator.isValidCheckout(
                lastDartScore = 20,
                lastDartMult = 1,
                remainingAfter = 0,
                rule = CheckoutRule.STRAIGHT
            )
        )
    }

    @Test
    fun `isValidCheckout TRIPLE is true when last dart is a triple`() {
        assertTrue(
            CheckoutCalculator.isValidCheckout(
                lastDartScore = 20,
                lastDartMult = 3,
                remainingAfter = 0,
                rule = CheckoutRule.TRIPLE
            )
        )
    }

    @Test
    fun `isValidCheckout is false when remainingAfter is not zero`() {
        assertFalse(
            CheckoutCalculator.isValidCheckout(
                lastDartScore = 20,
                lastDartMult = 2,
                remainingAfter = 1,
                rule = CheckoutRule.DOUBLE
            )
        )
    }
}
