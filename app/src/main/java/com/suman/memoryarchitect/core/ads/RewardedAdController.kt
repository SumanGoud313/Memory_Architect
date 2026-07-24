package com.suman.memoryarchitect.core.ads

import android.app.Activity

/**
 * Loads and immediately shows a single rewarded ad, returning only once the whole flow has
 * reached a terminal outcome (earned, dismissed without a reward, or failed) - callers never
 * juggle load/show as separate steps. [activity] is used only for the duration of this call (to
 * present the ad), never retained past it.
 */
interface RewardedAdController {
    /** [feature] ("hint"/"redo"/"rewatch"/...) tags every ad-funnel analytics event this attempt
     * logs - purely for instrumentation, never affects which ad unit is requested or shown. */
    suspend fun loadAndShow(activity: Activity, feature: String): RewardedAdResult
}
