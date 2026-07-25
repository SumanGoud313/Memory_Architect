package com.suman.memoryarchitect.domain.model

/** A [MissionDefinition] joined with the player's current [MissionProgress] for this rotation
 * window - what [com.suman.memoryarchitect.domain.repository.MissionRepository.getActiveMissions]
 * actually returns, so the UI never has to join the two itself. */
data class ActiveMission(
    val definition: MissionDefinition,
    val periodKey: Long,
    val currentCount: Int,
    val claimed: Boolean,
) {
    val isComplete: Boolean get() = currentCount >= definition.targetCount
    val canClaim: Boolean get() = isComplete && !claimed
}
