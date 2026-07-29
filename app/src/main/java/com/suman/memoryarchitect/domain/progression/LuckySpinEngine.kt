package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.CosmeticId
import com.suman.memoryarchitect.domain.model.CosmeticRarity
import com.suman.memoryarchitect.domain.model.SpinRewardKind
import kotlin.random.Random

/** Result of one client-computed cosmetic roll, before any server round-trip. [ShopRepositoryImpl]
 * re-verifies only the *ownership transition* (new grant vs. duplicate) server-side - the rarity/
 * item roll itself is client-trusted, a deliberate trade-off proportionate to a cosmetic-only
 * economy with zero competitive stake (see the Points Economy plan's "out of scope" notes). */
data class SpinRoll(val id: CosmeticId, val rarity: CosmeticRarity)

/**
 * Pure, injectable-[Random] engine backing the Lucky Spin - no pity/streak system (a roll is
 * always independent), no I/O. [spin] first checks [isLuckyCosmeticDay] - on a non-lucky day
 * (the large majority) it only ever rolls among [SpinRules.coinOutcomes]; on a lucky day it picks
 * a reward *kind* (coins vs. cosmetic) from [SpinRules.coinOutcomes]/[SpinRules.cosmeticOdds]
 * exactly as this engine always worked before the lucky-day gate existed. Only on the cosmetic
 * branch does [rollRarity] then [pickItem] decide which item.
 */
class LuckySpinEngine(
    private val rules: SpinRules = SpinRules.Default,
    private val random: Random = Random.Default,
) {
    fun rollRarity(): CosmeticRarity {
        val roll = random.nextDouble()
        var cumulative = 0.0
        for ((rarity, odds) in rules.oddsByRarity) {
            cumulative += odds
            if (roll < cumulative) return rarity
        }
        // Floating-point rounding at the tail end of the cumulative sum - fall back to the last
        // configured rarity rather than throwing, so a spin always resolves to something.
        return rules.oddsByRarity.keys.last()
    }

    fun pickItem(rarity: CosmeticRarity): CosmeticId {
        val pool = ShopCatalog.definitionsOfRarity(rarity).filter { it.spinEligible }
        check(pool.isNotEmpty()) { "No spin-eligible items for rarity $rarity" }
        return pool[random.nextInt(pool.size)].id
    }

    /** The cosmetic-only roll [spin] delegates to on its cosmetic branch (and always uses when
     * [guaranteeCosmetic] forces one) - unchanged from this engine's original, only-ever-cosmetic
     * behavior. */
    fun rollCosmetic(): SpinRoll {
        val rarity = rollRarity()
        return SpinRoll(pickItem(rarity), rarity)
    }

    /** Deterministic, stateless per-day gate - true for roughly one day in every
     * [SpinRules.luckyDayIntervalDays] (default 3, "3 days a once"), false every other day. Purely
     * a function of the calendar day (no persisted "last lucky day" to track, so it "works in the
     * background" - the player never sees a visible schedule or countdown to it). [todayEpochDay]
     * is run through [fmix32] (the same Murmur3 finalizer [MissionCatalog.fmix32] documents)
     * rather than a plain `todayEpochDay % N`, so which days land lucky doesn't look like a rigid,
     * predictable fixed cadence - it "shuffles" while still averaging out to the configured rate
     * over many days (verified statistically by `LuckySpinEngineTest`). */
    fun isLuckyCosmeticDay(todayEpochDay: Long): Boolean =
        Math.floorMod(fmix32(todayEpochDay.toInt()), rules.luckyDayIntervalDays) == 0

    private fun fmix32(hIn: Int): Int {
        var h = hIn
        h = h xor (h ushr 16)
        h *= 0x85EBCA6B.toInt()
        h = h xor (h ushr 13)
        h *= 0xC2B2AE35.toInt()
        h = h xor (h ushr 16)
        return h
    }

    /** [guaranteeCosmetic] is set for a player's very first-ever spin (see
     * [com.suman.memoryarchitect.domain.model.PlayerProfile.hasEverSpun]'s doc) - it always
     * resolves to [SpinRewardKind.Cosmetic] regardless of [isLuckyCosmeticDay]/
     * [SpinRules.coinOutcomes]/[SpinRules.cosmeticOdds], though the rarity within that is still
     * rolled normally so a first spin isn't always the same predictable tier.
     *
     * [todayEpochDay] gates whether a cosmetic is even *possible* today at all - see
     * [isLuckyCosmeticDay]'s doc. On a non-lucky day, every spin resolves purely among
     * [SpinRules.coinOutcomes] (re-normalized to their own relative weights, since
     * [SpinRules.cosmeticOdds]' whole share isn't in play that day) - a cosmetic can never appear
     * outside a lucky day, no matter how many spins are used. */
    fun spin(guaranteeCosmetic: Boolean = false, todayEpochDay: Long): SpinRewardKind {
        if (guaranteeCosmetic) return rollCosmetic().toCosmeticReward()
        if (!isLuckyCosmeticDay(todayEpochDay)) return SpinRewardKind.Coins(rollCoinOnlyAmount())

        val roll = random.nextDouble()
        var cumulative = 0.0
        for ((amount, odds) in rules.coinOutcomes) {
            cumulative += odds
            if (roll < cumulative) return SpinRewardKind.Coins(amount)
        }
        // Falls through past coinOutcomes' cumulative share into the remaining cosmeticOdds slice
        // (or floating-point rounding at the very tail) - either way, a cosmetic is always a safe
        // fallback rather than throwing.
        return rollCosmetic().toCosmeticReward()
    }

    /** A non-lucky day's roll - [SpinRules.coinOutcomes] re-normalized to sum to 1.0 on their own
     * (0.35/0.30/0.10 -> ~0.467/0.40/0.133), since [SpinRules.cosmeticOdds]' 0.25 share is entirely
     * off the table today. */
    private fun rollCoinOnlyAmount(): Long {
        val totalOdds = rules.coinOutcomes.sumOf { it.second }
        val roll = random.nextDouble() * totalOdds
        var cumulative = 0.0
        for ((amount, odds) in rules.coinOutcomes) {
            cumulative += odds
            if (roll < cumulative) return amount
        }
        // Floating-point rounding at the tail - fall back to the last configured amount.
        return rules.coinOutcomes.last().first
    }

    private fun SpinRoll.toCosmeticReward() = SpinRewardKind.Cosmetic(id, rarity)
}
