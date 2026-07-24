package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.RewardId

/**
 * Data-driven tuning for [RewardCatalog] — mirrors [LevelCampaignRules]'s "balancing is a data
 * change, not a code change" philosophy. Every [cadenceLevels] levels the player crosses, the
 * next kind in the cycle (room theme -> palette -> title -> badge -> repeat) hands out the next
 * id from its own pool; a kind whose pool is already exhausted is skipped without spending a
 * cadence slot, so unlocks never go quiet as long as any pool still has something left.
 */
data class RewardRules(
    val cadenceLevels: Int = 5,
    // 7 room themes (every scene except bedroom, which is available from the start), 6 palettes,
    // 7 titles, 7 badges - sized so the round-robin cadence below lands a reward on every single
    // 5-level milestone through level 100 with none left over early, instead of running dry at
    // level 85 the way the original, smaller pools did. See the level-design audit.
    val roomThemeIds: List<RewardId> = listOf(
        RewardId.ROOM_KITCHEN,
        RewardId.ROOM_COFFEE_SHOP,
        RewardId.ROOM_GAMING_ROOM,
        RewardId.ROOM_OFFICE,
        RewardId.ROOM_LIBRARY,
        RewardId.ROOM_GARDEN,
        RewardId.ROOM_SPACE_STATION,
    ),
    val paletteIds: List<RewardId> = listOf(
        RewardId.PALETTE_OCEAN,
        RewardId.PALETTE_FOREST,
        RewardId.PALETTE_ORCHID,
        RewardId.PALETTE_MIDNIGHT,
        RewardId.PALETTE_SUNSET,
        RewardId.PALETTE_EMBER,
    ),
    val titleIds: List<RewardId> = listOf(
        RewardId.TITLE_APPRENTICE,
        RewardId.TITLE_RISING_TALENT,
        RewardId.TITLE_SEASONED_DESIGNER,
        RewardId.TITLE_MASTER_ARCHITECT,
        RewardId.TITLE_LEGENDARY_ARCHITECT,
        RewardId.TITLE_GRANDMASTER_ARCHITECT,
        RewardId.TITLE_ETERNAL_ARCHITECT,
    ),
    val badgeIds: List<RewardId> = listOf(
        RewardId.BADGE_BRONZE_KEY,
        RewardId.BADGE_SILVER_KEY,
        RewardId.BADGE_GOLD_KEY,
        RewardId.BADGE_PLATINUM_KEY,
        RewardId.BADGE_DIAMOND_KEY,
        RewardId.BADGE_OBSIDIAN_KEY,
        RewardId.BADGE_CELESTIAL_KEY,
    ),
) {
    companion object {
        val Default = RewardRules()
    }
}