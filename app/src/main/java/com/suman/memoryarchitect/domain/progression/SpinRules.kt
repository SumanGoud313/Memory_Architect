package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity

/** Data-driven tuning for [LuckySpinEngine] - mirrors [RewardRules]'s "balancing is a data
 * change" philosophy. Spins are free (no coin cost any more - see
 * [com.suman.memoryarchitect.domain.repository.ShopRepository.spin]'s doc for the daily/ad/ticket
 * gating that replaced it). [coinOutcomes]' odds plus [cosmeticOdds] must sum to 1.0,
 * and [oddsByRarity] must separately sum to 1.0 (both validated by `LuckySpinEngineTest`, not
 * enforced at construction, matching how [DailyRewardCatalog]'s table isn't validated at
 * construction either).
 *
 * Coins are the deliberate majority outcome (mirrors the request this table was built for: "don't
 * provide coin shop rewards every time, mostly 150/250 coins, with a luck-based 500 coin
 * jackpot") - a genuine Shop cosmetic ([cosmeticOdds]) is the rarer, more exciting minority
 * outcome, same spirit as [MysteryChestOdds]' escalating-but-mostly-modest table.
 *
 * [cosmeticOdds] only ever applies on a "lucky day" (see [LuckySpinEngine.isLuckyCosmeticDay]) -
 * roughly one day in every [luckyDayIntervalDays]. Every other day, a spin can only ever resolve
 * to one of [coinOutcomes] (re-normalized to their own relative weights), never a cosmetic at all
 * - per the request this table was reworked for: "3 days a once as luck in series, remaining days
 * only random coins between 150/250/500." */
data class SpinRules(
    val coinOutcomes: List<Pair<Long, Double>> = listOf(
        150L to 0.35,
        250L to 0.30,
        500L to 0.10, // the rarer "jackpot" amount
    ),
    /** Odds of a *lucky day's* spin resolving to a Shop cosmetic instead of coins - the remainder
     * of [coinOutcomes]' odds (0.35 + 0.30 + 0.10 = 0.75, so this is 0.25) - never consulted at
     * all on a non-lucky day (see [luckyDayIntervalDays]). Once this branch is chosen,
     * [LuckySpinEngine.rollRarity]/[LuckySpinEngine.pickItem] (via [oddsByRarity]) decide *which*
     * cosmetic, exactly as before this table existed. */
    val cosmeticOdds: Double = 0.25,
    /** Roughly one day in every [luckyDayIntervalDays] is a "lucky day" (see
     * [LuckySpinEngine.isLuckyCosmeticDay]) where a cosmetic is even possible at all - every other
     * day only ever pays [coinOutcomes]. 3 matches the request this was built for verbatim
     * ("3 days a once"). */
    val luckyDayIntervalDays: Int = 3,
    val oddsByRarity: Map<CosmeticRarity, Double> = mapOf(
        CosmeticRarity.COMMON to 0.60,
        CosmeticRarity.RARE to 0.28,
        CosmeticRarity.EPIC to 0.10,
        CosmeticRarity.LEGENDARY to 0.02,
    ),
    /** A roll that lands on an already-owned item converts to a coin refund of `priceCoins *
     * duplicateRefundFraction` instead of a no-op - see [LuckySpinEngine.spin]'s doc for why. */
    val duplicateRefundFraction: Double = 0.5,
    /** How many rewarded-ad-gated bonus spins [com.suman.memoryarchitect.domain.repository.SpinSource.AD]
     * allows per day - was a single spin (a plain boolean gate) before this; raised to 3 per the
     * request this was tuned for. Re-verified against
     * [com.suman.memoryarchitect.domain.model.LuckySpinState.adSpinsUsedToday] server-side
     * (Firestore transaction / mock backend), same "recognize, don't just trust" posture the
     * free-spin gate already has - never trusted from the caller's own button-enabled state. */
    val maxAdSpinsPerDay: Int = 3,
) {
    companion object {
        val Default = SpinRules()
    }
}
