package com.suman.memoryarchitect.domain.generation

import com.suman.memoryarchitect.domain.model.DifficultyTier
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.LevelConstraints
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Pure function from (tier, streak, mode) to [LevelConstraints]. No I/O, no Android
 * dependency — the curve shape lives in [GenerationRules] so live balancing is a data
 * change, not a code change.
 */
class DifficultyEngine(private val rules: GenerationRules = GenerationRules.Default) {

    fun computeConstraints(tier: DifficultyTier, streak: Int, mode: GameMode): LevelConstraints {
        val effectiveTierIndex = tier.ordinal

        val streakBonus = ln((streak + 1).toDouble()) * rules.streakRampFactor

        // Daily/Weekly Challenge get a fixed, deliberately small object count instead of the
        // tier-derived formula below - a simpler, more approachable puzzle size than the
        // tier-scaled default. The memorize duration is pinned alongside it rather than left to
        // recompute from the smaller object count: MemorizeTimeCalculator's per-object floor
        // (see LevelGenerator) would otherwise scale *down* with fewer objects, quietly undoing
        // this session's earlier evidence-based fix for these two modes. Both pinned values are
        // that same formula's own output for the object counts these modes used before this
        // reduction (~10 for Daily/MEDIUM, ~13 for Weekly/HARD, both with rotation active) -
        // reused here as a floor, not invented fresh.
        val objectCount = when (mode) {
            GameMode.DAILY_CHALLENGE -> DAILY_CHALLENGE_OBJECT_COUNT
            GameMode.WEEKLY_CHALLENGE -> WEEKLY_CHALLENGE_OBJECT_COUNT
            else -> (rules.baseObjectCount + effectiveTierIndex * rules.objectCountPerTier + streakBonus)
                .roundToInt()
                .coerceIn(rules.minObjectCount, rules.maxObjectCount)
        }

        val memorizeDurationMs = when (mode) {
            GameMode.DAILY_CHALLENGE -> DAILY_CHALLENGE_MEMORIZE_MS
            GameMode.WEEKLY_CHALLENGE -> WEEKLY_CHALLENGE_MEMORIZE_MS
            // Practice is explicitly "warm up, no stakes" - a flat, generous window regardless of
            // tier rather than the tier-scaled formula below, which could otherwise run as short
            // as rules.minMemorizeDurationMs for a mode that's meant to be the easiest on-ramp.
            GameMode.PRACTICE -> PRACTICE_MEMORIZE_MS
            else -> (rules.baseMemorizeDurationMs - effectiveTierIndex * rules.memorizeDurationStepMs)
                .coerceAtLeast(rules.minMemorizeDurationMs)
        }

        val rotationEnabled = mode != GameMode.PRACTICE && effectiveTierIndex >= rules.rotationUnlockTier
        val orderModeEnabled = mode != GameMode.PRACTICE && effectiveTierIndex >= rules.orderUnlockTier

        val timeLimitMs = if (mode == GameMode.PRACTICE) {
            null
        } else {
            (rules.baseTimeLimitMs - effectiveTierIndex * rules.timeLimitStepMs).coerceAtLeast(rules.minTimeLimitMs)
        }

        // No distractors for Daily/Weekly - the fixed object count above means exactly that many
        // shown in total, not that many targets plus more distractors stacked on top.
        val distractorRatio = if (mode == GameMode.DAILY_CHALLENGE || mode == GameMode.WEEKLY_CHALLENGE) 0f else rules.distractorRatio

        return LevelConstraints(
            objectCount = objectCount,
            distractorRatio = distractorRatio,
            memorizeDurationMs = memorizeDurationMs,
            rotationEnabled = rotationEnabled,
            rotationStepDegrees = rules.rotationStepDegrees,
            orderModeEnabled = orderModeEnabled,
            themeComplexity = effectiveTierIndex,
            timeLimitMs = timeLimitMs,
        )
    }

    private companion object {
        const val DAILY_CHALLENGE_OBJECT_COUNT = 3
        const val WEEKLY_CHALLENGE_OBJECT_COUNT = 5
        const val DAILY_CHALLENGE_MEMORIZE_MS = 11_500L
        const val WEEKLY_CHALLENGE_MEMORIZE_MS = 15_000L
        const val PRACTICE_MEMORIZE_MS = 12_000L
    }
}