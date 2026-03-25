package com.clubdarts.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.clubdarts.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for the "Undo last throw" feature on the game result screen.
 *
 * Test scenarios:
 *  1. Undo returns the player to the live game screen with the score restored.
 *  2. Cancelling the undo dialog keeps the result screen intact.
 *  3. After undo, the game no longer appears in history (not saved).
 *
 * Uses the same 301 Straight throw sequence as [StandardWorkflowTest]:
 *   Visit 1: T20 + T20 + T20 = 180  →  remaining: 121
 *   Visit 2: T20 + T20 + 1   = 121  →  remaining: 0  (Straight checkout)
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UndoLastThrowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun undoLastThrow_afterCheckout_returnsToLiveGameScreen() {
        playGameToResultScreen()

        // Result screen is showing
        composeTestRule.onNodeWithText("Winner!").assertIsDisplayed()

        // Open the undo dialog
        composeTestRule.onNodeWithText("Undo last throw").performClick()

        // Dialog title is visible
        composeTestRule.onNodeWithText("Undo last throw?").assertIsDisplayed()

        // Confirm undo — the dialog confirm button also says "Undo last throw"
        // Use hasClickAction() to distinguish it from the title
        composeTestRule.onNode(hasText("Undo last throw") and hasClickAction()).performClick()

        // Should navigate back to the live game screen — "Winner!" should be gone
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Winner!").fetchSemanticsNodes().isEmpty()
        }

        // The live game screen elements should be present again
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun undoLastThrow_cancelDialog_keepsResultScreen() {
        playGameToResultScreen()

        composeTestRule.onNodeWithText("Winner!").assertIsDisplayed()

        // Open the undo dialog
        composeTestRule.onNodeWithText("Undo last throw").performClick()
        composeTestRule.onNodeWithText("Undo last throw?").assertIsDisplayed()

        // Cancel — the dialog should close and the result screen remains
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Dialog should be dismissed
        composeTestRule.onNodeWithText("Undo last throw?").assertDoesNotExist()

        // Result screen is still showing
        composeTestRule.onNodeWithText("Winner!").assertIsDisplayed()
    }

    @Test
    fun undoLastThrow_gameNotSavedBeforeUndo_gameRemovedFromHistory() {
        playGameToResultScreen()

        // Do NOT save the game — undo immediately
        composeTestRule.onNodeWithText("Undo last throw").performClick()
        composeTestRule.onNodeWithText("Undo last throw?").assertIsDisplayed()
        composeTestRule.onNode(hasText("Undo last throw") and hasClickAction()).performClick()

        // Wait for navigation back to live screen
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Winner!").fetchSemanticsNodes().isEmpty()
        }

        // Abort the game so we can check history
        composeTestRule.onNodeWithContentDescription("Abort game").performClick()
        composeTestRule.onNodeWithText("Abort").performClick()

        // Navigate to history — no completed game should be shown
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Match history").assertIsDisplayed()
        composeTestRule.onNodeWithText("No matches yet").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Helper: play a 301 Straight 1-leg game to the result screen
    // -------------------------------------------------------------------------

    private fun playGameToResultScreen() {
        // Add Alice
        composeTestRule.onNodeWithText("Players").performClick()
        composeTestRule.onNodeWithContentDescription("Add player").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Alice")
        composeTestRule.onNodeWithText("Save").performClick()

        // Add Bob
        composeTestRule.onNodeWithContentDescription("Add player").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Bob")
        composeTestRule.onNodeWithText("Save").performClick()

        // Navigate to Game tab
        composeTestRule.onNodeWithText("Game").performClick()

        // Expand settings and select Straight checkout
        composeTestRule.onNodeWithContentDescription("Expand settings").performClick()
        composeTestRule.onNodeWithText("Straight").performClick()

        // Add both players
        composeTestRule.onNodeWithText("Add player").performClick()
        composeTestRule.onAllNodesWithContentDescription("Add")[0].performClick()
        composeTestRule.onAllNodesWithContentDescription("Add")[0].performClick()
        Espresso.pressBack()

        // Start the game
        composeTestRule.onNodeWithText("Start game").performClick()

        // Visit 1: T20 + T20 + T20 = 180  (Alice, remaining: 121)
        enterTriple20(); enterTriple20(); enterTriple20()

        // Visit 2: T20 + T20 + 1 = 121  (Bob's turn skipped — only 2 players)
        // Actually Bob throws Visit 1 after Alice, so:
        // Alice: Visit 1 → 121 remaining
        // Bob: Visit 1 → then Alice: Visit 2 → checkout
        // Wait — we need Alice to reach 0.  Let me re-check the sequence.
        //
        // With 301 and Alice going first:
        //   Alice Visit 1: T20+T20+T20 = 180 → 121 left
        //   Bob   Visit 1: T20+T20+T20 = 180 → 121 left
        //   Alice Visit 2: T20+T20+1   = 121 → 0   (checkout!)
        //
        // But for simplicity in the test we just play until checkout regardless of turns.
        // The StandardWorkflowTest follows the same sequence.
        enterTriple20(); enterTriple20(); enterTriple20()  // Bob Visit 1
        enterTriple20(); enterTriple20()                   // Alice Visit 2 darts 1+2
        composeTestRule.onNode(hasText("1") and hasClickAction()).performClick()  // Alice dart 3 = checkout
    }

    /** Taps Triple multiplier then the 20 button. */
    private fun enterTriple20() {
        composeTestRule.onNodeWithText("Triple").performClick()
        composeTestRule.onNode(hasText("20") and hasClickAction()).performClick()
    }
}
