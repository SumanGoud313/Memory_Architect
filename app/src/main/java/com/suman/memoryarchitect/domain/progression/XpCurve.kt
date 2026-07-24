package com.suman.memoryarchitect.domain.progression

import kotlin.math.pow
import kotlin.math.roundToLong

/** Pure XP-to-level curve. Level 1 always requires 0 XP. */
class XpCurve(private val rules: ProgressionRules = ProgressionRules.Default) {

    fun xpRequiredForLevel(level: Int): Long {
        if (level <= 1) return 0L
        return (rules.baseXpPerLevel * (level - 1).toDouble().pow(rules.levelCurveExponent)).roundToLong()
    }

    fun levelForXp(totalXp: Long): Int {
        var level = 1
        while (xpRequiredForLevel(level + 1) <= totalXp) level++
        return level
    }

    /** (xpIntoCurrentLevel, xpNeededForNextLevel) — for a "N / M XP" progress display. */
    fun xpProgressWithinLevel(totalXp: Long): Pair<Long, Long> {
        val level = levelForXp(totalXp)
        val floor = xpRequiredForLevel(level)
        val ceiling = xpRequiredForLevel(level + 1)
        return (totalXp - floor) to (ceiling - floor)
    }
}
