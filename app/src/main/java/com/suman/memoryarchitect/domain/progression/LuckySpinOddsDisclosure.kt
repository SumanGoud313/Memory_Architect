package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticRarity

/**
 * The real, per-spin probability of every possible [com.suman.memoryarchitect.domain.model.SpinRewardKind]
 * outcome, computed directly from [SpinRules] - never a hardcoded/duplicated table, so this can
 * never drift out of sync with the actual configured odds the way a separately-maintained
 * disclosure string could. Required by Play Store policy for any randomized reward mechanic
 * ("loot box"-adjacent): the odds a player is shown must be the odds actually in effect.
 *
 * [SpinRules.coinOutcomes]/[SpinRules.cosmeticOdds] are only the odds *on a lucky day* (see
 * [SpinRules]'s own doc) - a non-lucky day re-normalizes [SpinRules.coinOutcomes] to sum to 1.0 and
 * never offers a cosmetic at all. [compute] blends both cases by [SpinRules.luckyDayIntervalDays]
 * (1 lucky day in every N) into the single true, day-independent probability of each outcome -
 * what "the odds of this spin" actually means from a player's point of view, since which kind of
 * day it is isn't a choice they make.
 */
object LuckySpinOddsDisclosure {

    /** [probability] is a fraction in `0.0..1.0` - every [Entry] returned by [compute] together
     * always sums to (very close to) `1.0`. */
    data class Entry(val coinsAwarded: Long?, val rarity: CosmeticRarity?, val probability: Double)

    fun compute(rules: SpinRules = SpinRules.Default): List<Entry> {
        val luckyDayWeight = 1.0 / rules.luckyDayIntervalDays
        val normalDayWeight = 1.0 - luckyDayWeight
        // On a lucky day these odds are used as-is; on a normal day (no cosmetic possible) they're
        // re-normalized to fill the whole 1.0 - mirrors LuckySpinEngine.spin's own re-normalization.
        val coinOutcomesSum = rules.coinOutcomes.sumOf { it.second }

        val coinEntries = rules.coinOutcomes.map { (amount, oddsOnLuckyDay) ->
            val normalDayOdds = if (coinOutcomesSum > 0.0) oddsOnLuckyDay / coinOutcomesSum else 0.0
            val blended = luckyDayWeight * oddsOnLuckyDay + normalDayWeight * normalDayOdds
            Entry(coinsAwarded = amount, rarity = null, probability = blended)
        }
        val cosmeticEntries = rules.oddsByRarity.map { (rarity, oddsWithinCosmetic) ->
            // Only ever reachable on a lucky day - a normal day never offers a cosmetic at all.
            Entry(coinsAwarded = null, rarity = rarity, probability = luckyDayWeight * rules.cosmeticOdds * oddsWithinCosmetic)
        }
        return coinEntries + cosmeticEntries
    }
}
