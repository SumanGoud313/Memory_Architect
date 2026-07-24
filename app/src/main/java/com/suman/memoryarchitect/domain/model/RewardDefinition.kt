package com.suman.memoryarchitect.domain.model

/** One entry in [com.suman.memoryarchitect.domain.progression.RewardCatalog]'s timeline. */
data class RewardDefinition(
    val id: RewardId,
    val kind: RewardKind,
    val unlockLevel: Int,
)