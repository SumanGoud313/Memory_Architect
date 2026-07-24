package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.RewardDefinition
import com.suman.memoryarchitect.domain.model.RewardId

/** Pure function: no I/O, no persistence — just "given this level, what's newly true?" Mirrors
 * [com.suman.memoryarchitect.domain.achievements.AchievementEvaluator]. */
class RewardEvaluator {

    fun evaluateNewlyUnlocked(level: Int, alreadyUnlocked: Set<RewardId>): List<RewardDefinition> =
        RewardCatalog.timeline.filter { it.unlockLevel <= level && it.id !in alreadyUnlocked }
}