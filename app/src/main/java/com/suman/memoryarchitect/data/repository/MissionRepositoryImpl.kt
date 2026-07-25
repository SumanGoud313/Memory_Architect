package com.suman.memoryarchitect.data.repository

import com.suman.memoryarchitect.core.analytics.FirebaseAvailability
import com.suman.memoryarchitect.core.auth.PlayerIdentityManager
import com.suman.memoryarchitect.core.common.DispatcherProvider
import com.suman.memoryarchitect.core.database.InventoryItemDao
import com.suman.memoryarchitect.core.database.InventoryItemEntity
import com.suman.memoryarchitect.core.database.MissionProgressDao
import com.suman.memoryarchitect.core.database.MissionProgressEntity
import com.suman.memoryarchitect.core.database.PlayerProgressDao
import com.suman.memoryarchitect.core.database.PlayerProgressCacheEntity
import com.suman.memoryarchitect.domain.model.ActiveMission
import com.suman.memoryarchitect.domain.model.AppError
import com.suman.memoryarchitect.domain.model.MissionClaimResult
import com.suman.memoryarchitect.domain.model.MissionEvent
import com.suman.memoryarchitect.domain.model.MissionId
import com.suman.memoryarchitect.domain.model.MissionPeriod
import com.suman.memoryarchitect.domain.model.MissionRequirementType
import com.suman.memoryarchitect.domain.model.Outcome
import com.suman.memoryarchitect.domain.model.PlayerProfile
import com.suman.memoryarchitect.domain.progression.MissionCatalog
import com.suman.memoryarchitect.domain.repository.MissionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only progress tracking + server-mirrored claims, same split [ProgressionRepositoryImpl]
 * draws between local statistics/achievements and the server-authoritative profile/daily reward.
 * Which backend actually answers [claimMissionReward] is picked the same way
 * [ProgressionRepositoryImpl.activeRemoteSource] does - see [MissionRemoteSource]'s doc for why.
 */
@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val mockBackendSource: MockBackendMissionRemoteSource,
    private val firestoreSource: FirestoreMissionRemoteSource,
    private val playerIdentityManager: PlayerIdentityManager,
    private val missionProgressDao: MissionProgressDao,
    private val inventoryItemDao: InventoryItemDao,
    private val playerProgressDao: PlayerProgressDao,
    private val dispatchers: DispatcherProvider,
    private val errorMapper: ErrorMapper,
) : MissionRepository {

    private suspend fun activeRemoteSource(): MissionRemoteSource {
        if (!FirebaseAvailability.isConfigured) return mockBackendSource
        val uid = playerIdentityManager.awaitUid()
        return if (uid != null) firestoreSource else mockBackendSource
    }

    override suspend fun getActiveMissions(todayEpochDay: Long): List<ActiveMission> = withContext(dispatchers.io) {
        MissionPeriod.entries.flatMap { period ->
            val periodKey = MissionCatalog.periodKeyFor(period, todayEpochDay)
            MissionCatalog.activeMissionIds(period, periodKey).map { missionId ->
                val definition = MissionCatalog.definitionFor(missionId)
                val progress = currentProgress(missionId, periodKey)
                ActiveMission(definition, periodKey, progress.currentCount, progress.claimed)
            }
        }
    }

    override suspend fun recordMissionEvent(event: MissionEvent, todayEpochDay: Long): Unit = withContext(dispatchers.io) {
        MissionPeriod.entries.forEach { period ->
            val periodKey = MissionCatalog.periodKeyFor(period, todayEpochDay)
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
            val periodKey = MissionCatalog.periodKeyFor(definition.period, todayEpochDay)
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
                Outcome.Error(with(errorMapper) { failure.toAppError() })
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
        lastSyncedAt = System.currentTimeMillis(),
    )
}
