package com.suman.memoryarchitect.ui.screens.gameplay

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.suman.memoryarchitect.core.ads.RewardedAdUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Covers the one behavior a ViewModel-level test can't: which callback an actual tap on the
 * exhausted-free-budget [HintButton] routes to, per the priority order the production-readiness
 * fix requires - owned token first, rewarded ad only once no token is held. [GameplayViewModelTest]
 * separately covers that redeeming a token deducts inventory exactly once and can't double-fire;
 * this test covers that the button itself picks the right action to call in the first place.
 * Matches by [hasClickAction] rather than a test tag - [HintButton]'s only clickable element is the
 * card itself (a plain `Box` wraps it purely for the armed-glow layer, with no click semantics of
 * its own), so it's the sole match when this composable is rendered in isolation.
 *
 * Requires a connected device/emulator's Compose runtime, same as every other androidTest in this
 * module already does.
 */
@RunWith(AndroidJUnit4::class)
class HintButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exhaustedFreeBudget_withOwnedToken_tapRedeemsTokenNotAd() {
        var tokenTapped = false
        var adTapped = false
        composeRule.setContent {
            HintButton(
                remaining = 0,
                isArmed = false,
                onClick = {},
                adState = RewardedAdUiState.Idle,
                onWatchAd = { adTapped = true },
                hasInventoryToken = true,
                inventoryTokenCount = 3,
                onUseToken = { tokenTapped = true },
                maxRewardedHints = 3,
                rewardedRemaining = 2,
            )
        }

        composeRule.onNode(hasClickAction()).performClick()

        assert(tokenTapped) { "Expected the owned token to be redeemed first" }
        assert(!adTapped) { "Rewarded ad should not be offered while an owned token is available" }
    }

    @Test
    fun exhaustedFreeBudget_withNoToken_tapOffersRewardedAd() {
        var tokenTapped = false
        var adTapped = false
        composeRule.setContent {
            HintButton(
                remaining = 0,
                isArmed = false,
                onClick = {},
                adState = RewardedAdUiState.Idle,
                onWatchAd = { adTapped = true },
                hasInventoryToken = false,
                inventoryTokenCount = 0,
                onUseToken = { tokenTapped = true },
                maxRewardedHints = 3,
                rewardedRemaining = 2,
            )
        }

        composeRule.onNode(hasClickAction()).performClick()

        assert(adTapped) { "Expected the rewarded ad path once no owned token is available" }
        assert(!tokenTapped)
    }

    @Test
    fun freeBudgetRemaining_tapUsesFreeUse_neverTokenOrAd() {
        var freeTapped = false
        var tokenTapped = false
        var adTapped = false
        composeRule.setContent {
            HintButton(
                remaining = 2,
                isArmed = false,
                onClick = { freeTapped = true },
                adState = RewardedAdUiState.Idle,
                onWatchAd = { adTapped = true },
                hasInventoryToken = true,
                inventoryTokenCount = 5,
                onUseToken = { tokenTapped = true },
            )
        }

        composeRule.onNode(hasClickAction()).performClick()

        assert(freeTapped) { "A remaining free use must always win over token/ad, even when both are also available" }
        assert(!tokenTapped && !adTapped)
    }
}
