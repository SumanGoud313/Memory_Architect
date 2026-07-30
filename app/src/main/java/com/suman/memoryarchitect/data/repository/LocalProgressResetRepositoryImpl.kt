package com.suman.memoryarchitect.data.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.HintUsageDao
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.LevelBestTimeDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressDao
import com.suman.memoryarchitect.core.database.MissionProgressDao
import com.suman.memoryarchitect.core.database.OwnedCosmeticDao
import com.suman.memoryarchitect.core.database.PendingScoreSubmissionDao
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.RedoUsageDao
import com.suman.memoryarchitect.core.database.RewatchUsageDao
import com.suman.memoryarchitect.core.database.StatisticsDao
import com.suman.memoryarchitect.core.database.UnlockedAchievementDao
import com.suman.memoryarchitect.core.database.UnlockedRewardDao
import com.suman.memoryarchitect.data.remote.ProgressionApi
import com.suman.memoryarchitect.domain.repository.LocalProgressResetRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single-purpose reset for every locally-persisted progress table — deliberately its own
 * repository rather than a method bolted onto [LevelCampaignRepositoryImpl] or
 * [ProgressionRepositoryImpl], since "wipe everything" cuts across both of their concerns.
 *
 * Also best-effort resets server-side profile state - via [ProgressionApi.resetProfile] against
 * the dev mock backend, or direct client-side Firestore deletes of the same 5 owned collections
 * against a real Firebase project (Spark-plan-compatible - no Cloud Function needed, since these
 * are the player's own documents and `firestore.rules` now grants `allow delete: if isOwner(uid)`
 * on exactly this set). Without this, [ProgressionRepositoryImpl.getProfile]'s online-first fetch
 * would silently repopulate the just-cleared [PlayerProgressDao] with the server's stale
 * xp/coins/streak on the very next load, making the reset look like it never took effect. Both
 * paths are best-effort, failures swallowed rather than propagated - `resetProfile` because a real
 * backend may not expose that endpoint at all (see its Kdoc), the Firestore deletes because a reset
 * attempted while offline genuinely can't reach Firestore yet (there's no local queue for this -
 * it's a rare, deliberate user action, not background sync); either way the local wipe below is
 * what actually matters for the player's immediate experience, and is always applied
 * unconditionally.
 */
@Singleton
class LocalProgressResetRepositoryImpl @Inject constructor(
    private val levelBestTimeDao: LevelBestTimeDao,
    private val levelCampaignProgressDao: LevelCampaignProgressDao,
    private val playerProgressDao: PlayerProgressDao,
    private val statisticsDao: StatisticsDao,
    private val unlockedAchievementDao: UnlockedAchievementDao,
    private val unlockedRewardDao: UnlockedRewardDao,
    private val pendingScoreSubmissionDao: PendingScoreSubmissionDao,
    private val hintUsageDao: HintUsageDao,
    private val redoUsageDao: RedoUsageDao,
    private val rewatchUsageDao: RewatchUsageDao,
    private val ownedCosmeticDao: OwnedCosmeticDao,
    private val equippedCosmeticDao: EquippedCosmeticDao,
    private val missionProgressDao: MissionProgressDao,
    private val inventoryItemDao: InventoryItemDao,
    private val api: ProgressionApi,
    private val playerIdentityManager: PlayerIdentityManager,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
    private val dispatchers: DispatcherProvider,
) : LocalProgressResetRepository {

    private val firestore by lazy { Firebase.firestore }

    override suspend fun resetAll() = withContext(dispatchers.io) {
        if (firebaseAvailabilityProvider.isConfigured) {
            try {
                val uid = playerIdentityManager.awaitUid()
                if (uid != null) {
                    RESET_COLLECTIONS.map { collection ->
                        async { firestore.collection(collection).document(uid).delete().await() }
                    }.awaitAll()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Offline, or not signed in yet - the local wipe below still runs regardless.
            }
        } else {
            try {
                api.resetProfile()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Offline, or a real backend with no such endpoint - the local wipe below still runs.
            }
        }
        levelBestTimeDao.clearAll()
        levelCampaignProgressDao.clearAll()
        playerProgressDao.clearAll()
        statisticsDao.clearAll()
        unlockedAchievementDao.clearAll()
        unlockedRewardDao.clearAll()
        pendingScoreSubmissionDao.clearAll()
        hintUsageDao.clearAll()
        redoUsageDao.clearAll()
        rewatchUsageDao.clearAll()
        ownedCosmeticDao.clearAll()
        equippedCosmeticDao.clearAll()
        missionProgressDao.clearAll()
        inventoryItemDao.clearAll()
    }

    private companion object {
        // Exactly the 5 collections the old resetPlayerData Cloud Function deleted - deliberately
        // does not touch players/{uid} (public leaderboard denormalization) or playerCosmetics/{uid}
        // (owned cosmetics) - a local progress reset isn't meant to also erase a player's
        // competitive leaderboard history or owned cosmetics.
        val RESET_COLLECTIONS = listOf("playerProfiles", "dailyRewards", "inventory", "missionClaims", "returningPlayerGifts")
    }
}
