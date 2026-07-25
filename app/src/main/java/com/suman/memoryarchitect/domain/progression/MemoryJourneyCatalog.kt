package com.suman.memoryarchitect.domain.progression

import com.suman.memoryarchitect.domain.model.MemoryJourneyStanding
import com.suman.memoryarchitect.domain.model.MemoryJourneyTier
import com.suman.memoryarchitect.domain.model.MemoryJourneyTierId

/**
 * A pure *aggregation* layer over everything the player already does - it introduces no new
 * source of truth, only this shared point-value table ([MemoryJourneyRules]) plus these tier
 * thresholds, both read against a single accumulating [com.suman.memoryarchitect.domain.model.PlayerProfile.journeyPoints]
 * counter (server-authoritative, incremented alongside xp/coins in the exact same writes
 * [com.suman.memoryarchitect.domain.repository.ProgressionRepository.submitScore]/
 * [com.suman.memoryarchitect.domain.repository.MissionRepository.claimMissionReward] already make -
 * see those classes' data-layer implementations rather than a new repository of its own).
 *
 * Deliberately no exclusive cosmetic grant per tier (the plan doc's "Avatar Frames/Room Themes/
 * animated Confetti" list assumes Phase 2's fragment/`earnableOnly` cosmetic-exclusivity mechanism,
 * which this build skipped straight past) - each tier instead grants a Collector Title, the same
 * lightweight "recognition, not a new asset pipeline" reward [RewardCatalog]'s TITLE kind already
 * models. A later phase can layer a real cosmetic grant on top of [tierFor] without touching this
 * table's shape.
 */
object MemoryJourneyCatalog {

    val tiers: List<MemoryJourneyTier> = listOf(
        MemoryJourneyTier(MemoryJourneyTierId.KEEPER_OF_MEMORIES, thresholdPoints = 100L, title = "Keeper of Memories"),
        MemoryJourneyTier(MemoryJourneyTierId.DILIGENT_ARCHIVIST, thresholdPoints = 300L, title = "Diligent Archivist"),
        MemoryJourneyTier(MemoryJourneyTierId.SEASONED_CURATOR, thresholdPoints = 700L, title = "Seasoned Curator"),
        MemoryJourneyTier(MemoryJourneyTierId.MEMORY_ADEPT, thresholdPoints = 1500L, title = "Memory Adept"),
        MemoryJourneyTier(MemoryJourneyTierId.VAULT_GUARDIAN, thresholdPoints = 3000L, title = "Vault Guardian"),
        MemoryJourneyTier(MemoryJourneyTierId.MASTER_ARCHIVIST, thresholdPoints = 5500L, title = "Master Archivist"),
        MemoryJourneyTier(MemoryJourneyTierId.GRAND_CURATOR, thresholdPoints = 9000L, title = "Grand Curator"),
        MemoryJourneyTier(MemoryJourneyTierId.MEMORY_SAGE, thresholdPoints = 14000L, title = "Memory Sage"),
        MemoryJourneyTier(MemoryJourneyTierId.LEGENDARY_ARCHITECT, thresholdPoints = 20000L, title = "Legendary Architect"),
        MemoryJourneyTier(MemoryJourneyTierId.ETERNAL_MEMORY_KEEPER, thresholdPoints = 30000L, title = "Eternal Memory Keeper"),
    )

    fun tierFor(totalPoints: Long): MemoryJourneyTier? = tiers.lastOrNull { totalPoints >= it.thresholdPoints }

    fun definitionFor(id: MemoryJourneyTierId): MemoryJourneyTier = tiers.first { it.id == id }

    /** [MemoryJourneyStanding.progressToNext] is 0..1 through the *gap* between [current]'s own
     * threshold and [next]'s - not a fraction of [next]'s absolute threshold - so the bar always
     * starts empty right after a tier-up rather than already partway full. */
    fun standingFor(totalPoints: Long): MemoryJourneyStanding {
        val current = tierFor(totalPoints)
        val currentIndex = current?.let { tiers.indexOf(it) } ?: -1
        val next = tiers.getOrNull(currentIndex + 1)
        if (next == null) {
            return MemoryJourneyStanding(totalPoints, current, next = null, progressToNext = 1f)
        }
        val floor = current?.thresholdPoints ?: 0L
        val progress = ((totalPoints - floor).toFloat() / (next.thresholdPoints - floor).toFloat()).coerceIn(0f, 1f)
        return MemoryJourneyStanding(totalPoints, current, next, progress)
    }
}
