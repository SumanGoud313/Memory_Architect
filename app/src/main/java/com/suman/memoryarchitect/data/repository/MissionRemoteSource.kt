package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.domain.model.Inventory
import com.suman.memoryarchitect.domain.model.InventoryItemKind
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionId

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
     * `submitScore`, and bounded the same "plausibility, not full re-derivation" way by the
     * mirrored Cloud Function (see `functions/src/missions.ts`) rather than rejected outright,
     * since the round/session-worth of gameplay actions that produced it were never sent to the
     * server one at a time. */
    suspend fun claimMissionReward(missionId: MissionId, periodKey: Long, progressCount: Int): MissionClaimResult

    /** Must be atomic against a double-consume race (a level loading twice, a retried request) -
     * both implementations guarantee this the same way [claimMissionReward]/
     * [ProgressionRemoteSource.claimDailyReward] guarantee no double-claim. */
    suspend fun consumeInventoryItem(kind: InventoryItemKind, quantity: Int): Inventory
}
