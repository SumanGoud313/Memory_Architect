package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.EquippedCosmeticDao
import com.suman.memoryarchitect.core.database.HintUsageDao
import com.suman.memoryarchitect.core.database.LevelBestTimeDao
import com.suman.memoryarchitect.core.database.LevelCampaignProgressDao
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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single-purpose reset for every locally-persisted progress table — deliberately its own
 * repository rather than a method bolted onto [LevelCampaignRepositoryImpl] or
 * [ProgressionRepositoryImpl], since "wipe everything" cuts across both of their concerns.
 *
 * Also best-effort resets server-side profile state via [ProgressionApi.resetProfile] - without
 * this, [ProgressionRepositoryImpl.getProfile]'s online-first fetch would silently repopulate
 * the just-cleared [PlayerProgressDao] with the server's stale xp/coins/streak on the very next
 * load, making the reset look like it never took effect. Failures there are swallowed rather
 * than propagated: a real backend may not expose this endpoint at all (see the Kdoc on
 * [ProgressionApi.resetProfile]), and the local wipe below is what actually matters.
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
    private val api: ProgressionApi,
    private val dispatchers: DispatcherProvider,
) : LocalProgressResetRepository {

    override suspend fun resetAll() = withContext(dispatchers.io) {
        try {
            api.resetProfile()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Offline, or a real backend with no such endpoint - the local wipe below still runs.
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
    }
}
