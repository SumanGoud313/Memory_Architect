package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.ReturningPlayerGiftClaimResult
import com.suman.memoryarchitect.domain.model.ScoreResult

/**
 * The server-authoritative half of [ProgressionRepositoryImpl] - everything about *which* backend
 * actually stores a player's XP/coins/streak/daily-reward state lives behind this one interface,
 * so [ProgressionRepositoryImpl] itself (caching, optimistic updates, the pending-sync queue,
 * achievement/reward evaluation) is completely unaware of which implementation is active.
 *
 * Two implementations, picked per-call by [ProgressionRepositoryImpl.activeRemoteSource]:
 * - [MockBackendProgressionRemoteSource] - the original dev-only mock backend
 *   (`mock-backend/`), which has **no per-player identity at all**: every device that talks to it
 *   shares one process-wide `playerProfile` object. Fine for solo local development, but the
 *   moment a second real device/player uses it, their progress corrupts each other's - this is
 *   not a hypothetical, it's how `mock-backend/index.js` is actually written.
 * - [FirestoreProgressionRemoteSource] - a real per-player datastore, keyed by
 *   [com.suman.memoryarchitect.core.auth.PlayerIdentityManager]'s anonymous uid, using Firestore
 *   transactions so concurrent submissions (two devices, a flaky retry) never lose an update or
 *   double-claim a reward. This is the only one of the two that's actually safe for more than one
 *   real player, and is preferred whenever it's available (see `activeRemoteSource`).
 *
 * All four methods throw on failure (mirroring how `ProgressionApi`'s Retrofit calls already
 * behave) - [ProgressionRepositoryImpl] catches and maps exactly as it did before this
 * abstraction existed.
 */
interface ProgressionRemoteSource {
    suspend fun getProfile(): PlayerProfile

    /** Returns the fully updated profile after this round's XP/coins/streak are applied.
     * [levelSeed] is forwarded for a future server-side re-verification (see [ScoreResult]'s doc)
     * neither implementation currently performs - [MockBackendProgressionRemoteSource] sends it
     * over the wire regardless (preserving the exact request shape this backend already accepted),
     * [FirestoreProgressionRemoteSource] doesn't need it for the XP/coin formula, which only
     * depends on the score/accuracy/combo already computed client-side.
     *
     * [submissionNonce] is minted once per finished round (see [GameplayViewModel.submitReconstruction]
     * (com.suman.memoryarchitect.feature.gameplay.GameplayViewModel)) and reused across any retry of
     * *that same round* - never regenerated per network attempt. [FirestoreProgressionRemoteSource]
     * rejects a nonce it's already seen (throws [DuplicateSubmissionException]) inside the same
     * transaction that grants XP/coins, so a replayed/duplicated call can never grant credit twice.
     * [MockBackendProgressionRemoteSource] ignores it (dev-only, single-shared-profile backend has
     * no per-submission identity to guard). */
    /** [awardXp] is `false` only for a repeat clear of an already-completed Classic level - see
     * [com.suman.memoryarchitect.domain.repository.ProgressionRepository.submitScore]'s doc. Both
     * implementations zero out the xp contribution when `false`; coins/streak/achievements/rewards
     * are entirely unaffected. */
    suspend fun submitScore(
        mode: GameMode,
        score: ScoreResult,
        levelSeed: Long,
        playedOnEpochDay: Long,
        submissionNonce: String,
        newlyUnlockedAchievementCount: Int,
        awardXp: Boolean,
    ): PlayerProfile

    suspend fun getDailyRewardStatus(todayEpochDay: Long): DailyRewardStatus

    /** Must be atomic against a double-claim race (two rapid taps, a retried request after a
     * timeout) - both implementations guarantee this, the mock backend via its single-threaded
     * Node event loop, Firestore via [com.google.firebase.firestore.FirebaseFirestore.runTransaction]. */
    suspend fun claimDailyReward(claimedOnEpochDay: Long): DailyRewardClaimResult

    /** Independently re-derives eligibility from the stored profile/claim-dedup state rather than
     * trusting the client's own [com.suman.memoryarchitect.domain.usecase.GetReturningPlayerWelcomeUseCase]
     * check - throws [ReturningPlayerGiftNotEligibleException] if the gap isn't actually long
     * enough, or [ReturningPlayerGiftAlreadyClaimedException] if today's gift was already claimed. */
    suspend fun claimReturningPlayerGift(claimedOnEpochDay: Long): ReturningPlayerGiftClaimResult
}
