package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.MysteryChestReward

/**
 * The server-authoritative half of mission/inventory state - same role [ProgressionRemoteSource]
 * plays for xp/coins/streak, same two implementations picked the same way (see
 * [com.suman.memoryarchitect.data.repository.MissionRepositoryImpl.activeRemoteSource]).
 *
 * Deliberately does **not** expose a "record progress" method - progress toward an active mission
 * is tracked purely locally (see [com.suman.memoryarchitect.domain.repository.MissionRepository.recordMissionEvent]),
 * the same "recognition, not an anti-cheat concern" trust level [PlayerStatistics]/achievements
 * already use. Only a *claim* touches the server, and it's independently re-verifiable there
 * despite trusting the client's reported [claimMissionReward]'s `progressCount`: both the active
 * mission set and the reward it pays are fully recomputable server-side from `periodKey` alone
 * (see [com.suman.memoryarchitect.domain.progression.MissionCatalog]) - a forged/stale claim for a
 * mission that was never active, or for one whose reported progress never reached its target, is
 * rejected inside the same transaction that would otherwise grant it.
 */
interface MissionRemoteSource {
    suspend fun getInventory(): Inventory

    /** [progressCount] is the client's own locally-tracked count at the moment of claiming -
     * trusted the same way [com.suman.memoryarchitect.domain.model.ScoreResult] values are for
     * `submitScore`, since the round/session-worth of gameplay actions that produced it were never
     * sent to the server one at a time. Only bounded by `firestore.rules`' shape checks, not
     * re-derived - an accepted gap of running with no Cloud Function (see the Spark migration
     * report). */
    suspend fun claimMissionReward(missionId: MissionId, periodKey: Long, progressCount: Int): MissionClaimResult

    /** Must be atomic against a double-consume race (a level loading twice, a retried request) -
     * both implementations guarantee this the same way [claimMissionReward]/
     * [ProgressionRemoteSource.claimDailyReward] guarantee no double-claim. */
    suspend fun consumeInventoryItem(kind: InventoryItemKind, quantity: Int): Inventory

    /** Atomically consumes one [InventoryItemKind.MYSTERY_CHEST] and grants [reward] - [reward] is
     * client-rolled (see [MysteryChestOdds]) but every possible value is small and bounded enough
     * that the server's existing generic coin-gain plausibility check already covers it; see
     * [MysteryChestOdds]'s own doc for why no dedicated re-derivation is needed the way
     * [claimMissionReward] has one. Throws [InsufficientInventoryException] if no chest is owned. */
    suspend fun openMysteryChest(reward: MysteryChestReward): Pair<PlayerProfile, Inventory>

    /** Atomically consumes one [InventoryItemKind.XP_BOOST] and grants [xpGranted] flat XP - see
     * [com.suman.memoryarchitect.domain.progression.XpBoostRules]. Throws
     * [InsufficientInventoryException] if no boost is owned. */
    suspend fun applyXpBoost(xpGranted: Long): Pair<PlayerProfile, Inventory>

    /** See [com.suman.memoryarchitect.domain.repository.MissionRepository.claimCategoryBonus]'s
     * doc. [coinsAwarded]/[xpAwarded] are [com.suman.memoryarchitect.domain.progression.MissionCategoryBonusCatalog.roll]'s
     * client-side roll - only re-verified for *plausibility* here ([coinsAwarded] falls within
     * [com.suman.memoryarchitect.domain.progression.MissionCategoryBonusCatalog.CategoryBonus.coinRange],
     * [xpAwarded] must equal that same period's fixed [com.suman.memoryarchitect.domain.progression.MissionCategoryBonusCatalog.CategoryBonus.xp]),
     * never trusted outright; [InventoryItemKind] grants are never sent by the caller at all,
     * always applied from this class's own copy of that same catalog. Throws
     * [MissionNotEligibleException] if [period]'s active set (at [periodKey]) isn't fully claimed
     * yet (or [coinsAwarded]/[xpAwarded] fall outside the configured range), [MissionAlreadyClaimedException]
     * if this bonus was already granted. */
    suspend fun claimCategoryBonus(period: MissionPeriod, periodKey: Long, coinsAwarded: Long, xpAwarded: Long): MissionCategoryBonusOutcome

    /** See [com.suman.memoryarchitect.domain.repository.MissionRepository.unlockAllMissionsEarly]'s
     * doc. Throws [MissionNotEligibleException] if any of the three periods isn't fully claimed
     * yet, [InsufficientCoinsException] if fewer than 1,000 coins are held. */
    suspend fun unlockAllMissionsEarly(dailyPeriodKey: Long, weeklyPeriodKey: Long, monthlyPeriodKey: Long): MissionRefreshOutcome
}

data class MissionCategoryBonusOutcome(
    val reward: MissionReward,
    val profile: PlayerProfile,
    val inventory: Inventory,
)

data class MissionRefreshOutcome(
    val profile: PlayerProfile,
    val refreshState: MissionRefreshState,
)
