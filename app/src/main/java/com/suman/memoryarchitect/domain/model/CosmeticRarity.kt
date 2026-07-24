package com.suman.memoryarchitect.domain.model

/** Drives both [com.suman.memoryarchitect.domain.progression.ShopRules] price bands and
 * [com.suman.memoryarchitect.domain.progression.SpinRules] odds - the same rarity value governs
 * "how much does this cost" and "how likely is a spin to land on it." */
enum class CosmeticRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY,
}
