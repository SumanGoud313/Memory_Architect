package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.domain.model.DailyRewardClaimResult
import com.suman.memoryarchitect.domain.model.DailyRewardStatus
import com.suman.memoryarchitect.domain.model.GameMode
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.ScoreResult
import com.suman.memoryarchitect.domain.model.StreakRules
import com.suman.memoryarchitect.domain.progression.DailyRewardCatalog
import com.suman.memoryarchitect.domain.progression.ProgressionRules
import com.suman.memoryarchitect.domain.progression.StreakCalculator
import kotlinx.coroutines.tasks.await
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * The real, per-player-scoped counterpart to [MockBackendProgressionRemoteSource] - see
 * [ProgressionRemoteSource]'s doc for why this exists. Two private Firestore collections (never
 * the public `players/{uid}` the Leaderboard system reads - this is a player's own authoritative
 * progress, not something other players should see or query):
 *
 * - `playerProfiles/{uid}` - xp/coins/streak/challenge-lock timestamps, the same shape
 *   [PlayerProfile] already has.
 * - `dailyRewards/{uid}` - the 7-day login-reward cycle's claim state.
 *
 * Both [submitScore] and [claimDailyReward] run inside a
 * [com.google.firebase.firestore.FirebaseFirestore.runTransaction] block: Firestore retries the
 * whole read-modify-write automatically if another write lands on the same document mid-transaction
 * (two devices signed into the same account, a retried request after a timeout), so neither a lost
 * XP/coin update nor a double-claimed daily reward is possible - a real correctness guarantee the
 * original single-shared-profile mock backend never needed (and never had) because it only ever
 * served one caller's worth of state at a time.
 *
 * The XP/coin/streak *math* itself is unchanged - [StreakCalculator], [ProgressionRules], and
 * [DailyRewardCatalog] are the exact same pure domain classes [ProgressionRepositoryImpl] already
 * used for its own optimistic local update and `mock-backend/progression.js` mirrors; this class
 * just runs them against Firestore-backed state instead of a single in-memory object.
 */
