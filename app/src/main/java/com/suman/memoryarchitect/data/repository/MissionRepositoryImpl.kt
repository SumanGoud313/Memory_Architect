package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailabilityProvider
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.MissionProgressDao
import com.suman.memoryarchitect.core.database.MissionProgressEntity
import com.suman.memoryarchitect.core.database.MissionRefreshStateDao
import com.suman.memoryarchitect.core.database.MissionRefreshStateEntity
import com.suman.memoryarchitect.core.database.PendingMissionClaimDao
import com.suman.memoryarchitect.core.database.PendingMissionClaimEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.core.database.RemoteConfigDao
import com.suman.memoryarchitect.core.sync.PendingMissionClaimSyncScheduler
import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.LiveEvent
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRefreshState
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.domain.model.MissionReward
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.model.RemoteConfig
import com.suman.memoryarchitect.domain.progression.LiveEventCatalog
import com.suman.memoryarchitect.domain.progression.MissionCatalog
import com.suman.memoryarchitect.domain.progression.MissionCategoryBonusCatalog
import com.suman.memoryarchitect.domain.repository.MissionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only progress tracking + server-mirrored claims, same split [ProgressionRepositoryImpl]
 * draws between local statistics/achievements and the server-authoritative profile/daily reward.
 * Which backend actually answers [claimMissionReward] is picked the same way
 * [ProgressionRepositoryImpl.activeRemoteSource] does - see [MissionRemoteSource]'s doc for why.
 *
 * [MissionPeriod.EVENT] support reads [remoteConfigDao] directly (the same TTL'd cache
 * [RemoteConfigRepositoryImpl] already writes through to) rather than depending on
 * [com.suman.memoryarchitect.domain.repository.RemoteConfigRepository] itself - no repository in
 * this codebase depends on another one, and a live network fetch on every mission list/progress
 * update would be a needless slowdown for something that changes at most a few times a year.
 * Whoever last called [com.suman.memoryarchitect.domain.usecase.GetActiveLiveEventUseCase] (or
 * anything else that reads Remote Config) is what keeps this cache warm; until then, Event
 * missions simply don't appear - the exact same "cache miss reads as empty" behavior
 * [com.suman.memoryarchitect.domain.progression.LiveEventCatalog.activeEvent] already has for a
 * cold install.
 */
