package com.suman.memoryarchitect.core.ads

import android.app.Activity

/**
 * Loads and immediately shows a single interstitial ad, returning only once the whole flow has
 * reached a terminal outcome (shown and dismissed, or failed) - mirrors [RewardedAdController]'s
 * exact shape and lifecycle contract, minus the reward concept. [activity] is used only for the
 * duration of this call, never retained past it.
 *
 * This controller never decides *whether* an interstitial should show right now - that's
 * [InterstitialPacingGate]'s job entirely (frequency caps, cooldowns, session awareness, first-
 * session/first-few-level protection). This class only knows how to load and present one when
 * asked to, exactly like [RewardedAdController] only knows how to load/present, never how often.
 */
interface InterstitialAdController {
    /** [placement] ("level_select_return"/...) tags every ad-funnel analytics event this attempt
     * logs - purely for instrumentation, never affects which ad unit is requested or shown. */
    suspend fun loadAndShow(activity: Activity, placement: String): InterstitialAdResult
}
