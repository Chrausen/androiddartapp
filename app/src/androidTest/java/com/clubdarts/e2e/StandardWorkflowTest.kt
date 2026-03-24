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
 * End-to-end test for the standard x01 workflow:
 *   add 2 players → set up a 301 Straight game → play to finish → save → verify in history.
 *
 * The test database module (TestDatabaseModule) replaces the production Room database with an
 * in-memory instance so every run starts with clean state.
 *
 * Throw sequence (301, Straight checkout):
 *   Visit 1: T20 + T20 + T20 = 180  →  remaining: 121
 *   Visit 2: T20 + T20 + 1   = 121  →  remaining: 0   (last dart = single 1, valid Straight finish)
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StandardWorkflowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    // -------------------------------------------------------------------------
    // Main test
    // -------------------------------------------------------------------------

    @Test
    fun standardWorkflow_x01_twoPlayers_gameAppearsInHistory() {

        // ── Step 1: Add "Alice" via the Players screen ────────────────────────
        composeTestRule.onNodeWithText("Players").performClick()
        composeTestRule.onNodeWithContentDescription("Add player").performClick()
        // The dialog contains a single editable text field (the player name input)
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Alice")
        composeTestRule.onNodeWithText("Save").performClick()

        // ── Step 2: Add "Bob" ─────────────────────────────────────────────────
        composeTestRule.onNodeWithContentDescription("Add player").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Bob")
        composeTestRule.onNodeWithText("Save").performClick()

        // Verify both players are shown in the list
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()

        // ── Step 3: Navigate to Game tab ──────────────────────────────────────
        composeTestRule.onNodeWithText("Game").performClick()
        composeTestRule.onNodeWithText("New game").assertIsDisplayed()

        // ── Step 4: Expand game settings, then pick "Straight" checkout ───────
        // The settings header row has a collapsible chevron; clicking it (or the row) expands.
        composeTestRule.onNodeWithContentDescription("Expand settings").performClick()
        composeTestRule.onNodeWithText("Straight").performClick()
        // Leave starting score at 301 (default) and legs at 1

        // ── Step 5: Add Alice and Bob through the player picker ───────────────
        composeTestRule.onNodeWithText("Add player").performClick()

        // The picker bottom sheet is now visible.
        // "Add" is the content description of each player's add-icon button.
        // Clicking the first available "Add" button selects Alice (she appears first).
        composeTestRule.onAllNodesWithContentDescription("Add")[0].performClick()
        // After Alice is selected her "Add" button disappears; Bob's is now [0].
        composeTestRule.onAllNodesWithContentDescription("Add")[0].performClick()

        // Dismiss the picker (back press closes the ModalBottomSheet)
        Espresso.pressBack()

        // ── Step 6: Start the game ────────────────────────────────────────────
        composeTestRule.onNodeWithText("Start game").performClick()

        // ── Step 7: Visit 1 — T20 + T20 + T20 = 180 (remaining: 121) ─────────
        // After the 3rd dart the visit is auto-resolved by the ViewModel.
        enterTriple20()  // dart 1
        enterTriple20()  // dart 2
        enterTriple20()  // dart 3 → visit resolved

        // ── Step 8: Visit 2 — T20 + T20 + 1 = 121 (checkout, remaining: 0) ───
        enterTriple20()  // dart 1
        enterTriple20()  // dart 2
        // Click the "1" numpad button. Filter by hasClickAction() to distinguish
        // the button from any "1" that might appear in the score display.
        composeTestRule.onNode(hasText("1") and hasClickAction()).performClick()

        // ── Step 9: Result screen ─────────────────────────────────────────────
        // The game navigates to the result screen automatically on checkout.
        composeTestRule.onNodeWithText("Winner!").assertIsDisplayed()
        // Save the game so it appears in history
        composeTestRule.onNodeWithText("Save to history").performClick()
        composeTestRule.onNodeWithText("Saved to history").assertIsDisplayed()

        // ── Step 10: Verify in History ────────────────────────────────────────
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("Match history").assertIsDisplayed()
        // Both player names should be visible in the game card
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Taps the "Triple" multiplier button then the "20" number button. */
    private fun enterTriple20() {
        composeTestRule.onNodeWithText("Triple").performClick()
        // Use hasClickAction() to target the numpad "20" button rather than any
        // score display that might incidentally show the number 20.
        composeTestRule.onNode(hasText("20") and hasClickAction()).performClick()
    }
}
