package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.RewardDefinition
import com.suman.memoryarchitect.domain.model.RewardId
import com.suman.memoryarchitect.domain.model.RewardKind

/**
 * Static, client-known catalog of milestone rewards — same "recognition, evaluated locally"
 * approach as [com.suman.memoryarchitect.domain.achievements.AchievementCatalog]. The
 * timeline is built, not hand-written: it cycles through [RewardKind.entries] in order, and on
 * each turn the current kind hands out the next id from its own pool (see [RewardRules]) at the
 * next [RewardRules.cadenceLevels] multiple. A kind with an empty pool is skipped without
 * consuming a level slot, so every cadence level up to the last dispensed reward grants exactly
 * one unlock — nobody hits a multi-level drought just because room themes ran out first.
 */
object RewardCatalog {
    val timeline: List<RewardDefinition> = buildTimeline(RewardRules.Default)

    /** [RoomArtRegistry][com.suman.memoryarchitect.ui.illustration.RoomArtRegistry] scene id
     * for a room-theme reward, or null for every other kind. */
    fun roomSceneId(id: RewardId): String? = when (id) {
        RewardId.ROOM_KITCHEN -> "kitchen"
        RewardId.ROOM_COFFEE_SHOP -> "coffee_shop"
        RewardId.ROOM_GAMING_ROOM -> "gaming_room"
        RewardId.ROOM_OFFICE -> "office"
        RewardId.ROOM_LIBRARY -> "library"
        RewardId.ROOM_GARDEN -> "garden"
        RewardId.ROOM_SPACE_STATION -> "space_station"
        else -> null
    }

    /** The most recently unlocked title, auto-equipped — there's no separate equip UI. */
    fun equippedTitle(unlocked: Set<RewardId>): RewardId? = latestUnlockedOfKind(RewardKind.TITLE, unlocked)

    /** The most recently unlocked palette, auto-equipped — there's no separate equip UI. */
    fun equippedPalette(unlocked: Set<RewardId>): RewardId? = latestUnlockedOfKind(RewardKind.PALETTE, unlocked)

    private fun latestUnlockedOfKind(kind: RewardKind, unlocked: Set<RewardId>): RewardId? = timeline
        .filter { it.kind == kind && it.id in unlocked }
        .maxByOrNull { it.unlockLevel }
        ?.id

    private fun buildTimeline(rules: RewardRules): List<RewardDefinition> {
        val pools = mapOf(
            RewardKind.ROOM_THEME to ArrayDeque(rules.roomThemeIds),
            RewardKind.PALETTE to ArrayDeque(rules.paletteIds),
            RewardKind.TITLE to ArrayDeque(rules.titleIds),
            RewardKind.BADGE to ArrayDeque(rules.badgeIds),
        )
        val kindOrder = RewardKind.entries
        val result = mutableListOf<RewardDefinition>()
        var level = rules.cadenceLevels
        var kindIndex = 0
        while (pools.values.any { it.isNotEmpty() }) {
            val kind = kindOrder[kindIndex % kindOrder.size]
            kindIndex++
            val pool = pools.getValue(kind)
            if (pool.isNotEmpty()) {
                result += RewardDefinition(pool.removeFirst(), kind, level)
                level += rules.cadenceLevels
            }
        }
        return result
    }
}