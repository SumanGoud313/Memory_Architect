package com.suman.memoryarchitect.domain.achievements

import com.suman.memoryarchitect.domain.model.AchievementId

/** Pure function: no I/O, no persistence — just "given this snapshot, what's newly true?" */
class AchievementEvaluator {

    fun evaluateNewlyUnlocked(
        snapshot: ProgressSnapshot,
        alreadyUnlocked: Set<AchievementId>,
    ): List<AchievementId> = AchievementCatalog.definitions
        .filter { it.id !in alreadyUnlocked && it.isSatisfiedBy(snapshot) }
        .map { it.id }
}