@Singleton
class FirestoreProgressionRemoteSource @Inject constructor(
    private val playerIdentityManager: PlayerIdentityManager,
    private val clock: Clock,
) : ProgressionRemoteSource {

    private val firestore by lazy { Firebase.firestore }
    private val streakCalculator = StreakCalculator()

    override suspend fun getProfile(): PlayerProfile {
        val uid = requireUid()
        val snapshot = firestore.collection(PROFILES_COLLECTION).document(uid).get().await()
        return snapshot.toProfile() ?: PlayerProfile.EMPTY
    }

    override suspend fun submitScore(mode: GameMode, score: ScoreResult, levelSeed: Long, playedOnEpochDay: Long, submissionNonce: String): PlayerProfile {
        val uid = requireUid()
        val docRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        val nonceRef = firestore.collection(SUBMISSION_NONCES_COLLECTION).document(nonceDocId(uid, submissionNonce))
        val xpAwarded = (score.finalScore * ProgressionRules.Default.xpPerScorePoint).roundToLong()
        val coinsAwarded = coinsAwardedFor(mode, score)
        val won = isChallengeWin(mode, score)
        val nowEpochMs = clock.millis()
        return firestore.runTransaction { transaction ->
            // Reject before touching the profile at all - a replay (a retried network call after
            // the first attempt actually committed, or a forged resubmission) must never reach the
            // XP/coin math below. Checked and written inside the same transaction as the profile
            // update, exactly like claimDailyReward's double-claim guard below.
            if (transaction.get(nonceRef).exists()) throw DuplicateSubmissionException()
            val current = transaction.get(docRef).toProfile() ?: PlayerProfile.EMPTY
            val streakUpdate = streakCalculator.updateStreak(
                current.lastPlayedEpochDay,
                playedOnEpochDay,
                current.currentStreak,
                current.longestStreak,
                current.streakShields,
            )
            val updated = current.copy(
                xp = current.xp + xpAwarded,
                coins = current.coins + coinsAwarded,
                currentStreak = streakUpdate.currentStreak,
                longestStreak = streakUpdate.longestStreak,
                streakShields = streakUpdate.streakShields,
                lastPlayedEpochDay = playedOnEpochDay,
                dailyChallengeWonAtEpochSecond = if (mode == GameMode.DAILY_CHALLENGE && won) nowEpochMs / 1000 else current.dailyChallengeWonAtEpochSecond,
                weeklyChallengeWonAtEpochSecond = if (mode == GameMode.WEEKLY_CHALLENGE && won) nowEpochMs / 1000 else current.weeklyChallengeWonAtEpochSecond,
            )
            transaction.set(nonceRef, mapOf("uid" to uid, "createdAtEpochMs" to nowEpochMs))
            transaction.set(docRef, updated.toFirestoreMap(clock))
            updated
        }.await()
    }

    override suspend fun getDailyRewardStatus(todayEpochDay: Long): DailyRewardStatus {
        val uid = requireUid()
        val snapshot = firestore.collection(DAILY_REWARDS_COLLECTION).document(uid).get().await()
        val lastClaimedEpochDay = snapshot.getLong("lastClaimedEpochDay")
        val cycleDay = (snapshot.getLong("cycleDay") ?: 0L).toInt()
        val canClaimToday = DailyRewardCatalog.canClaim(lastClaimedEpochDay, todayEpochDay)
        val previewCycleDay = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay, cycleDay, todayEpochDay)
        return DailyRewardStatus(
            cycleDay = if (canClaimToday) previewCycleDay else cycleDay,
            canClaimToday = canClaimToday,
            lastClaimedEpochDay = lastClaimedEpochDay,
        )
    }

    /** Throws [DailyRewardAlreadyClaimedException] (aborting the transaction, no partial write)
     * if [claimedOnEpochDay] was already claimed by the time this transaction actually runs - the
     * check happens *inside* the transaction against the freshly-read document, not against the
     * [getDailyRewardStatus] snapshot the UI decided to show the claim button from, so two rapid
     * taps (or a retried request) can never both succeed. */
    override suspend fun claimDailyReward(claimedOnEpochDay: Long): DailyRewardClaimResult {
        val uid = requireUid()
        val rewardRef = firestore.collection(DAILY_REWARDS_COLLECTION).document(uid)
        val profileRef = firestore.collection(PROFILES_COLLECTION).document(uid)
        return firestore.runTransaction { transaction ->
            val rewardSnapshot = transaction.get(rewardRef)
            val lastClaimedEpochDay = rewardSnapshot.getLong("lastClaimedEpochDay")
            if (!DailyRewardCatalog.canClaim(lastClaimedEpochDay, claimedOnEpochDay)) {
                throw DailyRewardAlreadyClaimedException()
            }
            val previousCycleDay = (rewardSnapshot.getLong("cycleDay") ?: 0L).toInt()
            val cycleDay = DailyRewardCatalog.nextCycleDay(lastClaimedEpochDay, previousCycleDay, claimedOnEpochDay)
            val entry = DailyRewardCatalog.entryForDay(cycleDay)
            val currentProfile = transaction.get(profileRef).toProfile() ?: PlayerProfile.EMPTY
            val shieldAwarded = entry.bonusShield && currentProfile.streakShields < StreakRules.Default.maxStoredShields
            val updatedProfile = currentProfile.copy(
                coins = currentProfile.coins + entry.coins,
                xp = currentProfile.xp + entry.xp,
                streakShields = if (shieldAwarded) currentProfile.streakShields + 1 else currentProfile.streakShields,
            )
            transaction.set(rewardRef, mapOf("cycleDay" to cycleDay, "lastClaimedEpochDay" to claimedOnEpochDay))
            transaction.set(profileRef, updatedProfile.toFirestoreMap(clock))
            DailyRewardClaimResult(cycleDay, entry.coins, entry.xp, updatedProfile, shieldAwarded)
        }.await()
    }

    private suspend fun requireUid(): String = playerIdentityManager.awaitUid()
        ?: throw IllegalStateException("FirestoreProgressionRemoteSource used with no signed-in player - callers must gate on FirebaseAvailability + a resolved uid via ProgressionRepositoryImpl.activeRemoteSource first")

    /** Mirrors `mock-backend/progression.js`'s `coinsAwardedFor`/[ProgressionRepositoryImpl]'s own
     * private copy - kept as three independent copies of the same small formula (client-optimistic,
     * mock-backend, Firestore) rather than a shared function, matching how the mock backend and
     * client already independently mirror each other; a real Cloud Functions deployment would be
     * the natural place to collapse this into one authoritative copy. */
    private fun coinsAwardedFor(mode: GameMode, score: ScoreResult): Long {
        val rules = ProgressionRules.Default
        return when (mode) {
            GameMode.DAILY_CHALLENGE -> if (isChallengeWin(mode, score)) rules.dailyChallengeWinCoins else 0L
            GameMode.WEEKLY_CHALLENGE -> if (isChallengeWin(mode, score)) rules.weeklyChallengeWinCoins else 0L
            else -> {
                val base = (score.finalScore * rules.coinsPerScorePoint).roundToLong()
                val comboBonus = (score.comboCount - 1).coerceAtLeast(0) * rules.comboBonusCoinsPerStep
                base + comboBonus
            }
        }
    }

    private fun isChallengeWin(mode: GameMode, score: ScoreResult): Boolean =
        (mode == GameMode.DAILY_CHALLENGE || mode == GameMode.WEEKLY_CHALLENGE) &&
            score.sceneAccuracy >= ProgressionRules.Default.challengeWinAccuracyThreshold

    private fun DocumentSnapshot.toProfile(): PlayerProfile? {
        if (!exists()) return null
        return PlayerProfile(
            xp = getLong("xp") ?: 0L,
            coins = getLong("coins") ?: 0L,
            currentStreak = (getLong("currentStreak") ?: 0L).toInt(),
            longestStreak = (getLong("longestStreak") ?: 0L).toInt(),
            lastPlayedEpochDay = getLong("lastPlayedEpochDay"),
            streakShields = (getLong("streakShields") ?: 0L).toInt(),
            dailyChallengeWonAtEpochSecond = getLong("dailyChallengeWonAtEpochSecond"),
            weeklyChallengeWonAtEpochSecond = getLong("weeklyChallengeWonAtEpochSecond"),
        )
    }

    private fun PlayerProfile.toFirestoreMap(clock: Clock): Map<String, Any?> = mapOf(
        "xp" to xp,
        "coins" to coins,
        "currentStreak" to currentStreak,
        "longestStreak" to longestStreak,
        "lastPlayedEpochDay" to lastPlayedEpochDay,
        "streakShields" to streakShields,
        "dailyChallengeWonAtEpochSecond" to dailyChallengeWonAtEpochSecond,
        "weeklyChallengeWonAtEpochSecond" to weeklyChallengeWonAtEpochSecond,
        "updatedAtEpochMs" to clock.millis(),
    )

    private companion object {
        const val PROFILES_COLLECTION = "playerProfiles"
        const val DAILY_REWARDS_COLLECTION = "dailyRewards"
        const val SUBMISSION_NONCES_COLLECTION = "submissionNonces"

        /** `{uid}_{nonce}` rather than a bare nonce as the doc id - keeps one player's nonces from
         * ever colliding with another's even in the (cryptographically negligible) case of a UUID
         * collision, and lets `firestore.rules` bound writes to "only your own nonce docs" via a
         * prefix-style owner check without needing a separate `uid` field to compare against. */
        fun nonceDocId(uid: String, nonce: String): String = "${uid}_$nonce"
    }
}