@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val mockBackendSource: MockBackendMissionRemoteSource,
    private val firestoreSource: FirestoreMissionRemoteSource,
    private val playerIdentityManager: PlayerIdentityManager,
    private val missionProgressDao: MissionProgressDao,
    private val inventoryItemDao: InventoryItemDao,
    private val playerProgressDao: PlayerProgressDao,
    private val remoteConfigDao: RemoteConfigDao,
    private val pendingMissionClaimDao: PendingMissionClaimDao,
    private val pendingMissionClaimSyncScheduler: PendingMissionClaimSyncScheduler,
    private val missionRefreshStateDao: MissionRefreshStateDao,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
    private val firebaseAvailabilityProvider: FirebaseAvailabilityProvider,
) : MissionRepository {

    private suspend fun activeRemoteSource(): MissionRemoteSource {
        if (!firebaseAvailabilityProvider.isConfigured) return mockBackendSource
        val uid = playerIdentityManager.awaitUid()
        return if (uid != null) firestoreSource else mockBackendSource
    }

    /** `null` whenever no event is live (including a cold Remote Config cache) - see this class's
     * own doc for why this reads [remoteConfigDao] directly instead of a live network fetch. */
    private suspend fun activeEvent(): LiveEvent? {
        val cached = remoteConfigDao.getAll()
        if (cached.isEmpty()) return null
        val remoteConfig = RemoteConfig(cached.associate { it.configKey to it.value }, fetchedAt = 0L)
        return LiveEventCatalog.activeEvent(remoteConfig, Instant.now(clock).epochSecond)
    }

    private suspend fun cachedRefreshState(): MissionRefreshState =
        missionRefreshStateDao.get()?.toDomain() ?: MissionRefreshState.EMPTY

    /** [MissionCatalog.effectivePeriodKey] with [refreshState]'s matching forced-key field picked
     * out - never applies a forced key to [MissionPeriod.EVENT] (no forced-key concept there, see
     * [MissionRefreshState]'s doc). */
    private fun effectivePeriodKeyFor(
        period: MissionPeriod,
        todayEpochDay: Long,
        activeEventStartEpochSecond: Long?,
        refreshState: MissionRefreshState,
    ): Long {
        val forcedPeriodKey = when (period) {
            MissionPeriod.DAILY -> refreshState.dailyForcedPeriodKey
            MissionPeriod.WEEKLY -> refreshState.weeklyForcedPeriodKey
            MissionPeriod.MONTHLY -> refreshState.monthlyForcedPeriodKey
            MissionPeriod.EVENT -> null
        }
        return MissionCatalog.effectivePeriodKey(period, todayEpochDay, forcedPeriodKey, activeEventStartEpochSecond)
    }

    /** Every period except [MissionPeriod.EVENT] when [activeEvent] is `null` - an Event mission
     * simply doesn't exist outside a live event's window, rather than resolving to some stale or
     * placeholder periodKey. */
    private fun periodsToResolve(activeEvent: LiveEvent?): List<MissionPeriod> =
        if (activeEvent != null) MissionPeriod.entries else MissionPeriod.entries - MissionPeriod.EVENT

    override suspend fun getActiveMissions(todayEpochDay: Long): List<ActiveMission> = withContext(dispatchers.io) {
        val event = activeEvent()
        val refreshState = cachedRefreshState()
        periodsToResolve(event).flatMap { period ->
            val periodKey = effectivePeriodKeyFor(period, todayEpochDay, event?.startEpochSecond, refreshState)
            MissionCatalog.activeMissionIds(period, periodKey).map { missionId ->
                val definition = MissionCatalog.definitionFor(missionId)
                val progress = currentProgress(missionId, periodKey)
                ActiveMission(definition, periodKey, progress.currentCount, progress.claimed)
            }
        }
    }

    override suspend fun recordMissionEvent(event: MissionEvent, todayEpochDay: Long): Unit = withContext(dispatchers.io) {
        val liveEvent = activeEvent()
        val refreshState = cachedRefreshState()
        periodsToResolve(liveEvent).forEach { period ->
            val periodKey = effectivePeriodKeyFor(period, todayEpochDay, liveEvent?.startEpochSecond, refreshState)
            MissionCatalog.activeMissionIds(period, periodKey).forEach { missionId ->
                val definition = MissionCatalog.definitionFor(missionId)
                val increment = incrementFor(definition.requirement, event) ?: return@forEach
                val progress = currentProgress(missionId, periodKey)
                if (progress.claimed) return@forEach
                val newCount = (progress.currentCount + increment).coerceAtMost(definition.targetCount)
                if (newCount == progress.currentCount) return@forEach
                missionProgressDao.upsert(progress.copy(currentCount = newCount))
            }
        }
    }

    override suspend fun claimMissionReward(missionId: MissionId, todayEpochDay: Long): Outcome<MissionClaimResult> =
        withContext(dispatchers.io) {
            val definition = MissionCatalog.definitionFor(missionId)
            val eventStartEpochSecond = if (definition.period == MissionPeriod.EVENT) activeEvent()?.startEpochSecond else null
            val periodKey = effectivePeriodKeyFor(definition.period, todayEpochDay, eventStartEpochSecond, cachedRefreshState())
            val progress = currentProgress(missionId, periodKey)
            try {
                val result = activeRemoteSource().claimMissionReward(missionId, periodKey, progress.currentCount)
                missionProgressDao.upsert(progress.copy(claimed = true))
                playerProgressDao.upsert(result.profile.toCacheEntity())
                result.inventory.quantities.forEach { (kind, quantity) ->
                    inventoryItemDao.upsert(InventoryItemEntity(kind.name, quantity))
                }
                Outcome.Success(result)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (alreadyClaimed: MissionAlreadyClaimedException) {
                // Same routine, expected condition as DailyRewardAlreadyClaimedException - a
                // double-claim race, not an unexpected failure worth a Crashlytics report.
                Outcome.Error(AppError.Server(code = 409, message = alreadyClaimed.message))
            } catch (notEligible: MissionNotEligibleException) {
                Outcome.Error(AppError.Server(code = 400, message = notEligible.message))
            } catch (failure: Throwable) {
                // A real network failure (offline, timeout, transient server error) rather than a
                // legitimate rejection - queue it so the player never has to remember to re-tap
                // once back online, mirroring ProgressionRepositoryImpl.submitScore's own queue.
                pendingMissionClaimDao.insert(
                    PendingMissionClaimEntity(
                        missionId = missionId.name,
                        periodKey = periodKey,
                        progressCount = progress.currentCount,
                        createdAt = clock.millis(),
                    ),
                )
                pendingMissionClaimSyncScheduler.scheduleRetry()
                Outcome.Error(with(errorMapper) { failure.toAppError() })
            }
        }

    /** Flushes every queued claim (oldest first): on success, reconciles local progress/profile/
     * inventory with the server's response and drops the entry; on a duplicate-claim response
     * (already landed server-side, the original success response just never reached the client),
     * reconciles the local claimed flag and drops it anyway rather than retrying forever; on
     * "no longer eligible" (the period rotated away while this was queued), drops it since
     * retrying could never succeed; any other failure is rethrown so WorkManager retries the whole
     * worker later - the same shape [com.suman.memoryarchitect.data.repository.ProgressionRepositoryImpl.retryPendingSubmissions]
     * already established for score submissions. */
    override suspend fun retryPendingClaims(): Unit = withContext(dispatchers.io) {
        val pending = pendingMissionClaimDao.getAll().toList()
        for (entity in pending) {
            val missionId = runCatching { MissionId.valueOf(entity.missionId) }.getOrNull()
            if (missionId == null) {
                pendingMissionClaimDao.delete(entity)
                continue
            }
            try {
                val result = activeRemoteSource().claimMissionReward(missionId, entity.periodKey, entity.progressCount)
                markClaimedIfCurrentPeriod(entity)
                playerProgressDao.upsert(result.profile.toCacheEntity())
                result.inventory.quantities.forEach { (kind, quantity) ->
                    inventoryItemDao.upsert(InventoryItemEntity(kind.name, quantity))
                }
                pendingMissionClaimDao.delete(entity)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (alreadyClaimed: MissionAlreadyClaimedException) {
                markClaimedIfCurrentPeriod(entity)
                pendingMissionClaimDao.delete(entity)
            } catch (notEligible: MissionNotEligibleException) {
                pendingMissionClaimDao.delete(entity)
            } catch (failure: Throwable) {
                throw failure
            }
        }
    }

    override suspend fun getMissionRefreshState(): MissionRefreshState = withContext(dispatchers.io) {
        cachedRefreshState()
    }

    override suspend fun claimCategoryBonus(period: MissionPeriod, periodKey: Long): Outcome<MissionReward> = withContext(dispatchers.io) {
        val rolledReward = MissionCategoryBonusCatalog.roll(period)
            ?: return@withContext Outcome.Error(AppError.Server(code = 400, message = "no category bonus for $period"))
        try {
            val outcome = activeRemoteSource().claimCategoryBonus(period, periodKey, rolledReward.coins, rolledReward.xp)
            playerProgressDao.upsert(outcome.profile.toCacheEntity())
            outcome.inventory.quantities.forEach { (kind, quantity) ->
                inventoryItemDao.upsert(InventoryItemEntity(kind.name, quantity))
            }
            Outcome.Success(outcome.reward)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (alreadyClaimed: MissionAlreadyClaimedException) {
            // Routine, expected - see MissionsViewModel's trigger doc: this fires opportunistically
            // every time a full category is detected, so a repeat call after it's already been
            // granted once is normal, not an error worth surfacing.
            Outcome.Error(AppError.Server(code = 409, message = alreadyClaimed.message))
        } catch (notEligible: MissionNotEligibleException) {
            Outcome.Error(AppError.Server(code = 400, message = notEligible.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    override suspend fun unlockAllMissionsEarly(
        dailyPeriodKey: Long,
        weeklyPeriodKey: Long,
        monthlyPeriodKey: Long,
    ): Outcome<MissionRefreshState> = withContext(dispatchers.io) {
        try {
            val outcome = activeRemoteSource().unlockAllMissionsEarly(dailyPeriodKey, weeklyPeriodKey, monthlyPeriodKey)
            playerProgressDao.upsert(outcome.profile.toCacheEntity())
            missionRefreshStateDao.upsert(outcome.refreshState.toCacheEntity())
            Outcome.Success(outcome.refreshState)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (notEligible: MissionNotEligibleException) {
            Outcome.Error(AppError.Server(code = 400, message = notEligible.message))
        } catch (insufficient: InsufficientCoinsException) {
            Outcome.Error(AppError.Server(code = 402, message = insufficient.message))
        } catch (failure: Throwable) {
            Outcome.Error(with(errorMapper) { failure.toAppError() })
        }
    }

    private fun MissionRefreshStateEntity.toDomain() = MissionRefreshState(
        dailyForcedPeriodKey = dailyForcedPeriodKey,
        weeklyForcedPeriodKey = weeklyForcedPeriodKey,
        monthlyForcedPeriodKey = monthlyForcedPeriodKey,
    )

    private fun MissionRefreshState.toCacheEntity() = MissionRefreshStateEntity(
        dailyForcedPeriodKey = dailyForcedPeriodKey,
        weeklyForcedPeriodKey = weeklyForcedPeriodKey,
        monthlyForcedPeriodKey = monthlyForcedPeriodKey,
    )

    /** Only marks the stored progress row claimed if it still belongs to the exact period this
     * queued claim was for - a rotated-away period's row reads as fresh (0, unclaimed) per
     * [currentProgress]'s own doc, and this must never resurrect a stale claim flag onto a brand
     * new period's row. */
    private suspend fun markClaimedIfCurrentPeriod(entity: PendingMissionClaimEntity) {
        val currentRow = missionProgressDao.get(entity.missionId)
        if (currentRow != null && currentRow.periodKey == entity.periodKey) {
            missionProgressDao.upsert(currentRow.copy(claimed = true))
        }
    }

    /** A stored row belongs to this exact ([missionId], [periodKey]) occurrence only if its own
     * `periodKey` still matches - a stale row from a previous cycle (even one for the same
     * [MissionId] rotating back around) reads as fresh (0, unclaimed) rather than inheriting the
     * old count, per [com.suman.memoryarchitect.domain.model.MissionProgress]'s doc. */
    private suspend fun currentProgress(missionId: MissionId, periodKey: Long): MissionProgressEntity {
        val existing = missionProgressDao.get(missionId.name)
        return if (existing != null && existing.periodKey == periodKey) {
            existing
        } else {
            MissionProgressEntity(missionId.name, periodKey, currentCount = 0, claimed = false)
        }
    }

    /** Maps [event] to how much progress it contributes toward [requirement], or `null` if
     * [event] doesn't advance this requirement at all - one mission event can silently no-op
     * against most of the active set and only actually matter to the (at most one, per period) it
     * matches. */
    private fun incrementFor(requirement: MissionRequirementType, event: MissionEvent): Int? = when (event) {
        is MissionEvent.LevelCompleted -> 1.takeIf { requirement == MissionRequirementType.COMPLETE_LEVELS }
        is MissionEvent.PracticeRoundCompleted -> 1.takeIf { requirement == MissionRequirementType.COMPLETE_PRACTICE_ROUNDS }
        is MissionEvent.DailyChallengeWon -> 1.takeIf { requirement == MissionRequirementType.COMPLETE_DAILY_CHALLENGE }
        is MissionEvent.WeeklyChallengeWon -> 1.takeIf { requirement == MissionRequirementType.COMPLETE_WEEKLY_CHALLENGE }
        is MissionEvent.CoinsEarned -> event.amount.toInt().takeIf { requirement == MissionRequirementType.EARN_COINS }
        is MissionEvent.ZeroHintLevelClear -> 1.takeIf { requirement == MissionRequirementType.ZERO_HINT_LEVEL_CLEAR }
        is MissionEvent.HighAccuracyClear -> 1.takeIf { requirement == MissionRequirementType.HIGH_ACCURACY_CLEAR }
        is MissionEvent.StarsEarned -> event.count.takeIf { requirement == MissionRequirementType.EARN_STARS }
        is MissionEvent.CosmeticUnlocked -> 1.takeIf { requirement == MissionRequirementType.UNLOCK_COSMETIC }
        is MissionEvent.CosmeticEquipped -> 1.takeIf { requirement == MissionRequirementType.EQUIP_COSMETIC }
        is MissionEvent.RewardedAdWatched -> 1.takeIf { requirement == MissionRequirementType.WATCH_REWARDED_AD }
    }

    private fun PlayerProfile.toCacheEntity() = PlayerProgressCacheEntity(
        xp = xp,
        coins = coins,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        lastPlayedEpochDay = lastPlayedEpochDay,
        streakShields = streakShields,
        dailyChallengeWonAtEpochSecond = dailyChallengeWonAtEpochSecond,
        weeklyChallengeWonAtEpochSecond = weeklyChallengeWonAtEpochSecond,
        journeyPoints = journeyPoints,
        lastSyncedAt = System.currentTimeMillis(),
    )
}
